package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import javax.inject.Inject

internal class DefaultGetRemoteFolders @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
) : GetRemoteFolders {

    override suspend fun invoke(): List<RemoteFolder> =
        megaNodeRepository
            .getRootNode()
            ?.let {
                megaNodeRepository
                    .getChildrenNode(it, SortOrder.ORDER_NONE)
                    .filter { it.isFolder }
            }
            ?.map { RemoteFolder(it.handle, it.name) }
            ?: emptyList()
}
