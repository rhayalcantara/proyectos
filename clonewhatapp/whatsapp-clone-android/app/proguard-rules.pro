# ============================================================
# ProGuard/R8 - Reglas de ofuscacion para WhatsApp Clone
# ============================================================

# === DTOs y Modelos (Serializacion Gson) ===
# Mantener todas las clases DTO de red para que Gson pueda serializar/deserializar
-keep class com.clonewhatsapp.core.network.dto.** { *; }
# Mantener modelos de dominio que pueden ser serializados
-keep class com.clonewhatsapp.domain.model.** { *; }
# Mantener interfaces API de Retrofit (usa reflexion para crear implementaciones)
-keep class com.clonewhatsapp.core.network.api.** { *; }

# === Retrofit ===
# Mantener anotaciones de Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Mantener clases con @SerializedName de Gson
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit usa interfaces como proxy dinamicos
-keep,allowobfuscation interface com.clonewhatsapp.core.network.api.** {
    @retrofit2.http.* <methods>;
}

# Mantener parametros genéricos de Retrofit (necesarios para converter factories)
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Mantener clases generadas por Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# === Gson ===
# Mantener clases que usan @SerializedName
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Prevenir ofuscacion de campos que Gson usa via reflexion
-keepclassmembers enum * {
    @com.google.gson.annotations.SerializedName <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# === OkHttp ===
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# === SignalR Java Client ===
-keep class com.microsoft.signalr.** { *; }
-dontwarn com.microsoft.signalr.**
# Mantener modelos de mensajes de SignalR (usa reflexion)
-keep class * extends com.microsoft.signalr.HubConnection { *; }

# === WebRTC ===
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
-keep class io.getstream.webrtc.** { *; }
-dontwarn io.getstream.webrtc.**

# === Room (Base de datos) ===
# Mantener entidades de Room (mapeadas a tablas SQL)
-keep class com.clonewhatsapp.core.database.entity.** { *; }
# Mantener DAOs de Room (generan implementaciones en tiempo de compilacion)
-keep interface com.clonewhatsapp.core.database.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# === Hilt / Dagger ===
# Mantener clases anotadas con @Inject
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
# Mantener ViewModels de Hilt
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
# Mantener modulos de Hilt
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-dontwarn dagger.hilt.**

# === Kotlin Coroutines ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# === Coil (Carga de imagenes) ===
-keep class coil3.** { *; }
-dontwarn coil3.**

# === Firebase ===
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# === AndroidX Security (EncryptedSharedPreferences) ===
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# === AndroidX Biometric ===
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# === WorkManager ===
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-dontwarn androidx.work.**

# === Timber (Logging) ===
# Eliminar logs de debug y verbose en release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
}

# === Kotlin Generales ===
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-dontwarn kotlin.**
-dontwarn kotlinx.**

# === Enums ===
# Mantener metodos de enums (values(), valueOf()) usados por reflexion
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# === Parcelable ===
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# === Serializable ===
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
