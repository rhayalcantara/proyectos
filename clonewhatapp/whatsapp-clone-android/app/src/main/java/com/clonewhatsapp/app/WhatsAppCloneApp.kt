package com.clonewhatsapp.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.clonewhatsapp.core.common.notification.NotificationChannels
import com.clonewhatsapp.core.network.sync.SyncInitializer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WhatsAppCloneApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncInitializer: SyncInitializer

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Crear canales de notificacion (requerido para SDK 26+)
        NotificationChannels.crearTodosLosCanales(this)

        // Inicializar sistema de sincronizacion offline
        syncInitializer.inicializar()
    }

    /**
     * Configuracion de WorkManager con HiltWorkerFactory.
     * Necesario para que @HiltWorker funcione correctamente.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
