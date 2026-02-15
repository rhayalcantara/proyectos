package com.clonewhatsapp.core.network.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker de WorkManager para sincronizar mensajes pendientes en segundo plano.
 *
 * Se ejecuta periodicamente (cada 15 minutos) con restriccion de conectividad.
 * Tambien puede ser invocado manualmente para sincronizacion inmediata.
 *
 * T-094: Sincronizacion automatica con WorkManager.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker: Iniciando sincronizacion de mensajes pendientes")

        return try {
            syncManager.procesarMensajesPendientes()

            val estadoActual = syncManager.estadoSync.value
            if (estadoActual.cantidadPendientes > 0) {
                Timber.d("SyncWorker: Quedan ${estadoActual.cantidadPendientes} mensajes pendientes, reintentando")
                Result.retry()
            } else {
                Timber.d("SyncWorker: Todos los mensajes enviados exitosamente")
                Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Error durante la sincronizacion")
            Result.retry()
        }
    }

    companion object {
        /** Nombre unico del trabajo periodico */
        const val NOMBRE_TRABAJO = "sync_mensajes_pendientes"

        /** Tag para identificar trabajos de sincronizacion */
        const val TAG_SYNC = "sync_mensajes"
    }
}
