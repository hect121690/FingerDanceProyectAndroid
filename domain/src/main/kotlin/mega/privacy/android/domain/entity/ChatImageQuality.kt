package mega.privacy.android.domain.entity

/**
 * Enum class defining chat image quality available settings.
 */
enum class ChatImageQuality {

    Automatic, Original, Optimised;

    companion object {
        val DEFAULT = Automatic
    }

}
