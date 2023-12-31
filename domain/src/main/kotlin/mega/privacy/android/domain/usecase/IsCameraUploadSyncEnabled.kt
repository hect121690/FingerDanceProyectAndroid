package mega.privacy.android.domain.usecase

/**
 * Use case to check if camera upload sync is enabled
 */
fun interface IsCameraUploadSyncEnabled {

    /**
     * Invoke
     *
     * @return sync is enabled
     */
    suspend operator fun invoke(): Boolean
}
