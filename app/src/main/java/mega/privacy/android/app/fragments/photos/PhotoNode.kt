package mega.privacy.android.app.fragments.photos

import nz.mega.sdk.MegaNode
import java.io.File

data class PhotoNode(
    val type: Int,
    var photoIndex: Int,
    override var node: MegaNode?,
    override var index: Int,
    override var modifiedDate: String,
    override var thumbnail: File?,
    override var selected: Boolean,
    override var uiDirty: Boolean = true   // Force refresh the newly created Node list item
) : SelectableNode {
    companion object {
        const val TYPE_TITLE = 0   // The datetime header
        const val TYPE_PHOTO = 1
    }
}