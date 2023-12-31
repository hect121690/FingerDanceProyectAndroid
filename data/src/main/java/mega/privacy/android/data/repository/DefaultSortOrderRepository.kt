package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.SortOrderMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * Default implementation of [SortOrderRepository]
 *
 * @property ioDispatcher
 * @property megaLocalStorageGateway
 * @property sortOrderMapper
 * @property sortOrderIntMapper
 */
internal class DefaultSortOrderRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    private val sortOrderMapper: SortOrderMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
) : SortOrderRepository {

    override suspend fun getCameraSortOrder(): SortOrder? = withContext(ioDispatcher) {
        sortOrderMapper(megaLocalStorageGateway.getCameraSortOrder())
    }

    override suspend fun getCloudSortOrder(): SortOrder? = withContext(ioDispatcher) {
        sortOrderMapper(megaLocalStorageGateway.getCloudSortOrder())
    }

    override suspend fun getLinksSortOrder(): SortOrder? = withContext(ioDispatcher) {
        sortOrderMapper(megaLocalStorageGateway.getLinksSortOrder())
    }

    override suspend fun getOthersSortOrder(): SortOrder? = withContext(ioDispatcher) {
        sortOrderMapper(megaLocalStorageGateway.getOthersSortOrder())
    }

    override suspend fun getOfflineSortOrder(): SortOrder? = withContext(ioDispatcher) {
        sortOrderMapper(megaLocalStorageGateway.getOfflineSortOrder())
    }

    override suspend fun setOfflineSortOrder(order: SortOrder) = withContext(ioDispatcher) {
        megaLocalStorageGateway.setOfflineSortOrder(sortOrderIntMapper(order))
    }

    override suspend fun setCameraSortOrder(order: SortOrder) = withContext(ioDispatcher) {
        megaLocalStorageGateway.setCameraSortOrder(sortOrderIntMapper(order))
    }

    override suspend fun setCloudSortOrder(order: SortOrder) = withContext(ioDispatcher) {
        megaLocalStorageGateway.setCloudSortOrder(sortOrderIntMapper(order))
    }

    override suspend fun setOthersSortOrder(order: SortOrder) = withContext(ioDispatcher) {
        megaLocalStorageGateway.setOthersSortOrder(sortOrderIntMapper(order))
    }
}
