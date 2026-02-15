package com.clonewhatsapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.clonewhatsapp.core.database.converter.Converters
import com.clonewhatsapp.core.database.dao.ChatDao
import com.clonewhatsapp.core.database.dao.MensajeDao
import com.clonewhatsapp.core.database.dao.MensajePendienteDao
import com.clonewhatsapp.core.database.dao.UsuarioDao
import com.clonewhatsapp.core.database.entity.ChatEntity
import com.clonewhatsapp.core.database.entity.ChatParticipantEntity
import com.clonewhatsapp.core.database.entity.MensajeEntity
import com.clonewhatsapp.core.database.entity.MensajePendienteEntity
import com.clonewhatsapp.core.database.entity.UsuarioEntity

@Database(
    entities = [
        ChatEntity::class,
        MensajeEntity::class,
        UsuarioEntity::class,
        ChatParticipantEntity::class,
        MensajePendienteEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WhatsAppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    abstract fun mensajeDao(): MensajeDao

    abstract fun usuarioDao(): UsuarioDao

    abstract fun mensajePendienteDao(): MensajePendienteDao
}
