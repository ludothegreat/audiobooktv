package xyz.ludothegreat.audiobooktv.data.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CredentialStorageModule {
    @Binds
    @Singleton
    abstract fun bindCredentialStorage(store: CredentialStore): CredentialStorage
}
