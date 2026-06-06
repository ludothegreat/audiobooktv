package xyz.ludothegreat.audiobooktv.domain

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindLibraryBookSource(impl: LibraryRepository): LibraryBookSource
}
