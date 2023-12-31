package mega.privacy.android.app.presentation.manager.model

import mega.privacy.android.domain.entity.Feature


/**
 * Manager state
 *
 * @property isFirstNavigationLevel
 * @property sharesTab
 * @property transfersTab
 * @property isFirstLogin
 * @property hasInboxChildren
 * @property shouldStopCameraUpload
 * @property shouldSendCameraBroadcastEvent
 * @property nodeUpdateReceived
 * @property pendingActionsCount
 * @property shouldAlertUserAboutSecurityUpgrade
 * @property showSyncSection
 * @property show2FADialog
 * @property canVerifyPhoneNumber
 * @property enabledFlags
 * @property isPushNotificationSettingsUpdatedEvent
 */
data class ManagerState(
    val isFirstNavigationLevel: Boolean = true,
    val sharesTab: SharesTab = SharesTab.INCOMING_TAB,
    val transfersTab: TransfersTab = TransfersTab.NONE,
    val isFirstLogin: Boolean = false,
    val hasInboxChildren: Boolean = false,
    val shouldStopCameraUpload: Boolean = false,
    val shouldSendCameraBroadcastEvent: Boolean = false,
    val nodeUpdateReceived: Boolean = false,
    val pendingActionsCount: Int = 0,
    val shouldAlertUserAboutSecurityUpgrade: Boolean = false,
    val showSyncSection: Boolean = false,
    val show2FADialog: Boolean = false,
    val canVerifyPhoneNumber: Boolean = false,
    val enabledFlags: Set<Feature> = emptySet(),
    val isPushNotificationSettingsUpdatedEvent: Boolean = false,
)
