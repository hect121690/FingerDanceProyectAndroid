package mega.privacy.android.app.presentation.settings.camerauploads.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

/**
 * Data class representing the state of Camera Uploads in Settings
 *
 * @property accessMediaLocationRationaleText Displays the Access Media Location rationale with the message
 * @property areLocationTagsIncluded When uploading Photos, this checks whether Location Tags should be embedded in each Photo or not
 * @property isCameraUploadsRunning Checks whether Camera Uploads is running or not
 * @property isChargingRequiredForVideoCompression Checks whether compressing videos require the device to be charged or not
 * @property shouldShowBusinessAccountPrompt Checks whether the Dialog indicating that the account is a Business Account should be shown or not
 * @property shouldShowBusinessAccountSuspendedPrompt Checks whether the Dialog indicating that the account is a suspended Business account should be shown or not
 * @property shouldTriggerCameraUploads Checks whether the Camera Uploads functionality in Settings should be triggered or not
 * @property shouldShowMediaPermissionsRationale Checks whether the Media Permissions Rationale should be shown or not
 * @property shouldShowNotificationPermissionRationale Checks whether the Notification Permission Rationale should be shown or not
 * @property uploadConnectionType Determines the connection type for uploading content in Camera Uploads
 * @property uploadOption Determines what content should be uploaded
 * @property videoCompressionSizeLimit The maximum video file size that can be compressed
 * @property videoQuality Determines the Video Quality of videos to be uploaded
 */
data class SettingsCameraUploadsState(
    @StringRes val accessMediaLocationRationaleText: Int? = null,
    val areLocationTagsIncluded: Boolean = false,
    val isCameraUploadsRunning: Boolean = false,
    val isChargingRequiredForVideoCompression: Boolean = false,
    val shouldShowBusinessAccountPrompt: Boolean = false,
    val shouldShowBusinessAccountSuspendedPrompt: Boolean = false,
    val shouldTriggerCameraUploads: Boolean = false,
    val shouldShowMediaPermissionsRationale: Boolean = false,
    val shouldShowNotificationPermissionRationale: Boolean = false,
    val uploadConnectionType: UploadConnectionType? = null,
    val uploadOption: UploadOption? = null,
    val videoCompressionSizeLimit: Int = 0,
    val videoQuality: VideoQuality? = null,
)