package com.clonewhatsapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.clonewhatsapp.core.database.WhatsAppDatabase
import com.clonewhatsapp.core.database.dao.ChatDao
import com.clonewhatsapp.core.database.dao.MensajeDao
import com.clonewhatsapp.core.database.dao.MensajePendienteDao
import com.clonewhatsapp.core.database.dao.UsuarioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): WhatsAppDatabase {
        return Room.databaseBuilder(
            context,
            WhatsAppDatabase::class.java,
            "whatsapp_clone.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideChatDao(database: WhatsAppDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    fun provideMensajeDao(database: WhatsAppDatabase): MensajeDao {
        return database.mensajeDao()
    }

    @Provides
    fun provideUsuarioDao(database: WhatsAppDatabase): UsuarioDao {
        return database.usuarioDao()
    }

    @Provides
    fun provideMensajePendienteDao(database: WhatsAppDatabase): MensajePendienteDao {
        return database.mensajePendienteDao()
    }
}
