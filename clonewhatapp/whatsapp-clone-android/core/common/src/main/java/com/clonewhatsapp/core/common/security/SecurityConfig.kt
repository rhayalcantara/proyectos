package com.clonewhatsapp.core.common.security

/**
 * Configuracion central de seguridad de la aplicacion.
 *
 * Contiene constantes y configuraciones relacionadas con la seguridad
 * que se utilizan en diferentes modulos de la aplicacion. Centraliza
 * estos valores para facilitar su revision y mantenimiento.
 */
object SecurityConfig {

    // === ALMACENAMIENTO SEGURO ===

    /**
     * Nombre del archivo de EncryptedSharedPreferences para tokens de autenticacion.
     * Usado por SecureTokenStorage en el modulo core:network.
     */
    const val AUTH_PREFS_NAME = "secure_auth_prefs"

    /**
     * Nombre del archivo de EncryptedSharedPreferences para configuracion biometrica.
     * Usado por BiometricSettingsViewModel en el modulo feature:auth.
     */
    const val BIOMETRIC_PREFS_NAME = "biometric_settings_prefs"

    // === RED ===

    /**
     * Timeout de conexion HTTP en segundos.
     */
    const val NETWORK_CONNECT_TIMEOUT_SECONDS = 30L

    /**
     * Timeout de lectura HTTP en segundos.
     */
    const val NETWORK_READ_TIMEOUT_SECONDS = 30L

    /**
     * Timeout de escritura HTTP en segundos.
     */
    const val NETWORK_WRITE_TIMEOUT_SECONDS = 30L

    /**
     * Referencia al archivo de configuracion de seguridad de red.
     * Ubicacion: app/src/main/res/xml/network_security_config.xml
     *
     * Configuracion actual:
     * - Permite trafico en texto claro solo para IPs de desarrollo local
     * - En debug: confiar en certificados instalados por el usuario
     * - En release: solo confiar en certificados del sistema
     */
    const val NETWORK_SECURITY_CONFIG_FILE = "network_security_config.xml"

    // === CERTIFICATE PINNING ===

    /**
     * Indica si el certificate pinning debe estar habilitado.
     * En debug se deshabilita para permitir proxies de depuracion.
     */
    const val CERTIFICATE_PINNING_ENABLED_RELEASE = true

    /**
     * Certificate pinning deshabilitado en debug por defecto.
     */
    const val CERTIFICATE_PINNING_ENABLED_DEBUG = false

    // === AUTENTICACION BIOMETRICA ===

    /**
     * Tiempo maximo (en milisegundos) que la autenticacion biometrica
     * permanece valida antes de requerir re-autenticacion.
     * Valor actual: 5 minutos.
     */
    const val BIOMETRIC_VALIDITY_DURATION_MS = 5 * 60 * 1000L

    /**
     * Tiempo de inactividad (en milisegundos) tras el cual se bloquea la app.
     * Valor actual: 1 minuto en segundo plano.
     */
    const val APP_LOCK_TIMEOUT_MS = 60 * 1000L

    // === TOKENS ===

    /**
     * Tiempo de vida del token JWT en milisegundos.
     * El token se refresca automaticamente cuando esta proximo a expirar.
     * Valor actual: 24 horas.
     */
    const val TOKEN_EXPIRATION_MS = 24 * 60 * 60 * 1000L

    /**
     * Tiempo antes de la expiracion (en milisegundos) en el que se inicia
     * el refresco automatico del token.
     * Valor actual: 1 hora antes de expirar.
     */
    const val TOKEN_REFRESH_THRESHOLD_MS = 60 * 60 * 1000L

    // === OFUSCACION (ProGuard/R8) ===

    /**
     * Paquetes que NO deben ser ofuscados por R8/ProGuard:
     * - DTOs de red: se serializan/deserializan con Gson
     * - Modelos de dominio: pueden usarse con Gson indirectamente
     * - Interfaces API: Retrofit las usa via reflexion
     * - Entidades de Room: se mapean a tablas de la base de datos
     */
    val KEEP_PACKAGES = listOf(
        "com.clonewhatsapp.core.network.dto",
        "com.clonewhatsapp.core.network.api",
        "com.clonewhatsapp.domain.model",
        "com.clonewhatsapp.core.database.entity"
    )

    // === FLAGS DE SEGURIDAD SEGUN TIPO DE BUILD ===

    /**
     * Configuracion de seguridad para builds de debug.
     *
     * En debug se relajan restricciones para facilitar el desarrollo:
     * - SSL bypass habilitado para certificados autofirmados
     * - Certificate pinning deshabilitado
     * - Logs de red detallados (BODY level)
     * - Reporte de auditoria de permisos habilitado
     */
    object Debug {
        const val SSL_BYPASS_ENABLED = true
        const val CERTIFICATE_PINNING_ENABLED = false
        const val VERBOSE_NETWORK_LOGGING = true
        const val PERMISSION_AUDIT_ENABLED = true
        const val LOG_SENSITIVE_DATA = false // Nunca loguear datos sensibles, ni en debug
    }

    /**
     * Configuracion de seguridad para builds de release.
     *
     * En release se aplican todas las restricciones de seguridad:
     * - SSL bypass deshabilitado
     * - Certificate pinning habilitado
     * - Logs de red minimizados (NONE level)
     * - Reporte de auditoria deshabilitado
     * - R8/ProGuard habilitado para ofuscacion
     */
    object Release {
        const val SSL_BYPASS_ENABLED = false
        const val CERTIFICATE_PINNING_ENABLED = true
        const val VERBOSE_NETWORK_LOGGING = false
        const val PERMISSION_AUDIT_ENABLED = false
        const val LOG_SENSITIVE_DATA = false
    }
}
