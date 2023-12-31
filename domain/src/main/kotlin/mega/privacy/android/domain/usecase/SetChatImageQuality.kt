package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatImageQuality

/**
 * Sets chat image quality preference.
 */
fun interface SetChatImageQuality {

    /**
     * Invoke.
     *
     * @param quality New chat image quality.
     * @return Chat image quality.
     */
    suspend operator fun invoke(quality: ChatImageQuality)
}