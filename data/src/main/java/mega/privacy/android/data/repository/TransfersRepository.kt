package mega.privacy.android.data.repository

import nz.mega.sdk.MegaTransfer

/**
 * Transfers repository.
 */
interface TransfersRepository {

    /**
     * Gets all transfers of a MegaTransfer::TYPE_UPLOAD type.
     *
     * @return List with upload transfers.
     */
    suspend fun getUploadTransfers(): List<MegaTransfer>

    /**
     * Gets all transfers of a MegaTransfer::TYPE_DOWNLOAD type.
     *
     * @return List with download transfers.
     */
    suspend fun getDownloadTransfers(): List<MegaTransfer>

    /**
     * Gets the number of pending download transfers that are not background transfers.
     *
     * @return Number of pending downloads.
     */
    suspend fun getNumPendingDownloadsNonBackground(): Int

    /**
     * Gets the number of pending upload transfers.
     *
     * @return Number of pending uploads.
     */
    suspend fun getNumPendingUploads(): Int

    /**
     * Gets number of pending transfers.
     *
     * @return Number of pending transfers.
     */
    suspend fun getNumPendingTransfers(): Int

    /**
     * Checks if the completed transfers list is empty.
     *
     * @return True if the completed transfers is empty, false otherwise.
     */
    suspend fun isCompletedTransfersEmpty(): Boolean

    /**
     * Are transfers paused (downloads and uploads)
     *
     * @return true if downloads and uploads are paused
     */
    suspend fun areTransfersPaused(): Boolean

    /**
     * Gets the number of pending and paused uploads.
     *
     * @return Number of pending and paused uploads.
     */
    suspend fun getNumPendingPausedUploads(): Int

    /**
     * Gets the number of pending, non-background and paused downloads.
     *
     * @return Number of pending, non-background and paused downloads.
     */
    suspend fun getNumPendingNonBackgroundPausedDownloads(): Int

    /**
     * Checks if the queue of transfers is paused or if all in progress transfers are individually paused.
     *
     * @return True if the queue of transfers is paused or if all in progress transfers are
     * individually paused, false otherwise.
     */
    suspend fun areAllTransfersPaused(): Boolean

    /**
     * Checks if the queue of upload transfers is paused or if all in progress upload transfers are individually paused.
     *
     * @return True if the queue of upload transfers is paused or if all in progress upload transfers
     * are individually paused, and False if otherwise.
     */
    suspend fun areAllUploadTransfersPaused(): Boolean
}