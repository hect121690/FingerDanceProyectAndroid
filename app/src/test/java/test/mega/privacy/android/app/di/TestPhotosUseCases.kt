package test.mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.photos.PhotosUseCases
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetPhotosByIds
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdUseCase
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.imageviewer.DefaultGetImageByNodeHandle
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodeHandle
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [PhotosUseCases::class],
    components = [SingletonComponent::class, ViewModelComponent::class]
)
@Module
object TestPhotosUseCases {

    @Provides
    fun provideGetDefaultAlbumPhotos(): GetDefaultAlbumPhotos = mock()

    @Provides
    fun provideGetPhotosByFolderIdUseCase(): GetPhotosByFolderIdUseCase = mock()

    @Provides
    fun provideObserveAlbumPhotosAddingProgressUseCase(): ObserveAlbumPhotosAddingProgress = mock()

    @Provides
    fun provideUpdateAlbumPhotosAddingProgressCompletedUseCase(): UpdateAlbumPhotosAddingProgressCompleted =
        mock()

    @Provides
    fun provideObserveAlbumPhotosRemovingProgressUseCase(): ObserveAlbumPhotosRemovingProgress =
        mock()

    @Provides
    fun provideUpdateAlbumPhotosRemovingProgressCompletedUseCase(): UpdateAlbumPhotosRemovingProgressCompleted =
        mock()

    @Provides
    fun provideGetPhotosByIdsUseCase(): GetPhotosByIds = mock()

    @Provides
    fun provideGetImageByNodeHandle(): GetImageByNodeHandle = mock()
}