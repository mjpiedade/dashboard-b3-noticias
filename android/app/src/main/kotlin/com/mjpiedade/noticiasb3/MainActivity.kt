package com.mjpiedade.noticiasb3

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.buildSpannedString
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Ecrã principal — mostra os 12 temas do dia com resumo/impacto/notícias.
 * Igual em espírito à PWA, mas nativo (sem service worker, sem cache do browser).
 *
 * - Cabeçalho: título + "há Xm" + contadores dos sinais
 * - Corpo: RecyclerView com cartões expansíveis
 * - Pull-to-refresh: força ida à rede
 * - Refresh automático quando o ecrã volta ao foco
 */
class MainActivity : AppCompatActivity() {

    private lateinit var idade: TextView
    private lateinit var contadores: TextView
    private lateinit var lista: RecyclerView
    private lateinit var refresh: SwipeRefreshLayout
    private val adapter = TemaAdapter()

    private var ultimoAtualizadoEm: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        idade = findViewById(R.id.idade)
        contadores = findViewById(R.id.contadores)
        lista = findViewById(R.id.lista)
        refresh = findViewById(R.id.refresh)

        lista.layoutManager = LinearLayoutManager(this)
        lista.adapter = adapter

        refresh.setColorSchemeColors(Sinal.RISK_ON.cor, Sinal.NEUTRO.cor, Sinal.RISK_OFF.cor)
        refresh.setProgressBackgroundColorSchemeColor(0xFF1A2233.toInt())
        refresh.setOnRefreshListener { carregar(forcar = true) }

        // primeira leitura: mostra cache imediatamente + vai buscar rede
        mostrar(NoticiasRepo.lerCache(this))
        carregar(forcar = true)
    }

    override fun onResume() {
        super.onResume()
        // Se o ecrã voltou ao foco depois de estar em background, refresca a idade
        // e vai buscar nova versão (silenciosa, sem spinner).
        atualizarIdade()
        carregar(forcar = false)
    }

    private fun carregar(forcar: Boolean) {
        if (forcar) refresh.isRefreshing = true
        lifecycleScope.launch {
            val feed = NoticiasRepo.buscarAsync(this@MainActivity)
            mostrar(feed)
            refresh.isRefreshing = false
        }
    }

    private fun mostrar(feed: Feed) {
        ultimoAtualizadoEm = feed.atualizadoEm
        atualizarIdade()
        atualizarContadores(feed)
        adapter.submeter(feed.temas)
        if (feed.temas.isEmpty()) {
            // Sem cache nem rede — deixa a lista vazia mas o utilizador vê o header.
            // (Podíamos mostrar um estado vazio, mas na prática assim que há rede aparece.)
        }
    }

    private fun atualizarIdade() {
        idade.text = idadeTexto(ultimoAtualizadoEm)
    }

    private fun atualizarContadores(feed: Feed) {
        val (on, neu, off) = feed.contagens
        contadores.text = buildSpannedString {
            pontoColorido(Sinal.RISK_ON.cor); append(" $on risk-on   ")
            pontoColorido(Sinal.NEUTRO.cor);  append(" $neu neutros   ")
            pontoColorido(Sinal.RISK_OFF.cor); append(" $off risk-off   · ${feed.temas.size} temas")
        }
    }

    private fun SpannableStringBuilder.pontoColorido(cor: Int) {
        val inicio = length
        append("●")
        setSpan(ForegroundColorSpan(cor), inicio, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /** "2026-08-12T14:11" -> "há 23m" / "há 2h" / "há 3d" / "—" (interpreta como SP TZ) */
    private fun idadeTexto(iso: String): String {
        if (iso.isBlank()) return "—"
        return try {
            val local = LocalDateTime.parse(iso, FMT)
            val quando: ZonedDateTime = OffsetDateTime.of(local, ZoneOffset.ofHours(-3)).toZonedDateTime()
            val minutos = ChronoUnit.MINUTES.between(quando, ZonedDateTime.now())
            when {
                minutos < 1 -> "agora"
                minutos < 60 -> "há ${minutos}m"
                minutos < 60 * 24 -> {
                    val h = minutos / 60
                    val m = minutos % 60
                    if (m == 0L) "há ${h}h" else "há ${h}h${m.toString().padStart(2, '0')}"
                }
                else -> "há ${minutos / (60 * 24)}d"
            }
        } catch (_: Exception) {
            "—"
        }
    }

    companion object {
        private val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    }
}
