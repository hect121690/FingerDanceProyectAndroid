package mega.privacy.android.domain.usecase

/**
 * Disable Camera Upload Setting
 */
fun interface DisableCameraUploadSettings {

    /**
     * Invocation function
     */
    suspend operator fun invoke()
}