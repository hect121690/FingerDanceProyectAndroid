package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.MegaShareMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.entity.FolderVersionInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaShare
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [MegaNodeRepository]
 *
 * @property context
 * @property megaApiGateway
 * @property megaApiFolderGateway
 * @property megaChatApiGateway
 * @property ioDispatcher
 * @property megaLocalStorageGateway
 * @property megaShareMapper
 * @property megaExceptionMapper
 * @property sortOrderIntMapper
 * @property cacheFolderGateway
 * @property nodeMapper
 * @property fileTypeInfoMapper
 * @property offlineNodeInformationMapper
 * @property fileGateway
 * @property chatFilesFolderUserAttributeMapper
 * @property streamingGateway
 * @property getLinksSortOrder
 */
internal class MegaNodeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    private val megaShareMapper: MegaShareMapper,
    private val megaExceptionMapper: MegaExceptionMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val cacheFolderGateway: CacheFolderGateway,
    private val nodeMapper: NodeMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper,
    private val fileGateway: FileGateway,
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper,
    private val streamingGateway: StreamingGateway,
    private val getLinksSortOrder: GetLinksSortOrder,
) : MegaNodeRepository {

    override suspend fun copyNode(
        nodeToCopy: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.copyNode(
                nodeToCopy = nodeToCopy,
                newNodeParent = newNodeParent,
                newNodeName = newNodeName,
                listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK && request.type == MegaRequest.TYPE_COPY) {
                            continuation.resumeWith(Result.success(NodeId(request.nodeHandle)))
                        } else {
                            continuation.failWithError(error, "copyNode")
                        }
                    }
                )
            )
        }
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun copyNodeByHandle(
        nodeToCopy: NodeId,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        val node = megaApiGateway.getMegaNodeByHandle(nodeToCopy.longValue)
        val parent = megaApiGateway.getMegaNodeByHandle(newNodeParent.longValue)
        if (node == null) {
            throw IllegalArgumentException("Node to copy with handle $nodeToCopy not found")
        }
        if (parent == null) {
            throw IllegalArgumentException("Destination node with handle $newNodeParent not found")
        }
        copyNode(node, parent, newNodeName)
    }

    override suspend fun moveNode(
        nodeToMove: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("moveNode") {
                NodeId(it.nodeHandle)
            }
            megaApiGateway.moveNode(
                nodeToMove = nodeToMove,
                newNodeParent = newNodeParent,
                newNodeName = newNodeName,
                listener = listener
            )
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun moveNodeByHandle(
        nodeToMove: NodeId,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        val node = megaApiGateway.getMegaNodeByHandle(nodeToMove.longValue)
        val parent = megaApiGateway.getMegaNodeByHandle(newNodeParent.longValue)
        if (node == null) {
            throw IllegalArgumentException("Node to copy with handle $nodeToMove not found")
        }
        if (parent == null) {
            throw IllegalArgumentException("Destination node with handle $newNodeParent not found")
        }
        moveNode(node, parent, newNodeName)
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun deleteNodeByHandle(nodeToDelete: NodeId) = withContext(ioDispatcher) {
        val node = megaApiGateway.getMegaNodeByHandle(nodeToDelete.longValue)
            ?: throw IllegalArgumentException("Node to delete with handle $nodeToDelete not found")
        if (!megaApiGateway.isInRubbish(node)) {
            throw IllegalArgumentException("Node needs to be in the rubbish bin before deleting it")
        }
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("deleteNodeByHandle") {}
            megaApiGateway.deleteNode(
                node = node,
                listener = listener
            )
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    @Throws(MegaException::class)
    override suspend fun getRootFolderVersionInfo(): FolderVersionInfo =
        withContext(ioDispatcher) {
            val rootNode = megaApiGateway.getRootNode()
            suspendCoroutine { continuation ->
                megaApiGateway.getFolderInfo(
                    rootNode,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestFolderInfoCompleted(continuation)
                    )
                )
            }
        }

    private fun onRequestFolderInfoCompleted(continuation: Continuation<FolderVersionInfo>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(
                    Result.success(
                        with(request.megaFolderInfo) {
                            FolderVersionInfo(
                                numVersions,
                                versionsSize
                            )
                        }
                    )
                )
            } else {
                continuation.failWithError(error, "onRequestFolderInfoCompleted")
            }
        }

    override suspend fun getRootNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getRootNode()
    }

    override suspend fun getInboxNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getInboxNode()
    }

    override suspend fun isNodeInInbox(megaNode: MegaNode) = withContext(ioDispatcher) {
        megaApiGateway.isInInbox(megaNode)
    }

    override suspend fun getRubbishBinNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getRubbishBinNode()
    }

    override suspend fun moveNodeToRubbishBinByHandle(nodeToMove: NodeId) {
        val node = megaApiGateway.getMegaNodeByHandle(nodeToMove.longValue)
        val rubbish = megaApiGateway.getRubbishBinNode()
        if (node == null) {
            throw IllegalArgumentException("Node to copy with handle $nodeToMove not found")
        }
        if (rubbish == null) {
            throw IllegalArgumentException("Rubbish bin node not found")
        }
        moveNode(node, rubbish, null)
    }

    override suspend fun isInRubbish(node: MegaNode): Boolean = withContext(ioDispatcher) {
        megaApiGateway.isInRubbish(node)
    }

    override suspend fun getParentNode(node: MegaNode): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getParentNode(node)
    }

    override suspend fun getChildNode(parentNode: MegaNode?, name: String?): MegaNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getChildNode(parentNode, name)
        }

    override suspend fun getChildrenNode(parentNode: MegaNode, order: SortOrder): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getChildrenByNode(parentNode, sortOrderIntMapper(order))
        }

    override suspend fun getNodeByPath(path: String?, megaNode: MegaNode?): MegaNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getNodeByPath(path, megaNode)
        }

    override suspend fun getNodeByHandle(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)
    }

    override suspend fun getFingerprint(filePath: String): String? = withContext(ioDispatcher) {
        megaApiGateway.getFingerprint(filePath)
    }

    override suspend fun getNodesByOriginalFingerprint(
        originalFingerprint: String,
        parentNode: MegaNode?,
    ): MegaNodeList? = withContext(ioDispatcher) {
        megaApiGateway.getNodesByOriginalFingerprint(originalFingerprint, parentNode)
    }

    override suspend fun getNodeByFingerprintAndParentNode(
        fingerprint: String,
        parentNode: MegaNode?,
    ): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getNodeByFingerprintAndParentNode(fingerprint, parentNode)
    }

    override suspend fun getNodeByFingerprint(fingerprint: String): MegaNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getNodeByFingerprint(fingerprint)
        }

    override suspend fun setOriginalFingerprint(node: MegaNode, originalFingerprint: String) {
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.setOriginalFingerprint(
                    node = node,
                    originalFingerprint = originalFingerprint,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (error.errorCode == MegaError.API_OK && request.type == MegaRequest.TYPE_SET_ATTR_NODE) {
                                continuation.resumeWith(Result.success(Unit))
                            } else {
                                continuation.failWithError(error, "setOriginalFingerprint")
                            }
                        }
                    )
                )
            }
        }
    }

    override suspend fun getIncomingSharesNode(order: SortOrder): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getIncomingSharesNode(sortOrderIntMapper(order))
        }

    override suspend fun getUserFromInShare(node: MegaNode, recursive: Boolean) =
        withContext(ioDispatcher) {
            megaApiGateway.getUserFromInShare(node, recursive)
        }

    override suspend fun authorizeNode(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiFolderGateway.authorizeNode(handle)
    }

    override suspend fun getPublicLinks(order: SortOrder): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getPublicLinks(sortOrderIntMapper(order))
        }

    override suspend fun isPendingShare(node: MegaNode): Boolean = withContext(ioDispatcher) {
        megaApiGateway.isPendingShare(node)
    }

    override suspend fun hasInboxChildren(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.getInboxNode()?.let { megaApiGateway.hasChildren(it) } ?: false
    }


    override suspend fun checkAccessErrorExtended(node: MegaNode, level: Int): MegaException =
        withContext(ioDispatcher) {
            megaExceptionMapper(megaApiGateway.checkAccessErrorExtended(node, level))
        }

    override suspend fun createShareKey(megaNode: MegaNode) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener =
                continuation.getRequestListener("createShareKey") { return@getRequestListener }
            megaApiGateway.openShareDialog(megaNode, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun searchInShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: SortOrder,
    ): List<MegaNode> {
        return withContext(ioDispatcher) {
            return@withContext if (query.isEmpty()) {
                megaApiGateway.getInShares(sortOrderIntMapper(order))
            } else {
                megaApiGateway.searchOnInShares(
                    query,
                    megaCancelToken,
                    sortOrderIntMapper(order)
                )
            }
        }
    }

    override suspend fun searchOutShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: SortOrder,
    ): List<MegaNode> {
        return withContext(ioDispatcher) {
            return@withContext if (query.isEmpty()) {
                val searchNodes = ArrayList<MegaNode>()
                val outShares = megaApiGateway.getOutgoingSharesNode(null)
                val addedHandles: MutableList<Long> = ArrayList()
                for (outShare in outShares) {
                    val node = megaApiGateway.getMegaNodeByHandle(outShare.nodeHandle)
                    if (node != null && !addedHandles.contains(node.handle)) {
                        addedHandles.add(node.handle)
                        searchNodes.add(node)
                    }
                }
                searchNodes
            } else {
                megaApiGateway.searchOnOutShares(
                    query = query,
                    megaCancelToken = megaCancelToken,
                    order = sortOrderIntMapper(order)
                )
            }
        }
    }

    override suspend fun getOutShares(nodeId: NodeId): List<MegaShare>? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { node ->
                megaApiGateway.getOutShares(node)
            }
        }

    override suspend fun searchLinkShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: SortOrder,
        isFirstLevelNavigation: Boolean,
    ): List<MegaNode> {
        return withContext(ioDispatcher) {
            return@withContext if (query.isEmpty()) {
                megaApiGateway.getPublicLinks(
                    if (isFirstLevelNavigation) sortOrderIntMapper(
                        getLinksSortOrder()
                    ) else sortOrderIntMapper(order)
                )
            } else {
                megaApiGateway.searchOnLinkShares(
                    query,
                    megaCancelToken,
                    sortOrderIntMapper(order)
                )
            }
        }
    }

    override suspend fun search(
        parentNode: MegaNode,
        query: String,
        order: SortOrder,
        megaCancelToken: MegaCancelToken,
    ): List<MegaNode> {
        return withContext(ioDispatcher) {
            return@withContext megaApiGateway.search(
                parentNode, query, megaCancelToken, sortOrderIntMapper(order)
            )
        }
    }
}
