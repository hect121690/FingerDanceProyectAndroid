package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeShareKeyResultMapper
import mega.privacy.android.data.mapper.shares.AccessPermissionMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.exception.SynchronisationException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [NodeRepository]
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
 */
internal class NodeRepositoryImpl @Inject constructor(
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
    private val nodeUpdateMapper: NodeUpdateMapper,
    private val accessPermissionMapper: AccessPermissionMapper,
    private val nodeShareKeyResultMapper: NodeShareKeyResultMapper,
) : NodeRepository {


    override suspend fun getOutgoingSharesNode(order: SortOrder): List<ShareData> =
        withContext(ioDispatcher) {
            megaApiGateway.getOutgoingSharesNode(sortOrderIntMapper(order))
                .map { megaShareMapper(it) }
        }

    override suspend fun getUnverifiedIncomingShares(order: SortOrder): List<ShareData> =
        withContext(ioDispatcher) {
            megaApiGateway.getUnverifiedIncomingShares(sortOrderIntMapper(order)).map {
                megaShareMapper(it)
            }
        }

    override suspend fun getUnverifiedOutgoingShares(order: SortOrder): List<ShareData> =
        withContext(ioDispatcher) {
            megaApiGateway.getOutgoingSharesNode(sortOrderIntMapper(order)).map {
                megaShareMapper(it)
            }
        }

    override suspend fun isNodeInRubbish(handle: Long) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let { megaApiGateway.isInRubbish(it) } ?: false
    }

    override suspend fun isNodeInInbox(handle: Long) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let { megaApiGateway.isInInbox(it) } ?: false
    }

    override suspend fun getBackupFolderId(): NodeId =
        withContext(ioDispatcher) {
            val backupsFolderAttributeIdentifier = MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER
            suspendCancellableCoroutine { continuation ->
                megaApiGateway.getUserAttribute(backupsFolderAttributeIdentifier,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (request.paramType == backupsFolderAttributeIdentifier) {
                                if (error.errorCode == MegaError.API_OK) {
                                    continuation.resumeWith(Result.success(NodeId(request.nodeHandle)))
                                } else {
                                    continuation.failWithError(error, "getBackupFolderId")
                                }
                            }
                        }
                    ))
            }
        }

    override suspend fun getNodeById(nodeId: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            convertToUnTypedNode(it)
        }
    }

    override suspend fun getNodeChildren(folderNode: FolderNode): List<UnTypedNode> {
        return withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(folderNode.id.longValue)?.let { parent ->
                megaApiGateway.getChildrenByNode(parent)
                    .map {
                        convertToUnTypedNode(it)
                    }
            } ?: throw SynchronisationException("Non null node found be null when fetched from api")
        }
    }

    override suspend fun getNodeChildren(
        nodeId: NodeId,
        order: SortOrder?,
    ): List<UnTypedNode> {
        return withContext(ioDispatcher) {
            return@withContext megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { parent ->
                val childList = order?.let { sortOrder ->
                    megaApiGateway.getChildrenByNode(
                        parent,
                        sortOrderIntMapper(sortOrder)
                    )
                } ?: run {
                    megaApiGateway.getChildrenByNode(parent)
                }
                childList.map { convertToUnTypedNode(it) }
            } ?: run {
                emptyList()
            }
        }
    }

    override suspend fun getNumVersions(handle: Long): Int = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let {
            megaApiGateway.getNumVersions(it)
        } ?: 0
    }

    override suspend fun getNodeHistoryVersions(handle: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle.longValue)?.let { megaNode ->
            megaApiGateway.getVersions(megaNode).map { version ->
                convertToUnTypedNode(version)
            }
        } ?: throw SynchronisationException("Non null node found be null when fetched from api")
    }

    override suspend fun getFolderTreeInfo(folderNode: FolderNode): FolderTreeInfo =
        withContext(ioDispatcher) {
            val megaNode = megaApiGateway.getMegaNodeByHandle(folderNode.id.longValue)
            suspendCoroutine { continuation ->
                megaApiGateway.getFolderInfo(
                    megaNode,
                    continuation.getRequestListener("getFolderTreeInfo") {
                        with(it.megaFolderInfo) {
                            FolderTreeInfo(
                                numberOfFiles = numFiles,
                                numberOfFolders = numFolders,
                                totalCurrentSizeInBytes = currentSize,
                                numberOfVersions = numVersions,
                                sizeOfPreviousVersionsInBytes = versionsSize,
                            )
                        }
                    }
                )
            }
        }

    override suspend fun deleteNodeVersionByHandle(nodeVersionToDelete: NodeId): Unit =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeVersionToDelete.longValue)?.let { version ->
                suspendCancellableCoroutine { continuation ->
                    megaApiGateway.deleteVersion(
                        version,
                        continuation.getRequestListener("deleteNodeVersionByHandle") {})
                }
            } ?: throw SynchronisationException("Non null node found be null when fetched from api")
        }

    override fun monitorNodeUpdates(): Flow<NodeUpdate> {
        return megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnNodesUpdate>()
            .mapNotNull {
                it.nodeList?.map { megaNode ->
                    convertToUnTypedNode(megaNode) to nodeUpdateMapper(megaNode)
                }
            }
            .map { nodes ->
                NodeUpdate(nodes.toMap())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun isNodeInRubbishOrDeleted(nodeHandle: Long): Boolean =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeHandle)?.let { megaApiGateway.isInRubbish(it) }
                ?: true
        }

    override suspend fun getOfflineNodeInformation(nodeId: NodeId) =
        withContext(ioDispatcher) {
            megaLocalStorageGateway.getOfflineInformation(nodeId.longValue)
                ?.let { offlineNodeInformationMapper(it) }
        }

    override suspend fun convertBase64ToHandle(base64: String): Long = withContext(ioDispatcher) {
        megaApiGateway.base64ToHandle(base64)
    }

    override suspend fun getOfflineNodeInformation(nodeHandle: Long) =
        withContext(ioDispatcher) {
            megaLocalStorageGateway.getOfflineInformation(nodeHandle)
                ?.let { offlineNodeInformationMapper(it) }
        }

    override suspend fun getOwnerIdFromInShare(nodeId: NodeId, recursive: Boolean): UserId? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { node ->
                megaApiGateway.getUserFromInShare(node, recursive)?.let { user ->
                    UserId(user.handle)
                }
            }
        }

    override suspend fun getNodeAccessPermission(nodeId: NodeId): AccessPermission? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { node ->
                accessPermissionMapper(megaApiGateway.getAccess(node))
            }
        }

    private suspend fun convertToUnTypedNode(node: MegaNode): UnTypedNode =
        nodeMapper(
            node,
            cacheFolderGateway::getThumbnailCacheFolder,
            cacheFolderGateway::getPreviewCacheFolder,
            cacheFolderGateway::getFullSizeCacheFolder,
            megaApiGateway::hasVersion,
            megaApiGateway::getNumChildFolders,
            megaApiGateway::getNumChildFiles,
            fileTypeInfoMapper,
            megaApiGateway::isPendingShare,
            megaApiGateway::isInRubbish,
        )

    override suspend fun stopSharingNode(nodeId: NodeId): Unit = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            megaApiGateway.stopSharingNode(it)
        }
    }

    override suspend fun createShareKey(node: TypedNode): (suspend (AccessPermission, String) -> Unit)? {
        return withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(node.id.longValue)?.let { megaNode ->
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener("openShareDialog") {
                        return@getRequestListener nodeShareKeyResultMapper(megaNode)
                    }

                    megaApiGateway.openShareDialog(megaNode, listener)
                    continuation.invokeOnCancellation {
                        megaApiGateway.removeRequestListener(listener)
                    }
                }
            }
        }
    }

    override suspend fun setShareAccess(
        nodeId: NodeId,
        accessPermission: AccessPermission,
        email: String,
    ) {
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
                nodeShareKeyResultMapper(it)(accessPermission, email)
            }
        }
    }

    override suspend fun loadOfflineNodes(
        path: String,
        searchQuery: String?,
    ): List<OfflineNodeInformation> = withContext(ioDispatcher) {
        megaLocalStorageGateway.loadOfflineNodes(path, searchQuery).map {
            offlineNodeInformationMapper(it)
        }
    }

    override suspend fun getInvalidHandle(): Long = megaApiGateway.getInvalidHandle()

    override suspend fun getRootNode() = withContext(ioDispatcher) {
        megaApiGateway.getRootNode()?.let {
            convertToUnTypedNode(it)
        }
    }

    override suspend fun removedInSharedNodesByEmail(email: String): Unit =
        withContext(ioDispatcher) {
            runCatching {
                megaApiGateway.getContact(email)?.let {
                    megaApiGateway.getInShares(it).forEach { node ->
                        megaApiGateway.deleteNode(node, null)
                    }
                }
            }
        }

    override suspend fun getInShares(email: String): List<UnTypedNode> = withContext(ioDispatcher) {
        runCatching {
            megaApiGateway.getContact(email)?.let {
                megaApiGateway.getInShares(it).map { node ->
                    convertToUnTypedNode(node)
                }
            }
        }.getOrNull() ?: emptyList()
    }


    override suspend fun getFileTypeInfo(nodeId: NodeId): FileTypeInfo? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue)?.let { megaNode ->
                return@withContext fileTypeInfoMapper(megaNode)
            }
        }
}
