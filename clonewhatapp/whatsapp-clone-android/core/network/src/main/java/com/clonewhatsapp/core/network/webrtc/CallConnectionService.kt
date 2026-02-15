package com.clonewhatsapp.core.network.webrtc

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Integracion con Android Telecom ConnectionService.
 *
 * Permite que las llamadas de la app se integren con la interfaz de llamadas
 * del sistema (pantalla de llamada nativa, Bluetooth, etc.).
 *
 * NOTA: No todos los OEM soportan ConnectionService correctamente.
 * El servicio CallService con notificaciones de primer plano es el mecanismo
 * principal. Este servicio es un complemento opcional.
 */
class CallConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val extras = request?.extras ?: Bundle()
        val callerName = extras.getString(EXTRA_CALLER_DISPLAY_NAME, "Desconocido")
        val callerId = extras.getString(EXTRA_CALLER_ID, "")

        val connection = CallConnection(applicationContext).apply {
            setInitializing()
            setCallerDisplayName(callerName, TelecomManager.PRESENTATION_ALLOWED)
            setAddress(
                Uri.fromParts("tel", callerId, null),
                TelecomManager.PRESENTATION_ALLOWED
            )
            connectionCapabilities = Connection.CAPABILITY_MUTE or
                Connection.CAPABILITY_SUPPORT_HOLD or
                Connection.CAPABILITY_HOLD
            setRinging()
        }

        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val extras = request?.extras ?: Bundle()
        val callerName = extras.getString(EXTRA_CALLER_DISPLAY_NAME, "Desconocido")

        val connection = CallConnection(applicationContext).apply {
            setInitializing()
            setCallerDisplayName(callerName, TelecomManager.PRESENTATION_ALLOWED)
            setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
            connectionCapabilities = Connection.CAPABILITY_MUTE or
                Connection.CAPABILITY_SUPPORT_HOLD or
                Connection.CAPABILITY_HOLD
            setDialing()
        }

        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.w(TAG, "Error al crear conexion entrante en Telecom")
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.w(TAG, "Error al crear conexion saliente en Telecom")
    }

    companion object {
        private const val TAG = "CallConnectionService"
        private const val PHONE_ACCOUNT_ID = "whatsapp_clone_calls"
        const val EXTRA_CALLER_DISPLAY_NAME = "extra_caller_display_name"
        const val EXTRA_CALLER_ID = "extra_caller_id"

        /**
         * Registra la PhoneAccount de la app en el TelecomManager.
         * Debe llamarse una vez, por ejemplo al iniciar la aplicacion.
         */
        fun registerPhoneAccount(context: Context) {
            try {
                val telecomManager =
                    context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                        ?: return

                val handle = obtenerPhoneAccountHandle(context)

                val account = PhoneAccount.builder(handle, "WhatsApp Clone")
                    .setCapabilities(
                        PhoneAccount.CAPABILITY_CALL_PROVIDER or
                            PhoneAccount.CAPABILITY_CONNECTION_MANAGER
                    )
                    .setShortDescription("Llamadas de WhatsApp Clone")
                    .build()

                telecomManager.registerPhoneAccount(account)
            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar PhoneAccount: ${e.message}")
            }
        }

        /**
         * Agrega una nueva llamada entrante al TelecomManager.
         */
        fun addNewIncomingCall(
            context: Context,
            callerId: String,
            callerName: String
        ) {
            try {
                val telecomManager =
                    context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                        ?: return

                val handle = obtenerPhoneAccountHandle(context)
                val extras = Bundle().apply {
                    putString(EXTRA_CALLER_DISPLAY_NAME, callerName)
                    putString(EXTRA_CALLER_ID, callerId)
                    putParcelable(
                        TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                        Uri.fromParts("tel", callerId, null)
                    )
                }

                telecomManager.addNewIncomingCall(handle, extras)
            } catch (e: SecurityException) {
                Log.w(TAG, "Sin permisos para Telecom, usando fallback: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al agregar llamada entrante a Telecom: ${e.message}")
            }
        }

        private fun obtenerPhoneAccountHandle(context: Context): PhoneAccountHandle {
            return PhoneAccountHandle(
                ComponentName(context, CallConnectionService::class.java),
                PHONE_ACCOUNT_ID
            )
        }
    }
}

/**
 * Representa una conexion de llamada individual dentro del sistema Telecom.
 */
internal class CallConnection(
    private val context: Context
) : Connection() {

    override fun onAnswer() {
        setActive()
    }

    override fun onReject() {
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onDisconnect() {
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onHold() {
        setOnHold()
    }

    override fun onUnhold() {
        setActive()
    }

    override fun onStateChanged(state: Int) {
        super.onStateChanged(state)
    }

    /**
     * Establece la conexion como activa (llamada contestada).
     */
    fun setCallActive() {
        setActive()
    }

    /**
     * Finaliza la conexion.
     */
    fun setCallDisconnected(cause: Int = DisconnectCause.LOCAL) {
        setDisconnected(DisconnectCause(cause))
        destroy()
    }
}
