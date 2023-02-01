package mega.privacy.android.domain.usecase.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.UpdateAlbumNameUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultUpdateAlbumNameUseCaseTest {

    lateinit var underTest: UpdateAlbumNameUseCase
    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = DefaultUpdateAlbumNameUseCase(albumRepository = albumRepository)
    }

    @Test
    fun `test that album get renamed`() =
        runTest {
            val albumId = AlbumId(1L)
            val newAlbumName = "newAlbum"

            underTest(albumId = albumId, newName = newAlbumName)

            verify(albumRepository, times(1))
                .updateAlbumName(
                    albumId = albumId,
                    newName = newAlbumName
                )
        }
}