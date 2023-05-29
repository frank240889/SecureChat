package dev.franco.securechat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.franco.comm.DiscoveryDevice
import dev.franco.comm.DiscoveryDeviceImpl
import dev.franco.comm.RegistrationService
import dev.franco.comm.RegistrationServiceImpl
import dev.franco.securechat.background.ClientConnectionManager
import dev.franco.securechat.background.ConnectionManager
import dev.franco.securechat.background.ServerConnectionManager
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
abstract class CommunicationModule {

    @ClientServiceManager
    @Binds
    abstract fun bindsCommunicationManager(
        clientConnectionManager: ClientConnectionManager,
    ): ConnectionManager

    @ServerServiceManager
    @Binds
    abstract fun bindsServerManager(
        serverConnectionManager: ServerConnectionManager,
    ): ConnectionManager

    @Binds
    abstract fun bindsSearchDevice(
        searchDeviceImpl: DiscoveryDeviceImpl,
    ): DiscoveryDevice

    @Binds
    abstract fun bindsExposeDevice(
        exposeDeviceImpl: RegistrationServiceImpl,
    ): RegistrationService
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ServerServiceManager

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ClientServiceManager
