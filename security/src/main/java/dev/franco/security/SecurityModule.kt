package dev.franco.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class SecurityModule {

    @Singleton
    @Binds
    abstract fun bindsEncryptionManager(
        encryptionManagerImpl: EncryptionManagerImpl,
    ): EncryptionManager

    @Singleton
    @Binds
    abstract fun bindsStorage(storageImpl: StorageImpl): Storage
}
