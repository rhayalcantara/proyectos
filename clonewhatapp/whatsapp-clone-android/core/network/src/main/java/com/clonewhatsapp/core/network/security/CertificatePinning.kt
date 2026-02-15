package com.clonewhatsapp.core.network.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import timber.log.Timber

/**
 * Configuracion de Certificate Pinning para OkHttp.
 *
 * Implementa la fijacion de certificados (certificate pinning) para proteger
 * las comunicaciones HTTPS contra ataques de intermediario (MITM).
 *
 * En modo DEBUG, el pinning se deshabilita para facilitar el desarrollo
 * con certificados autofirmados o proxies de depuracion.
 *
 * Los pines se definen como hashes SHA-256 del certificado del servidor.
 * Se incluyen pines de respaldo para soportar la rotacion de certificados.
 */
object CertificatePinning {

    private const val TAG = "CertificatePinning"

    /**
     * Dominio principal del servidor API.
     * Debe coincidir con el dominio configurado en la URL base de produccion.
     */
    private const val API_DOMAIN = "tu-servidor.com"

    /**
     * Pin principal del certificado del servidor (SHA-256).
     * Este es el hash del certificado actual en produccion.
     *
     * Para obtener el pin de un certificado, ejecutar:
     * openssl s_client -connect tu-servidor.com:443 | openssl x509 -pubkey -noout |
     *   openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
     *
     * TODO: Reemplazar con el hash real del certificado de produccion
     */
    private const val PIN_PRIMARY = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    /**
     * Pin de respaldo para rotacion de certificados (SHA-256).
     * Este pin corresponde al certificado que se usara cuando se rote el actual.
     * Es fundamental tener al menos un pin de respaldo para evitar bloqueos
     * cuando el certificado se renueve.
     *
     * TODO: Reemplazar con el hash real del certificado de respaldo
     */
    private const val PIN_BACKUP = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="

    /**
     * Crea un [CertificatePinner] configurado con los pines del servidor.
     *
     * Incluye el pin principal y el pin de respaldo para soportar
     * la rotacion sin interrumpir el servicio.
     *
     * @return [CertificatePinner] con los pines configurados
     */
    fun createCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add(API_DOMAIN, PIN_PRIMARY)
            .add(API_DOMAIN, PIN_BACKUP)
            .build()
    }

    /**
     * Aplica certificate pinning al [OkHttpClient.Builder].
     *
     * Solo aplica el pinning en builds de release. En modo DEBUG se omite
     * para permitir desarrollo con certificados autofirmados, proxies
     * de depuracion (como Charles Proxy), etc.
     *
     * @param builder Builder de OkHttpClient al que se agregara el pinning
     * @param isDebug Indica si la app esta en modo debug (tipicamente BuildConfig.DEBUG)
     * @return El mismo [OkHttpClient.Builder] con o sin pinning aplicado
     */
    fun applyPinning(
        builder: OkHttpClient.Builder,
        isDebug: Boolean
    ): OkHttpClient.Builder {
        if (isDebug) {
            Timber.tag(TAG).d(
                "Certificate pinning deshabilitado en modo DEBUG"
            )
            return builder
        }

        Timber.tag(TAG).d(
            "Certificate pinning habilitado para dominio: %s", API_DOMAIN
        )

        return builder.certificatePinner(createCertificatePinner())
    }

    /**
     * Crea un [CertificatePinner] para un dominio personalizado.
     *
     * Util cuando se necesita comunicar con dominios adicionales (por ejemplo,
     * un CDN para archivos multimedia) que tambien requieren pinning.
     *
     * @param domain Dominio al que aplicar el pinning
     * @param pins Lista de hashes SHA-256 de los certificados
     * @return [CertificatePinner] configurado para el dominio especificado
     */
    fun createCustomPinner(domain: String, vararg pins: String): CertificatePinner {
        val builder = CertificatePinner.Builder()
        pins.forEach { pin ->
            builder.add(domain, pin)
        }
        return builder.build()
    }
}
