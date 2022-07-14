package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.FavouriteFolderInfo

/**
 * The use case interface to get children nodes by node
 */
fun interface GetFavouriteFolderInfo {
    /**
     * Get children nodes by node
     * @param parentHandle parent node handle
     * @return Flow<FavouriteFolderInfo>
     */
    operator fun invoke(parentHandle: Long): Flow<FavouriteFolderInfo?>
}