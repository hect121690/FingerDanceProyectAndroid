package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult

/**
 * The use case interface to get Image Result given Node Public Link
 */
fun interface GetImageByNodePublicLink {

    /**
     * Get Image Result given Node Public Link
     *
     * @param nodeFileLink      Public link to a file in MEGA
     * @param fullSize          Flag to request full size image despite data/size requirements
     * @param highPriority      Flag to request image with high priority
     * @param resetDownloads    Callback to reset downloads
     *
     * @return Flow<ImageResult>
     */
    suspend operator fun invoke(
        nodeFileLink: String,
        fullSize: Boolean,
        highPriority: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult>
}