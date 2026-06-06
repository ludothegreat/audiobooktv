package xyz.ludothegreat.audiobooktv.data.cache

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryCacheModule {
    @Binds
    @Singleton
    abstract fun bindLibraryCacheStorage(impl: LibraryCache): LibraryCacheStorage
}
