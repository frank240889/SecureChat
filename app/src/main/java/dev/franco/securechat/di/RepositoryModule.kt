package dev.franco.securechat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.franco.securechat.data.source.local.ClientConnectionRepository
import dev.franco.securechat.data.source.local.DeviceConnectionRepository
import dev.franco.securechat.data.source.local.MessagesRepository
import dev.franco.securechat.data.source.local.MessagesRepositoryImpl
import dev.franco.securechat.data.source.local.ServerConnectionRepository
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindsChatRepository(
        messagesRepositoryImpl: MessagesRepositoryImpl,
    ): MessagesRepository

    @Singleton
    @ServerDeviceRepository
    @Binds
    abstract fun bindsServerConnectionRepository(
        serverConnectionRepository: ServerConnectionRepository,
    ): DeviceConnectionRepository

    @Singleton
    @ClientDeviceRepository
    @Binds
    abstract fun bindsClientConnectionRepository(
        clientConnectionRepository: ClientConnectionRepository,
    ): DeviceConnectionRepository
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ServerDeviceRepository

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ClientDeviceRepository
