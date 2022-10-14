package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.repository.DefaultChatRepository
import mega.privacy.android.data.repository.DefaultEnvironmentRepository
import mega.privacy.android.data.repository.DefaultFeatureFlagRepository
import mega.privacy.android.data.repository.DefaultGalleryFilesRepository
import mega.privacy.android.data.repository.DefaultNetworkRepository
import mega.privacy.android.data.repository.DefaultNotificationsRepository
import mega.privacy.android.data.repository.DefaultPushesRepository
import mega.privacy.android.data.repository.DefaultRecentActionsRepository
import mega.privacy.android.data.repository.DefaultSortOrderRepository
import mega.privacy.android.data.repository.DefaultStatisticsRepository
import mega.privacy.android.data.repository.DefaultSupportRepository
import mega.privacy.android.data.repository.RecentActionsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.FeatureFlagRepository
import mega.privacy.android.domain.repository.GalleryFilesRepository
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.repository.SortOrderRepository
import mega.privacy.android.domain.repository.StatisticsRepository
import mega.privacy.android.domain.repository.SupportRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun bindNetworkRepository(repository: DefaultNetworkRepository): NetworkRepository

    @Binds
    abstract fun bindDeviceRepository(implementation: DefaultEnvironmentRepository): EnvironmentRepository

    @Binds
    @Singleton
    abstract fun bindFeatureFlagRepository(repository: DefaultFeatureFlagRepository): FeatureFlagRepository

    @Binds
    abstract fun bindStatisticsRepository(repository: DefaultStatisticsRepository): StatisticsRepository

    @Binds
    abstract fun bindGalleryFilesRepository(repository: DefaultGalleryFilesRepository): GalleryFilesRepository

    /**
     * Bind recent actions repository
     */
    @Binds
    abstract fun bindRecentActionsRepository(repository: DefaultRecentActionsRepository): RecentActionsRepository

    @Binds
    abstract fun bindSupportRepository(implementation: DefaultSupportRepository): SupportRepository

    @Binds
    abstract fun bindNotificationsRepository(repository: DefaultNotificationsRepository): NotificationsRepository

    /**
     * Bind sort order repository
     */
    @Binds
    abstract fun bindSortOrderRepository(repository: DefaultSortOrderRepository): SortOrderRepository

    @Binds
    abstract fun bindChatRepository(repository: DefaultChatRepository): ChatRepository

    @Binds
    abstract fun bindPushesRepository(repository: DefaultPushesRepository): PushesRepository
}