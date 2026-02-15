package com.clonewhatsapp.core.network.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inicializador del sistema de sincronizacion.
 *
 * Configura y programa los trabajos de WorkManager para la sincronizacion
 * periodica de mensajes pendientes. Tambien inicia el SyncManager
 * para monitoreo en tiempo real.
 *
 * T-098: Inicializacion del sistema de sincronizacion.
 */
@Singleton
class SyncInitializer @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val syncManager: SyncManager
) {
    /**
     * Inicializa el sistema de sincronizacion completo.
     * - Inicia el SyncManager para monitoreo en tiempo real.
     * - Ejecuta una sincronizacion inmediata.
     * - Programa sincronizacion periodica cada 15 minutos.
     *
     * Debe ser llamado desde Application.onCreate() o MainViewModel.
     */
    fun inicializar() {
        Timber.d("Inicializando sistema de sincronizacion")

        // Iniciar el SyncManager para monitoreo reactivo
        syncManager.iniciarSync()

        // Ejecutar sincronizacion inmediata al abrir la app
        ejecutarSyncInmediato()

        // Programar sincronizacion periodica
        programarSyncPeriodico()
    }

    /**
     * Ejecuta una sincronizacion inmediata usando WorkManager.
     * Se ejecuta una sola vez con restriccion de red.
     */
    private fun ejecutarSyncInmediato() {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val trabajoInmediato = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(restricciones)
            .addTag(SyncWorker.TAG_SYNC)
            .build()

        WorkManager.getInstance(contexto).enqueue(trabajoInmediato)
        Timber.d("Sincronizacion inmediata programada")
    }

    /**
     * Programa la sincronizacion periodica de mensajes pendientes.
     * Se ejecuta cada 15 minutos (minimo permitido por WorkManager)
     * con restriccion de conectividad de red.
     */
    private fun programarSyncPeriodico() {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val trabajoPeriodico = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(restricciones)
            .addTag(SyncWorker.TAG_SYNC)
            .build()

        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
            SyncWorker.NOMBRE_TRABAJO,
            ExistingPeriodicWorkPolicy.KEEP,
            trabajoPeriodico
        )

        Timber.d("Sincronizacion periodica programada (cada 15 minutos)")
    }
}
