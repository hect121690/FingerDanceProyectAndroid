package mega.privacy.android.domain.usecase

/**
 * Does sync record file name exist
 *
 */
fun interface FileNameExists {

    /**
     * Invoke
     *
     * @return whether file name exists
     */
    operator fun invoke(fileName: String, isSecondary: Boolean): Boolean
}
