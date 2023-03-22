package mega.privacy.android.app.globalmanagement

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.PushNotificationSettingManagement
import mega.privacy.android.app.listeners.GetAttrUserListener
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.CUBackupInitializeChecker
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED
import mega.privacy.android.app.utils.JobUtil.scheduleCameraUploadJob
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetFullAccountInfo
import mega.privacy.android.domain.usecase.login.BroadcastFetchNodesFinishUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

/**
 * Background request listener
 *
 * @property application [Application]
 * @property myAccountInfo [MyAccountInfo]
 * @property megaChatApi [MegaChatApiAndroid]
 * @property dbH [DatabaseHandler]
 * @property megaApi [MegaApiAndroid]
 * @property transfersManagement [TransfersManagement]
 * @property pushNotificationSettingManagement [PushNotificationSettingManagement]
 * @property applicationScope [CoroutineScope]
 * @property getFullAccountInfo [GetFullAccountInfo]
 * @property broadcastFetchNodesFinishUseCase [BroadcastFetchNodesFinishUseCase]
 */
class BackgroundRequestListener @Inject constructor(
    private val application: Application,
    private val myAccountInfo: MyAccountInfo,
    private val megaChatApi: MegaChatApiAndroid,
    private val dbH: DatabaseHandler,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val transfersManagement: TransfersManagement,
    private val pushNotificationSettingManagement: PushNotificationSettingManagement,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getFullAccountInfo: GetFullAccountInfo,
    private val broadcastFetchNodesFinishUseCase: BroadcastFetchNodesFinishUseCase,
) : MegaRequestListenerInterface {
    /**
     * On request start
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("BackgroundRequestListener:onRequestStart: %s", request.requestString)
    }

    /**
     * On request update
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("BackgroundRequestListener:onRequestUpdate: %s", request.requestString)
    }

    /**
     * On request finish
     */
    override fun onRequestFinish(
        api: MegaApiJava,
        request: MegaRequest,
        e: MegaError,
    ) {
        Timber.d(
            "BackgroundRequestListener:onRequestFinish: %s____%d___%d",
            request.requestString,
            e.errorCode,
            request.paramType
        )
        if (e.errorCode == MegaError.API_EPAYWALL) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        if (e.errorCode == MegaError.API_EBUSINESSPASTDUE) {
            application.sendBroadcast(Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED))
            return
        }
        when (request.type) {
            MegaRequest.TYPE_LOGOUT -> handleLogoutRequest(e, request, api)
            MegaRequest.TYPE_FETCH_NODES -> handleFetchNodeRequest(e)
            MegaRequest.TYPE_GET_ATTR_USER -> handleGetAttrUserRequest(request, e)
            MegaRequest.TYPE_SET_ATTR_USER -> handleSetAttrUserRequest(request, e)
            MegaRequest.TYPE_PAUSE_TRANSFERS -> dbH.transferQueueStatus = request.flag
        }
    }

    private fun handleSetAttrUserRequest(request: MegaRequest, e: MegaError) {
        if (request.paramType == MegaApiJava.USER_ATTR_PUSH_SETTINGS) {
            if (e.errorCode == MegaError.API_OK) {
                pushNotificationSettingManagement.sendPushNotificationSettings(request.megaPushNotificationSettings)
            } else {
                Timber.e("Chat notification settings cannot be updated")
            }
        }
    }

    private fun handleGetAttrUserRequest(request: MegaRequest, e: MegaError) {
        if (request.paramType == MegaApiJava.USER_ATTR_PUSH_SETTINGS) {
            if (e.errorCode == MegaError.API_OK || e.errorCode == MegaError.API_ENOENT) {
                pushNotificationSettingManagement.sendPushNotificationSettings(request.megaPushNotificationSettings)
            }
        } else if (e.errorCode == MegaError.API_OK) {
            if (request.paramType == MegaApiJava.USER_ATTR_FIRSTNAME || request.paramType == MegaApiJava.USER_ATTR_LASTNAME) {
                request.email?.let { email ->
                    megaApi.getContact(email)?.let { user ->
                        Timber.d("User handle: %s", user.handle)
                        Timber.d("Visibility: %s",
                            user.visibility) //If user visibility == MegaUser.VISIBILITY_UNKNOWN then, non contact
                        if (user.visibility != MegaUser.VISIBILITY_VISIBLE) {
                            Timber.d("Non-contact")
                            when (request.paramType) {
                                MegaApiJava.USER_ATTR_FIRSTNAME -> {
                                    dbH.setNonContactEmail(request.email,
                                        user.handle.toString() + "")
                                    dbH.setNonContactFirstName(request.text,
                                        user.handle.toString() + "")
                                }
                                MegaApiJava.USER_ATTR_LASTNAME -> {
                                    dbH.setNonContactLastName(request.text,
                                        user.handle.toString() + "")
                                }
                                else -> {}
                            }
                        } else {
                            Timber.d("The user is or was CONTACT:")
                        }
                    } ?: run {
                        Timber.w("User is NULL")
                    }
                }
            }
        }
    }

    private fun handleFetchNodeRequest(e: MegaError) {
        Timber.d("TYPE_FETCH_NODES")
        applicationScope.launch { broadcastFetchNodesFinishUseCase() }

        if (e.errorCode == MegaError.API_OK) {
            askForFullAccountInfo()
            var listener: GetAttrUserListener? = GetAttrUserListener(application)
            megaApi.shouldShowRichLinkWarning(listener)
            megaApi.isRichPreviewsEnabled(listener)
            listener = GetAttrUserListener(application, true)
            if (dbH.myChatFilesFolderHandle == INVALID_HANDLE) {
                megaApi.getMyChatFilesFolder(listener)
            }

            // Init CU sync data after login successfully
            CUBackupInitializeChecker(megaApi).initCuSync()

            //Login check resumed pending transfers
            transfersManagement.checkResumedPendingTransfers()
            scheduleCameraUploadJob(application)
        }
    }

    private fun handleLogoutRequest(
        e: MegaError,
        request: MegaRequest,
        api: MegaApiJava,
    ) {
        Timber.d("LogoutUseCase finished: %s(%d)", e.errorString, e.errorCode)
        if (e.errorCode == MegaError.API_OK) {
            Timber.d("END logout sdk request - wait chat logout")
            MegaApplication.isLoggingOut = false
        } else if (e.errorCode == MegaError.API_EINCOMPLETE) {
            if (request.paramType == MegaError.API_ESSL) {
                Timber.w("SSL verification failed")
                application.sendBroadcast(Intent(
                    BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED))
            }
        } else if (e.errorCode == MegaError.API_ESID) {
            Timber.w("TYPE_LOGOUT:API_ESID")
            myAccountInfo.resetDefaults()
            (application as MegaApplication).isEsid = true
            AccountController.localLogoutApp(application, applicationScope)
        } else if (e.errorCode == MegaError.API_EBLOCKED) {
            api.localLogout()
            megaChatApi.logout()
        }
    }

    /**
     * On request temporary error
     */
    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest, e: MegaError,
    ) {
        Timber.d("BackgroundRequestListener: onRequestTemporaryError: %s",
            request.requestString)
    }

    private fun askForFullAccountInfo() {
        Timber.d("askForFullAccountInfo")
        applicationScope.launch {
            getFullAccountInfo()
        }
    }
}