package com.mjpiedade.noticiasb3

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Widget 4x2 com um tema visível de cada vez, setas para navegar,
 * selo colorido do sinal, contador X/N, "há Xm" e mini-contador de sinais.
 *
 * Design:
 * - índice do tema visível guardado em SharedPreferences (persistente entre updates)
 * - setas ‹ / ›  disparam broadcasts ACTION_PREV / ACTION_NEXT
 * - toque na área do título abre a PWA no tema atual (?tema=N)
 * - refresh dos dados fica a cargo do [RefreshWorker] (30 min); onEnabled agenda-o
 */
class NoticiasWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(ctx: Context) {
        RefreshWorker.agendarPeriodico(ctx)
        // primeira leitura imediata na criação do widget
        RefreshWorker.correrJa(ctx)
    }

    override fun onDisabled(ctx: Context) {
        RefreshWorker.cancelar(ctx)
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val feed = NoticiasRepo.lerCache(ctx)
        for (id in ids) atualizarUm(ctx, mgr, id, feed)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        when (intent.action) {
            ACTION_PREV, ACTION_NEXT, ACTION_REFRESH_DONE -> {
                val feed = NoticiasRepo.lerCache(ctx)
                val total = max(1, feed.temas.size)
                val prefs = prefs(ctx)
                val i = prefs.getInt(CHAVE_INDICE, 0)
                val novo = when (intent.action) {
                    ACTION_PREV -> ((i - 1) % total + total) % total
                    ACTION_NEXT -> (i + 1) % total
                    else -> i.coerceIn(0, total - 1)
                }
                if (novo != i) prefs.edit { putInt(CHAVE_INDICE, novo) }
                atualizarTodos(ctx, feed)
            }
        }
    }

    private fun atualizarUm(ctx: Context, mgr: AppWidgetManager, id: Int, feed: Feed) {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_noticias)
        val total = feed.temas.size

        if (total == 0) {
            rv.setInt(R.id.faixa, "setBackgroundColor", Sinal.NEUTRO.cor)
            rv.setTextViewText(R.id.selo, "SEM DADOS")
            rv.setTextViewText(R.id.contador, "")
            rv.setTextViewText(R.id.idade, "")
            rv.setTextViewText(R.id.titulo, "A carregar as notícias…")
            rv.setTextViewText(R.id.contagens, "")
            rv.setOnClickPendingIntent(R.id.raiz, abrirPwa(ctx, indice = null))
        } else {
            val i = prefs(ctx).getInt(CHAVE_INDICE, 0).coerceIn(0, total - 1)
            val t = feed.temas[i]

            rv.setInt(R.id.faixa, "setBackgroundColor", t.sinal.cor)
            rv.setTextViewText(R.id.selo, t.sinal.rotulo)
            rv.setTextViewText(R.id.contador, "${i + 1}/$total")
            rv.setTextViewText(R.id.idade, idade(feed.atualizadoEm))
            rv.setTextViewText(R.id.titulo, t.titulo)

            val (on, neu, off) = feed.contagens
            rv.setTextViewText(R.id.contagens, "● $on   ● $neu   ● $off")

            rv.setOnClickPendingIntent(R.id.raiz, abrirPwa(ctx, indice = i))
        }

        rv.setOnClickPendingIntent(R.id.btn_prev, difusao(ctx, ACTION_PREV, id))
        rv.setOnClickPendingIntent(R.id.btn_next, difusao(ctx, ACTION_NEXT, id))

        mgr.updateAppWidget(id, rv)
    }

    // ----- helpers -----

    private fun difusao(ctx: Context, acao: String, widgetId: Int): PendingIntent {
        val intent = Intent(ctx, NoticiasWidgetProvider::class.java).apply {
            action = acao
            // uri única para o PendingIntent não ser reutilizado entre widgets
            data = Uri.parse("noticiasb3://$acao/$widgetId")
        }
        return PendingIntent.getBroadcast(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun abrirPwa(ctx: Context, indice: Int?): PendingIntent {
        val url = if (indice != null) "${NoticiasRepo.URL_PWA}?tema=$indice" else NoticiasRepo.URL_PWA
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            ctx, indice ?: -1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** "2026-08-12T14:11" -> "há 23m" / "há 2h" / "há 3d" / "—" */
    private fun idade(iso: String): String {
        if (iso.isBlank()) return "—"
        return try {
            val quando = LocalDateTime.parse(iso, FMT).atZone(ZoneId.systemDefault())
            val minutos = ChronoUnit.MINUTES.between(quando, java.time.ZonedDateTime.now())
            when {
                minutos < 1 -> "agora"
                minutos < 60 -> "há ${minutos}m"
                minutos < 60 * 24 -> "há ${minutos / 60}h"
                else -> "há ${minutos / (60 * 24)}d"
            }
        } catch (_: Exception) {
            "—"
        }
    }

    companion object {
        const val ACTION_PREV = "com.mjpiedade.noticiasb3.ACTION_PREV"
        const val ACTION_NEXT = "com.mjpiedade.noticiasb3.ACTION_NEXT"
        const val ACTION_REFRESH_DONE = "com.mjpiedade.noticiasb3.ACTION_REFRESH_DONE"

        private const val PREFS = "noticiasb3_widget"
        private const val CHAVE_INDICE = "indice_actual"
        private val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

        fun prefs(ctx: Context): SharedPreferences =
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /** Chamado pelo [RefreshWorker] quando a busca terminar. */
        fun atualizarTodos(ctx: Context, feed: Feed) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val comp = ComponentName(ctx, NoticiasWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(comp)
            if (ids.isEmpty()) return
            val provider = NoticiasWidgetProvider()
            for (id in ids) provider.atualizarUm(ctx, mgr, id, feed)
        }
    }
}
