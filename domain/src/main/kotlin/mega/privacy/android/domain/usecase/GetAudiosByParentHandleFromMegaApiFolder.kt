package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for getting audio children by parent handle from MegaApiFolder
 */
interface GetAudiosByParentHandleFromMegaApiFolder {

    /**
     * Get audio children by parent handle from MegaApiFolder
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return audio nodes
     */
    suspend operator fun invoke(parentHandle: Long, order: SortOrder, ): List<TypedNode>?
}