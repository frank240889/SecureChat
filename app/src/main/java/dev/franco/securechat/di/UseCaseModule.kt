package dev.franco.securechat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.franco.securechat.usecase.GetMessagesUseCase
import dev.franco.securechat.usecase.GetMessagesUseCaseImpl
import dev.franco.securechat.usecase.SendMessageUseCase
import dev.franco.securechat.usecase.SendMessageUseCaseImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {
    @Binds
    abstract fun bindsGetMessageUseCase(
        getMessagesUseCaseImpl: GetMessagesUseCaseImpl,
    ): GetMessagesUseCase

    @Binds
    abstract fun bindsSendMessageUseCase(
        sendMessageUseCase: SendMessageUseCaseImpl,
    ): SendMessageUseCase
}
