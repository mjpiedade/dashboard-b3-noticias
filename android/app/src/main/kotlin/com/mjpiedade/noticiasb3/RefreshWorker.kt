package com.mjpiedade.noticiasb3

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Vai à rede buscar o JSON, guarda em cache e notifica o widget.
 * Corrida periódica de 30 min (o mínimo permitido pelo WorkManager).
 */
class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val feed = NoticiasRepo.buscar(applicationContext)
        NoticiasWidgetProvider.atualizarTodos(applicationContext, feed)
        // Se falhou totalmente (cache vazia e rede em baixo), pede retry (WM aplica backoff)
        return if (feed.temas.isEmpty()) Result.retry() else Result.success()
    }

    companion object {
        private const val NOME_PERIODICO = "noticias_b3_refresh_periodico"
        private const val NOME_UNICO = "noticias_b3_refresh_ja"

        fun agendarPeriodico(ctx: Context) {
            val restricoes = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val pedido = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(restricoes)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                NOME_PERIODICO,
                ExistingPeriodicWorkPolicy.KEEP,
                pedido,
            )
        }

        fun correrJa(ctx: Context) {
            val restricoes = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val pedido = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setConstraints(restricoes)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                NOME_UNICO,
                ExistingWorkPolicy.REPLACE,
                pedido,
            )
        }

        fun cancelar(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(NOME_PERIODICO)
        }
    }
}
