package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.node.DefaultFileNode
import mega.privacy.android.data.model.node.DefaultFolderNode
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class NodeMapperTest {
    private var underTest = NodeMapper()

    private val expectedName = "testName"
    private val expectedSize = 1000L
    private val expectedLabel = MegaNode.NODE_LBL_RED
    private val expectedId = 1L
    private val expectedParentId = 2L
    private val expectedBase64Id = "1L"
    private val expectedModificationTime = 123L
    private val expectedFingerprint = "fingerprint"
    private val expectedDuration = 100
    private val expectedPublicLink = "publicLink"
    private val expectedPublicLinkCreationTime = 456L

    @Test
    fun `test that files are mapped if isFile is true`() = runTest {
        val megaNode = getMockNode(isFile = true)
        val actual =
            underTest(
                megaNode = megaNode,
                thumbnailPath = { null },
                previewPath = { null },
                fullSizePath = { null },
                hasVersion = { false },
                numberOfChildFolders = { 0 },
                numberOfChildFiles = { 1 },
                isInRubbish = { false },
                fileTypeInfoMapper = { PdfFileTypeInfo },
                isPendingShare = { false },
            )

        assertThat(actual).isInstanceOf(DefaultFileNode::class.java)
    }

    @Test
    fun `test that folders are mapped if isFile is false`() = runTest {
        val megaNode = getMockNode(isFile = false)
        val actual =
            underTest(
                megaNode = megaNode,
                thumbnailPath = { null },
                previewPath = { null },
                fullSizePath = { null },
                hasVersion = { false },
                numberOfChildFolders = { 0 },
                numberOfChildFiles = { 1 },
                isInRubbish = { false },
                fileTypeInfoMapper = { PdfFileTypeInfo },
                isPendingShare = { false },
            )

        assertThat(actual).isInstanceOf(DefaultFolderNode::class.java)
    }

    @Test
    fun `test that values returned by gateway are used`() = runTest {
        val node = getMockNode(isFile = false)
        val expectedHasVersion = true
        val expectedNumChildFolders = 2
        val expectedNumChildFiles = 3
        val gateway = mock<MegaApiGateway> {
            onBlocking { hasVersion(node) }.thenReturn(expectedHasVersion)
            onBlocking { getNumChildFolders(node) }.thenReturn(expectedNumChildFolders)
            onBlocking { getNumChildFiles(node) }.thenReturn(expectedNumChildFiles)
            onBlocking { isInRubbish(node) }.thenReturn(true)
            onBlocking { isPendingShare(node) }.thenReturn(true)
        }

        val actual = underTest(
            megaNode = node,
            thumbnailPath = { null },
            previewPath = { null },
            fullSizePath = { null },
            hasVersion = gateway::hasVersion,
            numberOfChildFolders = gateway::getNumChildFolders,
            numberOfChildFiles = gateway::getNumChildFiles,
            isInRubbish = gateway::isInRubbish,
            fileTypeInfoMapper = { PdfFileTypeInfo },
            isPendingShare = gateway::isPendingShare,
        )

        assertThat(actual.name).isEqualTo(expectedName)
        assertThat(actual.label).isEqualTo(expectedLabel)
        assertThat(actual.hasVersion).isEqualTo(expectedHasVersion)
        assertThat(actual.id).isEqualTo(NodeId(expectedId))
        assertThat(actual.parentId).isEqualTo(NodeId(expectedParentId))
        assertThat(actual.base64Id).isEqualTo(expectedBase64Id)
        assertThat(actual.isFavourite).isEqualTo(node.isFavourite)
        assertThat(actual.isTakenDown).isEqualTo(node.isTakenDown)
        assertThat(actual).isInstanceOf(DefaultFolderNode::class.java)
        val actualAsFolder = actual as DefaultFolderNode
        assertThat(actualAsFolder.isInRubbishBin).isTrue()
        assertThat(actualAsFolder.isPendingShare).isTrue()
    }

    @Nested
    @DisplayName("Test that is exported data is correct")
    inner class Exported {

        @ParameterizedTest(name = "exported: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that exported data is returned if and only if is exported in MegaNode is true`(
            exported: Boolean,
        ) = runTest {
            val megaNode = megaNode(exported)
            val actual = mappedNode(megaNode)
            if (exported) {
                assertThat(actual.exportedData).isNotNull()
            } else {
                assertThat(actual.exportedData).isNull()
            }
        }

        @Test
        fun `test that exported public link is correct`() = runTest {
            val megaNode = megaNode(true)
            val actual = mappedNode(megaNode)
            assertThat(actual.exportedData?.publicLink).isEqualTo(expectedPublicLink)
        }

        @Test
        fun `test that exported public link creation time is correct`() = runTest {
            val megaNode = megaNode(true)
            val actual = mappedNode(megaNode)
            assertThat(actual.exportedData?.publicLinkCreationTime).isEqualTo(
                expectedPublicLinkCreationTime
            )
        }

        private suspend fun mappedNode(megaNode: MegaNode) = underTest(
            megaNode = megaNode,
            thumbnailPath = { null },
            previewPath = { null },
            fullSizePath = { null },
            hasVersion = { false },
            numberOfChildFolders = { 0 },
            numberOfChildFiles = { 1 },
            isInRubbish = { false },
            fileTypeInfoMapper = { PdfFileTypeInfo },
            isPendingShare = { false },
        )

        private fun megaNode(exported: Boolean) = getMockNode(
            isExported = exported,
            isFile = false
        )
    }

    private fun getMockNode(
        name: String = expectedName,
        size: Long = expectedSize,
        label: Int = expectedLabel,
        id: Long = expectedId,
        parentId: Long = expectedParentId,
        base64Id: String = expectedBase64Id,
        modificationTime: Long = expectedModificationTime,
        fingerprint: String = expectedFingerprint,
        duration: Int = expectedDuration,
        isExported: Boolean = true,
        publicLink: String = expectedPublicLink,
        publicLinkCreationTime: Long = expectedPublicLinkCreationTime,
        isFile: Boolean,
    ): MegaNode {
        val node = mock<MegaNode> {
            on { this.name }.thenReturn(name)
            on { this.size }.thenReturn(size)
            on { this.label }.thenReturn(label)
            on { this.handle }.thenReturn(id)
            on { this.parentHandle }.thenReturn(parentId)
            on { this.base64Handle }.thenReturn(base64Id)
            on { this.modificationTime }.thenReturn(modificationTime)
            on { this.fingerprint }.thenReturn(fingerprint)
            on { this.duration }.thenReturn(duration)
            on { this.isFile }.thenReturn(isFile)
            on { this.isFolder }.thenReturn(!isFile)
            on { this.isExported }.thenReturn(isExported)
            on { this.publicLink }.thenReturn(publicLink)
            on { this.publicLinkCreationTime }.thenReturn(publicLinkCreationTime)
        }
        return node
    }
}
