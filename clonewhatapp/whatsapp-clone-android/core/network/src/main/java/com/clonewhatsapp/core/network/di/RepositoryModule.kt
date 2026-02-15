package com.clonewhatsapp.core.network.di

import com.clonewhatsapp.core.network.repository.AuthRepositoryImpl
import com.clonewhatsapp.core.network.repository.ChatRepositoryImpl
import com.clonewhatsapp.core.network.repository.MessageRepositoryImpl
import com.clonewhatsapp.core.network.repository.StatusRepositoryImpl
import com.clonewhatsapp.core.network.signalr.SignalRRepositoryImpl
import com.clonewhatsapp.core.network.webrtc.CallRepositoryImpl
import com.clonewhatsapp.domain.repository.AuthRepository
import com.clonewhatsapp.domain.repository.CallRepository
import com.clonewhatsapp.domain.repository.ChatRepository
import com.clonewhatsapp.domain.repository.MensajeRepository
import com.clonewhatsapp.domain.repository.SignalRRepository
import com.clonewhatsapp.domain.repository.StatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds AuthRepository interface to AuthRepositoryImpl implementation
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    /**
     * Binds ChatRepository interface to ChatRepositoryImpl implementation
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    /**
     * Binds MensajeRepository interface to MessageRepositoryImpl implementation
     */
    @Binds
    @Singleton
    abstract fun bindMensajeRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MensajeRepository

    /**
     * Binds SignalRRepository interface to SignalRRepositoryImpl implementation
     */
    @Binds
    @Singleton
    abstract fun bindSignalRRepository(
        signalRRepositoryImpl: SignalRRepositoryImpl
    ): SignalRRepository

    /**
     * Binds CallRepository interface to CallRepositoryImpl implementation
     */
    @Binds
    @Singleton
    abstract fun bindCallRepository(
        callRepositoryImpl: CallRepositoryImpl
    ): CallRepository

    /**
     * Binds StatusRepository interface to StatusRepositoryImpl implementation
     */
    @Binds
    @Singleton
    abstract fun bindStatusRepository(
        statusRepositoryImpl: StatusRepositoryImpl
    ): StatusRepository
}
