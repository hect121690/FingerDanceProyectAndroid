package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.repository.*
import mega.privacy.android.app.domain.repository.*
import kotlin.contracts.ExperimentalContracts

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @ExperimentalContracts
    @Binds
    abstract fun bindAccountRepository(repository: DefaultAccountRepository): AccountRepository

    @Binds
    abstract fun bindNetworkRepository(repository: DefaultNetworkRepository): NetworkRepository

    @Binds
    abstract fun bindChatRepository(repository: DefaultChatRepository): ChatRepository

    @Binds
    abstract fun bindFilesRepository(implementation: MegaFilesRepository): FilesRepository

    @Binds
    abstract fun bindFavouritesRepository(repository: DefaultFavouritesRepository): FavouritesRepository
}