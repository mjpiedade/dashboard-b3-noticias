package com.mjpiedade.noticiasb3

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repositório do JSON dos temas: rede + cache em disco.
 *
 * - Fonte: https://mjpiedade.github.io/dashboard-b3-noticias/data/noticias_temas.json
 * - Cache: <filesDir>/noticias_temas.json (última versão vista, aberta offline)
 *
 * Sem gson/moshi/kotlinx-serialization — parse manual com org.json (do SDK).
 * Mantém as dependências ao mínimo (APK pequeno, menos coisas para partir).
 */
object NoticiasRepo {

    const val URL_JSON = "https://mjpiedade.github.io/dashboard-b3-noticias/data/noticias_temas.json"
    const val URL_PWA = "https://mjpiedade.github.io/dashboard-b3-noticias/"
    private const val NOME_CACHE = "noticias_temas.json"
    private const val TIMEOUT_MS = 15_000

    /** Vai à rede; se falhar, devolve o que estiver em cache (ou Feed.Vazio). */
    fun buscar(ctx: Context): Feed {
        val corpo = descarregar()
        if (corpo != null) {
            gravarCache(ctx, corpo)
            parse(corpo)?.let { return it }
        }
        return lerCache(ctx)
    }

    /** Wrapper para chamar do main thread sem bloquear a UI. */
    suspend fun buscarAsync(ctx: Context): Feed = withContext(Dispatchers.IO) { buscar(ctx) }

    /** Só cache — usado pelo widget quando não precisa de gastar bateria com rede. */
    fun lerCache(ctx: Context): Feed {
        val f = File(ctx.filesDir, NOME_CACHE)
        if (!f.exists()) return Feed.Vazio
        return try {
            parse(f.readText(Charsets.UTF_8)) ?: Feed.Vazio
        } catch (_: Exception) {
            Feed.Vazio
        }
    }

    private fun descarregar(): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(URL_JSON).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }
            val codigo = conn.responseCode
            if (codigo !in 200..299) null
            else conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun gravarCache(ctx: Context, corpo: String) {
        try {
            File(ctx.filesDir, NOME_CACHE).writeText(corpo, Charsets.UTF_8)
        } catch (_: Exception) {
            // sem cache é chato mas não é fatal — a próxima leitura tenta de novo
        }
    }

    private fun parse(corpo: String): Feed? {
        return try {
            val raiz = JSONObject(corpo)
            val atualizado = raiz.optString("atualizado_em", "")
            val arr = raiz.optJSONArray("temas") ?: return Feed(atualizado, emptyList())
            val temas = ArrayList<Tema>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val noticiasArr = o.optJSONArray("noticias")
                val noticias = ArrayList<NoticiaItem>(noticiasArr?.length() ?: 0)
                if (noticiasArr != null) {
                    for (j in 0 until noticiasArr.length()) {
                        val n = noticiasArr.optJSONObject(j) ?: continue
                        noticias.add(
                            NoticiaItem(
                                titulo = n.optString("titulo", ""),
                                fonte = n.optString("fonte", ""),
                                data = n.optString("data", ""),
                                hora = n.optString("hora", ""),
                            )
                        )
                    }
                }
                temas.add(
                    Tema(
                        titulo = o.optString("titulo", "").ifEmpty { "(sem título)" },
                        sinal = Sinal.de(o.optString("sinal", "neutro")),
                        nNoticias = o.optInt("n_noticias", noticias.size),
                        resumo = o.optString("resumo", ""),
                        impacto = o.optString("impacto", ""),
                        noticias = noticias,
                    )
                )
            }
            Feed(atualizado, temas)
        } catch (_: JSONException) {
            null
        }
    }
}
