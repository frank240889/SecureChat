package dev.franco.securechat.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.franco.securechat.data.database.ChatDataBase

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    fun provideDb(@ApplicationContext context: Context) = ChatDataBase.getDatabase(context)

    @Provides
    fun providesMessagesDao(
        dataBase: ChatDataBase,
    ) = dataBase.messageDao()
}
