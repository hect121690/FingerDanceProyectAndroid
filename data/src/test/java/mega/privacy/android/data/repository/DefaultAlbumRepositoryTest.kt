package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.facade.AlbumStringResourceGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.model.GlobalUpdate.OnSetElementsUpdate
import mega.privacy.android.data.model.GlobalUpdate.OnSetsUpdate
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.NodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaSetList
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAlbumRepositoryTest {
    private lateinit var underTest: AlbumRepository

    private val nodeRepository = mock<NodeRepository>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val userSetMapper: UserSetMapper = ::createUserSet
    private val albumStringResourceGateway = mock<AlbumStringResourceGateway>()

    private val testName = "Album1"

    @Before
    fun setUp() {
        whenever(nodeRepository.monitorNodeUpdates())
            .thenReturn(flowOf())
    }

    @Test
    fun `test that createAlbum returns an instance of UserSet`() = runTest {
        val api = mock<MegaApiJava>()

        val testMegaSet = mock<MegaSet> {
            on { id() }.thenReturn(1L)
            on { name() }.thenReturn(testName)
        }

        val userSet = createUserSet(
            testMegaSet.id(),
            testMegaSet.name(),
            null,
            testMegaSet.ts()
        )

        val request = mock<MegaRequest> {
            on { megaSet }.thenReturn(testMegaSet)
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(megaApiGateway.createSet(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        underTest = createUnderTest(this)
        val actualNewAlbum = underTest.createAlbum(testName)

        assertEquals(actualNewAlbum.id, userSet.id)
        assertEquals(actualNewAlbum.name, testName)
    }

    @Test
    fun `test that addPhotosToAlbum triggers adding sharing progress`() =
        runTest {
            val testAlbumId = AlbumId(1L)
            val testPhotos = listOf(NodeId(1L), NodeId(2L))

            whenever(megaApiGateway.createSetElement(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock {
                        on { errorCode }.thenReturn(MegaError.API_OK)
                    }
                )
            }

            underTest = createUnderTest(this)
            underTest.addPhotosToAlbum(albumID = testAlbumId, photoIDs = testPhotos)

            underTest.observeAlbumPhotosAddingProgress(testAlbumId).test {
                val progress = awaitItem()
                assertThat(progress?.isProgressing).isEqualTo(false)
                assertThat(progress?.totalAddedPhotos).isEqualTo(testPhotos.size)
            }
        }

    @Test
    fun `test that removePhotosFromAlbum triggers adding sharing progress`() =
        runTest {
            val testAlbumId = AlbumId(1L)
            val testPhotos = (1..2L).map {
                AlbumPhotoId(it, NodeId(1L), testAlbumId)
            }

            whenever(megaApiGateway.removeSetElement(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock {
                        on { errorCode }.thenReturn(MegaError.API_OK)
                    }
                )
            }

            underTest = createUnderTest(this)
            underTest.removePhotosFromAlbum(albumID = testAlbumId, photoIDs = testPhotos)

            underTest.observeAlbumPhotosRemovingProgress(testAlbumId).test {
                val progress = awaitItem()
                assertThat(progress?.isProgressing).isEqualTo(false)
                assertThat(progress?.totalRemovedPhotos).isEqualTo(testPhotos.size)
            }
        }

    @Test
    fun `test that getUserSets returns a single value if only one set exists`() = runTest {
        val expectedId = 1L
        val expectedName = "Album 1"

        val megaSet = mock<MegaSet> {
            on { id() }.thenReturn(expectedId)
            on { name() }.thenReturn(expectedName)
        }
        val megaSetList = mock<MegaSetList> {
            on { size() }.thenReturn(1L)
            on { get(any()) }.thenReturn(megaSet)
        }
        whenever(megaApiGateway.getSets()).thenReturn(megaSetList)

        val megaSetElementList = mock<MegaSetElementList> {
            on { size() }.thenReturn(0L)
        }
        whenever(megaApiGateway.getSetElements(any())).thenReturn(megaSetElementList)

        underTest = createUnderTest(this)
        val actualUserSets = underTest.getAllUserSets()

        assertThat(actualUserSets.size).isEqualTo(1)
        assertThat(actualUserSets[0].id).isEqualTo(expectedId)
        assertThat(actualUserSets[0].name).isEqualTo(expectedName)
    }

    @Test
    fun `test that getUserSets returns a the correct number of values if multiple sets exist`() =
        runTest {
            val expectedId = 1L
            val expectedName = "Album 1"
            val expectedSize = 5

            val megaSet = mock<MegaSet> {
                on { id() }.thenReturn(expectedId)
                on { name() }.thenReturn(expectedName)
            }
            val megaSetList = mock<MegaSetList> {
                on { size() }.thenReturn(expectedSize.toLong())
                on { get(any()) }.thenReturn(megaSet)
            }
            whenever(megaApiGateway.getSets()).thenReturn(megaSetList)

            val megaSetElementList = mock<MegaSetElementList> {
                on { size() }.thenReturn(0L)
            }
            whenever(megaApiGateway.getSetElements(any())).thenReturn(megaSetElementList)

            underTest = createUnderTest(this)
            val actualUserSets = underTest.getAllUserSets()

            assertThat(actualUserSets.size).isEqualTo(expectedSize)
        }

    @Test
    fun `getAlbumElementIDs should return correct result`() = runTest {
        val expectedNode = 1L
        val albumId = AlbumId(expectedNode)

        val megaSetElement = mock<MegaSetElement> {
            on { node() }.thenReturn(expectedNode)
            on { id() }.thenReturn(expectedNode)
        }
        val megaSetElementList = mock<MegaSetElementList> {
            on { size() }.thenReturn(1L)
            on { get(any()) }.thenReturn(megaSetElement)
        }
        whenever(megaApiGateway.getSetElements(any())).thenReturn(megaSetElementList)

        underTest = createUnderTest(this)
        val actualElementIds = underTest.getAlbumElementIDs(albumId)

        assertThat(actualElementIds.size).isEqualTo(1)
        assertThat(actualElementIds[0].nodeId.longValue).isEqualTo(expectedNode)
    }

    @Test
    fun `test that monitorUserSetsUpdate emits correct result`() = runTest {
        val expectedUserSets = (1..3L).map {
            createUserSet(
                id = it,
                name = "Album $it",
                cover = -1L,
                modificationTime = it,
            )
        }

        val megaSets = expectedUserSets.map { set ->
            mock<MegaSet> {
                on { id() }.thenReturn(set.id)
                on { name() }.thenReturn(set.name)
                on { cover() }.thenReturn(set.cover)
                on { ts() }.thenReturn(set.modificationTime)
            }
        }

        whenever(megaApiGateway.globalUpdates)
            .thenReturn(flowOf(OnSetsUpdate(ArrayList(megaSets))))

        underTest = createUnderTest(this)
        underTest.monitorUserSetsUpdate().test {
            val userSets = awaitItem()
            assertThat(userSets).isNotEmpty()
        }
    }

    @Test
    fun `test that monitorAlbumElementIds emits correct result`() = runTest {
        val albumId = AlbumId(1L)
        val expectedElementIds = (1..3L).map {
            AlbumPhotoId(
                id = it,
                nodeId = NodeId(it),
                albumId = albumId,
            )
        }

        val megaSetElements = expectedElementIds.map { node ->
            mock<MegaSetElement> {
                on { node() }.thenReturn(node.id)
                on { id() }.thenReturn(node.id)
                on { setId() }.thenReturn(albumId.id)
            }
        }

        whenever(megaApiGateway.globalUpdates)
            .thenReturn(flowOf(OnSetElementsUpdate(ArrayList(megaSetElements))))

        underTest = createUnderTest(this)
        underTest.monitorAlbumElementIds(albumId).test {
            val elementIds = awaitItem()
            assertThat(elementIds).isNotEmpty()
        }
    }

    @Test
    fun `test that get user set returns correct result`() = runTest {
        val albumId = AlbumId(1L)
        val expectedUserSet = createUserSet(
            id = 1L,
            name = "Album 1",
            cover = null,
            modificationTime = 0L,
        )

        val megaSet = mock<MegaSet> {
            with(expectedUserSet) {
                on { id() }.thenReturn(id)
                on { name() }.thenReturn(name)
                on { cover() }.thenReturn(-1L)
            }
        }
        whenever(megaApiGateway.getSet(any())).thenReturn(megaSet)

        underTest = createUnderTest(this)
        val actualUserSet = underTest.getUserSet(albumId)
        assertThat(expectedUserSet).isEqualTo(actualUserSet)
    }

    @Test
    fun `test that remove albums returns correct result`() = runTest {
        val albumIds = listOf(
            AlbumId(1L),
            AlbumId(2L),
            AlbumId(3L),
        )

        whenever(megaApiGateway.removeSet(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                mock {
                    on { errorCode }.thenReturn(MegaError.API_OK)
                },
            )
        }

        try {
            underTest = createUnderTest(this)
            underTest.removeAlbums(albumIds)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }

    @Test
    fun `test that rename album then returns the new album name`() = runTest {
        val api = mock<MegaApiJava>()

        val newName = "newAlbum"

        val request = mock<MegaRequest> {
            on { text }.thenReturn(newName)
        }

        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(megaApiGateway.updateSetName(any(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        underTest = createUnderTest(this)
        val actualName = underTest.updateAlbumName(AlbumId(1L), newName)

        assertEquals(newName, actualName)
    }

    @Test
    fun `test that getProscribedAlbumTitles return a list of strings`() = runTest {
        whenever(albumStringResourceGateway.getSystemAlbumNames()).thenReturn(listOf("abc"))
        whenever(albumStringResourceGateway.getProscribedStrings()).thenReturn(listOf("123"))

        underTest = createUnderTest(this)
        val actualStringsList = underTest.getProscribedAlbumTitles()

        verify(albumStringResourceGateway).getSystemAlbumNames()
        verify(albumStringResourceGateway).getProscribedStrings()
        assertEquals(listOf("abc", "123"), actualStringsList)
    }

    @Test
    fun `test that update album cover executes properly`() = runTest {
        val albumId = AlbumId(1L)
        val elementId = NodeId(2L)

        try {
            underTest = createUnderTest(this)
            underTest.updateAlbumCover(albumId, elementId)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }

    private fun createUnderTest(coroutineScope: CoroutineScope) = DefaultAlbumRepository(
        nodeRepository = nodeRepository,
        megaApiGateway = megaApiGateway,
        userSetMapper = userSetMapper,
        isNodeInRubbish = { false },
        albumStringResourceGateway = albumStringResourceGateway,
        ioDispatcher = UnconfinedTestDispatcher(),
        appScope = coroutineScope,
    )

    private fun createUserSet(
        id: Long,
        name: String,
        cover: Long?,
        modificationTime: Long,
    ): UserSet = object : UserSet {
        override val id: Long = id

        override val name: String = name

        override val cover: Long? = cover

        override val modificationTime: Long = modificationTime

        override fun equals(other: Any?): Boolean {
            val otherSet = other as? UserSet ?: return false
            return id == otherSet.id
                    && name == otherSet.name
                    && cover == otherSet.cover
                    && modificationTime == otherSet.modificationTime
        }
    }
}
