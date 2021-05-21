package mega.privacy.android.app.meeting.adapter

import android.content.res.Configuration
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.Util.dp2px
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import org.jetbrains.anko.displayMetrics
import javax.inject.Inject

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class VideoMeetingViewHolder(
    private val binding: ItemParticipantVideoBinding,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val orientation: Int,
    private val isTypeGridViewHolder: Boolean,
    private val listenerRenderer: MegaSurfaceRenderer.MegaSurfaceRendererListener?
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    lateinit var inMeetingViewModel: InMeetingViewModel

    private var isGrid: Boolean = true

    private var avatarSize = 88
    private var peerId: Long? = MEGACHAT_INVALID_HANDLE
    private var clientId: Long? = MEGACHAT_INVALID_HANDLE

    var isDrawing = true

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        participant: Participant,
        itemCount: Int,
        isFirstPage: Boolean
    ) {
        this.isGrid = isTypeGridViewHolder
        this.inMeetingViewModel = inMeetingViewModel

        if (participant.peerId == MEGACHAT_INVALID_HANDLE || participant.clientId == MEGACHAT_INVALID_HANDLE) {
            logError("Error. Peer id or client id invalid")
            return
        }

        this.peerId = participant.peerId
        this.clientId = participant.clientId

        when {
            isGrid -> {
                avatarSize = 88
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    portraitLayout(isFirstPage, itemCount)
                } else {
                    landscapeLayout(isFirstPage, itemCount)
                }

                binding.name.text = participant.name
            }
            else -> {
                avatarSize = 60
                val layoutParams = binding.root.layoutParams
                layoutParams.width = dp2px(ITEM_WIDTH)
                layoutParams.height = dp2px(ITEM_HEIGHT)
                binding.root.setOnClickListener {
                    inMeetingViewModel.onItemClick(participant)
                }

                binding.name.isVisible = false
            }
        }

        initAvatar(participant)
        checkUI(participant)
    }

    /**
     * Initialising the avatar
     *
     * @param participant
     */
    private fun initAvatar(participant: Participant) {
        val paramsAvatar = binding.avatar.layoutParams
        paramsAvatar.width =
            dp2px(avatarSize.toFloat(), MegaApplication.getInstance().displayMetrics)
        paramsAvatar.height =
            dp2px(avatarSize.toFloat(), MegaApplication.getInstance().displayMetrics)
        binding.avatar.layoutParams = paramsAvatar

        val paramsOnHoldIcon = binding.onHoldIcon.layoutParams
        paramsOnHoldIcon.width =
            dp2px(avatarSize.toFloat(), MegaApplication.getInstance().displayMetrics)
        paramsOnHoldIcon.height =
            dp2px(avatarSize.toFloat(), MegaApplication.getInstance().displayMetrics)
        binding.onHoldIcon.layoutParams = paramsOnHoldIcon

        binding.avatar.setImageBitmap(participant.avatar)
        if (isGrid || isDrawing) {
            logDebug("Remove the video initially")
            inMeetingViewModel.onCloseVideo(participant)
            removeTextureView(participant)
        }
    }

    /**
     * Initialising the UI
     */
    private fun checkUI(participant: Participant) {
        logDebug("Check the current UI status")
        inMeetingViewModel.getSession(participant.clientId)?.let {
            when {
                it.hasVideo() -> {
                    logDebug("Check if video should be on")
                    checkVideoOn(participant)
                }
                else -> {
                    logDebug("Video should be off")
                    videoOffUI(participant)
                }
            }

            updateAudioIcon(participant)
            updatePrivilegeIcon(participant)
            updatePeerSelected(participant)
        }
    }

    /**
     * Show UI when video is on
     *
     * @param participant
     */
    private fun videoOnUI(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        logDebug("UI video on")
        hideAvatar(participant)
        activateVideo(participant)
    }

    /**
     * Method to hide the Avatar
     *
     * @param participant
     */
    private fun hideAvatar(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        logDebug("Hide Avatar")
        binding.onHoldIcon.isVisible = false
        binding.avatar.alpha = 1f
        binding.avatar.isVisible = false
    }

    /**
     * Method for activating the video
     *
     * @param participant
     */
    private fun activateVideo(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        /*Video*/
        if (participant.videoListener == null) {
            logDebug("Active video when listener is null")
            binding.parentTextureView.removeAllViews()
            val myTexture = TextureView(MegaApplication.getInstance().applicationContext)
            myTexture.layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            myTexture.alpha = 1.0f
            myTexture.rotation = 0f

            val vListener = GroupVideoListener(
                myTexture,
                participant.peerId,
                participant.clientId,
                false
            )

            participant.videoListener = vListener

            binding.parentTextureView.addView(participant.videoListener!!.textureView)

            participant.videoListener!!.localRenderer?.let {
                it.addListener(listenerRenderer)
            }

            inMeetingViewModel.onActivateVideo(participant, false)
        } else {
            logDebug("Active video when listener is not null")
            if (binding.parentTextureView.childCount > 0) {
                binding.parentTextureView.removeAllViews()
            }

            participant.videoListener?.textureView?.let { textureView ->
                textureView.parent?.let { textureViewParent ->
                    (textureViewParent as ViewGroup).removeView(textureView)
                }
            }

            binding.parentTextureView.addView(participant.videoListener?.textureView)

            participant.videoListener?.height = 0
            participant.videoListener?.width = 0
        }

        binding.parentTextureView.isVisible = true
    }

    /**
     * Show UI when video is off
     *
     * @param participant
     */
    private fun videoOffUI(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        logDebug("UI video off")
        showAvatar(participant)
        closeVideo(participant)
        checkOnHold(participant)
    }

    /**
     * Show avatar
     *
     * @param participant
     */
    private fun showAvatar(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        logDebug("Show avatar")
        binding.avatar.isVisible = true
    }

    /**
     * Method to close Video
     *
     * @param participant
     */
    private fun closeVideo(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        logDebug("Close video")
        binding.parentTextureView.isVisible = false

        inMeetingViewModel.onCloseVideo(participant)

        participant.videoListener?.let { listener ->
            listener.localRenderer?.let {
                it.addListener(null)
            }

            logDebug("Removing texture view of $clientId")
            if (binding.parentTextureView.childCount > 0) {
                binding.parentTextureView.removeAllViews()
            }

            listener.textureView?.let { view ->
                view.parent?.let { surfaceParent ->
                    (surfaceParent as ViewGroup).removeView(view)
                }
            }

            participant.videoListener = null
        }
    }

    /**
     * Method to control the Call/Session on hold icon visibility
     *
     * @param participant
     */
    private fun checkOnHold(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        val isCallOnHold = inMeetingViewModel.isCallOnHold()
        val isSessionOnHold = inMeetingViewModel.isSessionOnHold(participant.clientId)
        when {
            isSessionOnHold -> {
                logDebug("Show on hold icon participant")
                binding.onHoldIcon.isVisible = true
                binding.avatar.alpha = 0.5f
            }
            else -> {
                logDebug("Hide on hold icon")
                binding.onHoldIcon.isVisible = false
                when {
                    isCallOnHold -> {
                        binding.avatar.alpha = 0.5f
                    }
                    else -> {
                        binding.avatar.alpha = 1f
                    }
                }
            }
        }
    }

    /**
     * Check if mute icon should be visible
     *
     * @param participant
     */
    fun updateAudioIcon(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        logDebug("Update audio icon")
        binding.muteIcon.isVisible = !participant.isAudioOn
    }

    /**
     * Control when a change is received in the video flag
     *
     * @param participant
     */
    fun checkVideoOn(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        if (participant.isVideoOn && !inMeetingViewModel.isCallOrSessionOnHold(participant.clientId)) {
            logDebug("Video should be on")
            videoOnUI(participant)
            return
        }

        logDebug("Video should be off")
        videoOffUI(participant)
    }

    /**
     * Update privileges
     *
     * @param participant
     */
    fun updatePrivilegeIcon(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        logDebug("Update privilege icon participant")
        binding.moderatorIcon.isVisible = participant.isModerator
    }

    /**
     * Update name and avatar
     *
     * @param participant
     */
    fun updateName(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        logDebug("Update name")
        binding.avatar.setImageBitmap(participant.avatar)
    }

    /**
     * Check changes in call on hold
     *
     * @param participant
     * @param isCallOnHold
     */
    fun updateCallOnHold(participant: Participant, isCallOnHold: Boolean) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        when {
            isCallOnHold -> {
                logDebug("Call is on hold")
                videoOffUI(participant)
            }
            else -> {
                logDebug("Call is not on hold")
                checkVideoOn(participant)
            }
        }
    }

    /**
     * Check changes in session on hold
     *
     * @param participant
     * @param isSessionOnHold
     */
    fun updateSessionOnHold(participant: Participant, isSessionOnHold: Boolean) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        when {
            isSessionOnHold -> {
                logDebug("Session is on hold")
                videoOffUI(participant)
            }
            else -> {
                logDebug("Session is not on hold")
                checkVideoOn(participant)
            }
        }
    }

    /**
     * Update the peer selected
     *
     * @param participant
     */
    fun updatePeerSelected(participant: Participant) {
        if (isGrid || participant.peerId != this.peerId || participant.clientId != this.clientId) return
        when {
            participant.isSpeaker -> {
                logDebug("Participant is speaker")
                binding.selectedForeground.background = ContextCompat.getDrawable(
                    binding.root.context,
                    if (inMeetingViewModel.isSpeakerSelectionAutomatic) R.drawable.border_green_layer
                    else R.drawable.border_green_layer_selected
                )
                binding.selectedForeground.isVisible = true
            }
            else -> {
                logDebug("Participant is not selected")
                binding.selectedForeground.isVisible = false
            }
        }
    }

    /**
     * Remove Texture view when fragment is destroyed
     *
     * @param participant
     */
    fun removeTextureView(participant: Participant) {
        if (participant.peerId != this.peerId || participant.clientId != this.clientId) return

        logDebug("Removing texture view of $clientId")
        if (binding.parentTextureView.childCount > 0) {
            binding.parentTextureView.removeAllViews()
            binding.parentTextureView.removeAllViewsInLayout()
        }

        participant.videoListener?.let { listener ->
            listener.localRenderer?.let {
                it.addListener(null)
            }
            listener.textureView?.let { view ->
                view.parent?.let { surfaceParent ->
                    (surfaceParent as ViewGroup).removeView(view)
                }
            }

            participant.videoListener = null
        }
    }

    private fun landscapeLayout(isFirstPage: Boolean, itemCount: Int) {
        if (!isGrid)
            return

        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.root.layoutParams as GridLayoutManager.LayoutParams

        if (isFirstPage) {
            when (itemCount) {
                1 -> {
                    w = screenWidth / 2
                    h = screenHeight
                    layoutParams.setMargins(w / 2, 0, w / 2, 0)
                }
                2 -> {
                    w = screenWidth / 2
                    h = screenHeight
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                3 -> {
                    w = (screenWidth / 3)
                    h = (screenHeight * 0.6).toInt()
                    layoutParams.setMargins(0, 0, 0, screenHeight - h)
                }
                5 -> {
                    w = screenWidth / 4
                    h = screenHeight / 2

                    when (adapterPosition) {
                        3, 4 -> layoutParams.setMargins(w / 2, 0, 0, 0)
                    }
                }
                4, 6 -> {
                    w = (screenWidth / 4)
                    h = (screenHeight / 2)
                }
            }
        } else {
            w = (screenWidth / 4)
            h = (screenHeight / 2)
        }

        layoutParams.width = w
        layoutParams.height = h
    }

    private fun portraitLayout(isFirstPage: Boolean, itemCount: Int) {
        if (!isGrid)
            return

        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.root.layoutParams as GridLayoutManager.LayoutParams

        val verticalMargin = ((screenHeight - screenWidth / 2 * 3) / 2)

        if (isFirstPage) {
            when (itemCount) {
                1 -> {
                    w = screenWidth
                    h = screenHeight
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                2 -> {
                    w = screenWidth
                    h = screenHeight / 2
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                3 -> {
                    w = (screenWidth * 0.8).toInt()
                    h = screenHeight / 3
                    layoutParams.setMargins((screenWidth - w) / 2, 0, (screenWidth - w) / 2, 0)
                }
                5 -> {
                    w = screenWidth / 2
                    h = w

                    when (adapterPosition) {
                        0, 1 -> {
                            layoutParams.setMargins(0, verticalMargin, 0, 0)
                        }
                        4 -> {
                            layoutParams.setMargins(
                                (screenWidth - w) / 2,
                                0,
                                (screenWidth - w) / 2,
                                0
                            )
                        }
                        else -> {
                            layoutParams.setMargins(0, 0, 0, 0)
                        }
                    }
                }
                4, 6 -> {
                    val pair = layout46(layoutParams, verticalMargin)
                    h = pair.first
                    w = pair.second
                }
            }
        } else {
            val pair = layout46(layoutParams, verticalMargin)
            h = pair.first
            w = pair.second
        }

        layoutParams.width = w
        layoutParams.height = h
    }

    private fun layout46(
        layoutParams: GridLayoutManager.LayoutParams,
        verticalMargin: Int
    ): Pair<Int, Int> {
        val w = screenWidth / 2
        when (adapterPosition) {
            0, 1 -> {
                layoutParams.setMargins(0, verticalMargin, 0, 0)
            }
            else -> {
                layoutParams.setMargins(0, 0, 0, 0)
            }
        }
        return Pair(w, w)
    }

    fun onRecycle() {
        if (isGrid)
            return

        isDrawing = false
        inMeetingViewModel.getParticipant(peerId!!, clientId!!)?.let {
            if (it.isVideoOn) {
                logDebug("Remove the video when participant is not visible")
                inMeetingViewModel.onCloseVideo(it)
                removeTextureView(it)
            }
        }
    }

    companion object {
        const val ITEM_WIDTH = 90f
        const val ITEM_HEIGHT = 90f
    }
}
