package mega.privacy.android.data.gateway

import android.net.Uri
import mega.privacy.android.domain.entity.CameraUploadMedia
import java.util.Queue

/**
 * Camera Upload Media Files Gateway
 */
interface CameraUploadMediaGateway {

    /**
     * Get the media queue for a given media type
     *
     * @param uri different media store file type
     * @param parentPath local path of camera upload
     * @param isVideo if camera upload media is video
     * @param selectionQuery db query
     *
     * @return queue of camera upload media
     */
    suspend fun getMediaQueue(
        uri: Uri,
        parentPath: String?,
        isVideo: Boolean,
        selectionQuery: String?,
    ): Queue<CameraUploadMedia>

    /**
     * Update camera upload folder (node list) icon
     *
     * @param nodeHandle    updated node handle
     * @param isSecondary   if updated node handle is secondary media
     */
    suspend fun sendUpdateFolderIconBroadcast(nodeHandle: Long, isSecondary: Boolean)

    /**
     * Update camera upload folder destination in settings
     *
     * @param nodeHandle    updated node handle
     * @param isSecondary   if updated node handle is secondary media
     */
    suspend fun sendUpdateFolderDestinationBroadcast(nodeHandle: Long, isSecondary: Boolean)
}
