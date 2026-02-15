package com.clonewhatsapp.core.network.di

import com.clonewhatsapp.core.network.api.AuthApi
import com.clonewhatsapp.core.network.api.ChatsApi
import com.clonewhatsapp.core.network.api.MessagesApi
import com.clonewhatsapp.core.network.api.PushApi
import com.clonewhatsapp.core.network.api.StatusApi
import com.clonewhatsapp.core.network.auth.AuthInterceptor
import com.clonewhatsapp.core.network.security.CertificatePinning
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Hilt module providing network dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides Gson instance for JSON serialization
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
    }

    /**
     * Provides logging interceptor for debug builds
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (com.clonewhatsapp.core.network.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Provides trust manager that accepts all certificates (for development only)
     * WARNING: This should NOT be used in production
     */
    @Provides
    @Singleton
    fun provideUnsafeTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    /**
     * Proporciona OkHttpClient con interceptores de autenticacion y logging.
     * Incluye bypass SSL para certificados autofirmados (solo en desarrollo).
     * En release: aplica certificate pinning para proteccion contra MITM.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        trustManager: X509TrustManager
    ): OkHttpClient {
        val isDebug = com.clonewhatsapp.core.network.BuildConfig.DEBUG

        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Solo bypass SSL en builds de debug
        if (isDebug) {
            try {
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())

                builder.sslSocketFactory(sslContext.socketFactory, trustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Aplicar certificate pinning solo en release
        CertificatePinning.applyPinning(builder, isDebug)

        return builder.build()
    }

    /**
     * Provides Retrofit instance
     * @param apiUrl Base URL injected from AppModule
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
        @Named("apiUrl") apiUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Provides AuthApi instance
     */
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    /**
     * Provides ChatsApi instance
     */
    @Provides
    @Singleton
    fun provideChatsApi(retrofit: Retrofit): ChatsApi {
        return retrofit.create(ChatsApi::class.java)
    }

    /**
     * Provides MessagesApi instance
     */
    @Provides
    @Singleton
    fun provideMessagesApi(retrofit: Retrofit): MessagesApi {
        return retrofit.create(MessagesApi::class.java)
    }

    /**
     * Provides StatusApi instance
     */
    @Provides
    @Singleton
    fun provideStatusApi(retrofit: Retrofit): StatusApi {
        return retrofit.create(StatusApi::class.java)
    }

    /**
     * Provides PushApi instance para registro de tokens FCM
     */
    @Provides
    @Singleton
    fun providePushApi(retrofit: Retrofit): PushApi {
        return retrofit.create(PushApi::class.java)
    }
}
