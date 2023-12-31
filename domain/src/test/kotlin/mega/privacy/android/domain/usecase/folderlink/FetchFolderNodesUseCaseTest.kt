package mega.privacy.android.domain.usecase.folderlink

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.folderlink.FetchNodeRequestResult
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FetchFolderNodesUseCaseTest {
    private lateinit var underTest: FetchFolderNodesUseCase
    private val repository: FolderLinkRepository = mock()
    private val addNodeType: AddNodeType = mock()
    private val getFolderLinkChildrenNodesUseCase: GetFolderLinkChildrenNodesUseCase = mock()
    private val unTypeNode = mock<UnTypedNode>()
    private val typedFolderNode = mock<TypedFolderNode>()
    private val typedNode = mock<TypedNode>()
    private val typedNodeList = mock<List<TypedNode>>()

    @Before
    fun setUp() {
        underTest =
            FetchFolderNodesUseCase(repository, addNodeType, getFolderLinkChildrenNodesUseCase)
    }

    @Test
    fun `Test that updateLastPublicHandle, addNodeType and getChildrenNodes are invoked`() =
        runTest {
            val folderSubHandle = "123"
            val nodeHandle = 1234L
            val result = FetchNodeRequestResult(nodeHandle, false)
            whenever(repository.fetchNodes()).thenReturn(result)
            whenever(repository.getRootNode()).thenReturn(unTypeNode)
            whenever(addNodeType(unTypeNode)).thenReturn(typedFolderNode)
            whenever(getFolderLinkChildrenNodesUseCase(any(), anyOrNull())).thenReturn(typedNodeList)

            underTest(folderSubHandle)
            verify(repository).updateLastPublicHandle(nodeHandle)
            verify(addNodeType, times(1)).invoke(unTypeNode)
            verify(getFolderLinkChildrenNodesUseCase, times(1)).invoke(any(), anyOrNull())
        }

    @Test
    fun `Test that on getting flag value as true InvalidDecryptionKey exception is thrown`() =
        runTest {
            val folderSubHandle = "123"
            val nodeHandle = 1234L
            val result = FetchNodeRequestResult(nodeHandle, true)
            whenever(repository.fetchNodes()).thenReturn(result)
            whenever(repository.getRootNode()).thenReturn(unTypeNode)

            Assert.assertThrows(FetchFolderNodesException.InvalidDecryptionKey::class.java) {
                runBlocking { underTest(folderSubHandle) }
            }
        }

    @Test
    fun `Test that on getting root node as null GenericError exception is thrown`() = runTest {
        val folderSubHandle = "123"
        val nodeHandle = 1234L
        val result = FetchNodeRequestResult(nodeHandle, false)
        whenever(repository.fetchNodes()).thenReturn(result)
        whenever(repository.getRootNode()).thenReturn(null)

        Assert.assertThrows(FetchFolderNodesException.GenericError::class.java) {
            runBlocking { underTest(folderSubHandle) }
        }
    }

    @Test
    fun `Test that getFolderLinkNode is invoked on valid rootNode and folderSubHandle`() = runTest {
        val folderSubHandle = "123"
        val nodeHandle = 1234L
        val result = FetchNodeRequestResult(nodeHandle, false)
        whenever(repository.fetchNodes()).thenReturn(result)
        whenever(repository.getRootNode()).thenReturn(unTypeNode)
        whenever(addNodeType(unTypeNode)).thenReturn(typedFolderNode)
        whenever(getFolderLinkChildrenNodesUseCase(any(), anyOrNull())).thenReturn(typedNodeList)

        underTest(folderSubHandle)
        verify(repository).updateLastPublicHandle(nodeHandle)
        verify(addNodeType, times(1)).invoke(unTypeNode)
        verify(repository).getFolderLinkNode(folderSubHandle)
    }

    @Test
    fun `Test that on root node not able to cast GenericError exception is thrown`() = runTest {
        val folderSubHandle = "123"
        val nodeHandle = 1234L
        val result = FetchNodeRequestResult(nodeHandle, false)
        whenever(repository.fetchNodes()).thenReturn(result)
        whenever(repository.getRootNode()).thenReturn(unTypeNode)
        whenever(addNodeType(unTypeNode)).thenReturn(typedNode)

        Assert.assertThrows(FetchFolderNodesException.GenericError::class.java) {
            runBlocking { underTest(folderSubHandle) }
        }
    }
}