package mega.privacy.android.app.getLink

import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.EventConstants.EVENT_COPY_LINK_TO_CLIPBOARD
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.getLink.useCase.EncryptLinkWithPasswordUseCase
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.util.*

class GetLinkViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbH: DatabaseHandler,
    private val encryptLinkWithPasswordUseCase: EncryptLinkWithPasswordUseCase,
    private val exportNodeUseCase: ExportNodeUseCase
) : BaseRxViewModel() {

    private val linkText: MutableLiveData<String> = MutableLiveData()
    private val linkWithPassword: MutableLiveData<String> = MutableLiveData()
    private val withElevation: MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var linkFragmentTitle: String
    private lateinit var node: MegaNode
    private lateinit var linkWithoutKey: String
    private lateinit var key: String
    private var passwordLink: String? = null
    private var isSendDecryptedKeySeparatelyEnabled = false

    fun getLink(): LiveData<String> = linkText
    fun getLinkText(): String = linkText.value ?: ""

    fun getLinkWithPassword(): LiveData<String> = linkWithPassword
    fun getLinkWithPasswordText(): String = linkWithPassword.value ?: ""

    fun checkElevation(): LiveData<Boolean> = withElevation

    fun setElevation(withElevation: Boolean) {
        this.withElevation.value = withElevation
    }

    fun getLinkFragmentTitle(): String {
        if (!this::linkFragmentTitle.isInitialized) {
            linkFragmentTitle =
                getString(if (node.isExported) R.string.edit_link_option else R.string.context_get_link_menu)
                    .toUpperCase(Locale.getDefault())
        }

        return linkFragmentTitle
    }

    fun getNode(): MegaNode = node

    fun getLinkWithoutKey(): String = linkWithoutKey

    fun getLinkKey(): String = key

    fun export() {
        exportNodeUseCase.export(node)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { updateLink(node.handle) },
                onError = { error ->
                    logWarning(error.message)
                }
            )
            .addTo(composite)
    }

    fun exportWithTimestamp(expiryDate: Int) {
        exportNodeUseCase.exportWithTimestamp(node, expiryDate)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { updateLink(node.handle) },
                onError = { error ->
                    logWarning(error.message)
                }
            )
            .addTo(composite)
    }

    fun copyLink() {
        LiveEventBus.get(EVENT_COPY_LINK_TO_CLIPBOARD, Pair::class.java).post(
            Pair(
                when {
                    isSendDecryptedKeySeparatelyEnabled -> linkWithoutKey
                    !isTextEmpty(getLinkWithPasswordText()) -> getLinkWithPasswordText()
                    else -> node.publicLink
                }, getString(R.string.link_copied_clipboard)
            )
        )
    }

    fun copyLinkKey() {
        LiveEventBus.get(EVENT_COPY_LINK_TO_CLIPBOARD, Pair::class.java)
            .post(Pair(key, getString(R.string.key_copied_clipboard)))
    }

    fun copyLinkPassword() {
        if (isTextEmpty(passwordLink)) {
            return
        }
        LiveEventBus.get(EVENT_COPY_LINK_TO_CLIPBOARD, Pair::class.java)
            .post(Pair(key, getString(R.string.password_copied_clipboard)))
    }

    fun getLinkPassword(): String? {
        return passwordLink
    }

    fun removeLinkWithPassword() {
        linkWithPassword.value = null
        passwordLink = null
        updateLink()
    }

    fun setLinkPassword(passwordLink: String) {
        this.passwordLink = passwordLink
    }

    fun shouldShowCopyright(): Boolean =
        dbH.showCopyright.toBoolean() && (megaApi.publicLinks == null || megaApi.publicLinks.size == 0)

    fun updateShowCopyRight(show: Boolean) {
        dbH.setShowCopyright(show)
    }

    fun updateSendDecryptedKeySeparatelyEnabled(enabled: Boolean) {
        isSendDecryptedKeySeparatelyEnabled = enabled
        updateLink()
    }

    fun isPro(): Boolean =
        MegaApplication.getInstance().myAccountInfo.accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE

    /**
     * Updates the node from which the link is getting or managing.
     * Gets the link without its decryption key and the key separately if the node
     * it's already exported.
     *
     * @param handle The identifier of the MegaNode from which the link has to be managed.
     */
    fun updateLink(handle: Long) {
        node = megaApi.getNodeByHandle(handle)

        if (node.isExported) {
            val link = node.publicLink
            linkWithoutKey = LinksUtil.getLinkWithoutKey(link)
            key = LinksUtil.getKeyLink(link)
        }

        updateLink()
    }

    /**
     * Updates the text to show as link value.
     */
    private fun updateLink() {
        linkText.value = when {
            !node.isExported -> getString(R.string.link_request_status)
            isSendDecryptedKeySeparatelyEnabled -> linkWithoutKey
            !isTextEmpty(getLinkWithPasswordText()) -> getLinkWithPasswordText()
            else -> node.publicLink
        }
    }

    fun shareLinkAndKeyOrPassword(action: (Intent) -> Unit) {
        shareLink(getLinkAndKeyOrPasswordToShare(), action)
    }

    fun shareCompleteLink(action: (Intent) -> Unit) {
        shareLink(getLinkToShare(), action)
    }

    /**
     * Launches an intent to share the link outside the app.
     *
     * @param link The link to share.
     */
    fun shareLink(link: String? = null, action: (Intent) -> Unit) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = Constants.PLAIN_TEXT_SHARE_TYPE
        intent.putExtra(Intent.EXTRA_TEXT, link ?: node.publicLink)
        action.invoke(Intent.createChooser(intent, getString(R.string.context_get_link)))
    }

    /**
     * Finish the action of set or reset the password protection, updates the UI resetting the set
     * passwordFragment view and showing again the linkFragment view with the password protection
     * enabled.
     *
     * @param linkWithPassword Link with password protection.
     */
    fun setLinkWithPassword(linkWithPassword: String) {
        this.linkWithPassword.value = linkWithPassword
//        passwordFragment.resetView()
//        showFragment(GetLinkActivity.GET_LINK_FRAGMENT)
//        linkFragment.updatePasswordLayouts()
    }

    fun sendLinkToChat(
        data: Intent?,
        shouldAttachKeyOrPassword: Boolean,
        action: (Intent?) -> Unit
    ) {
        sendToChat(data, getLinkToShare(), shouldAttachKeyOrPassword, action)
    }

    /**
     * Shares the link and extra content if enabled (decryption key or password) to chat.
     *
     * @param data                      Intent containing the info to share the content to chats.
     * @param link                      The link to share.
     * @param shouldAttachKeyOrPassword True if should share the decryption key or password. False otherwise.
     */
    fun sendToChat(
        data: Intent?,
        link: String? = null,
        shouldAttachKeyOrPassword: Boolean,
        action: (Intent?) -> Unit
    ) {
        data?.putExtra(Constants.EXTRA_LINK, link ?: node.publicLink)

        if (shouldAttachKeyOrPassword) {
            if (!isTextEmpty(getLinkWithPasswordText())) {
                data?.putExtra(Constants.EXTRA_PASSWORD, passwordLink)
            } else {
                data?.putExtra(Constants.EXTRA_KEY, LinksUtil.getKeyLink(node.publicLink))
            }
        }

        action.invoke(data)
    }

    /**
     * Gets the string containing the link without its key and its key separately or the link
     * protected with password and the password depending on the current enabled option.
     *
     * @return The string with the info described.
     */
    private fun getLinkAndKeyOrPasswordToShare(): String =
        if (!isTextEmpty(getLinkPassword())) getString(
            R.string.share_link_with_password,
            getLinkPassword(),
            getLinkPassword()
        )
        else getString(
            R.string.share_link_with_key,
            LinksUtil.getLinkWithoutKey(node.publicLink),
            LinksUtil.getKeyLink(node.publicLink)
        )

    /**
     * Gets the link to share depending on the current enabled option. It can be:
     * - The link along with its decryption key
     * - The link without the decryption key
     * - The link along with its decryption key and with password protection
     *
     * @return The string with the info described.
     */
    fun getLinkToShare(): String = when {
        !isTextEmpty(getLinkWithPasswordText()) -> getLinkWithPasswordText()!!
        isSendDecryptedKeySeparatelyEnabled -> LinksUtil.getLinkWithoutKey(node.publicLink)
        else -> node.publicLink
    }

    /**
     * Checks if should show the warning to share the decryption key or password protection.
     *
     * @return True if password protection or send decryption key separately option is enabled.
     *         False otherwise.
     */
    fun shouldShowShareKeyOrPasswordDialog(): Boolean =
        !isTextEmpty(getLinkWithPasswordText()) || isSendDecryptedKeySeparatelyEnabled

    fun encryptLink(passwordLink: String) {
        setLinkPassword(passwordLink)
        encryptLinkWithPasswordUseCase.encrypt(node.publicLink, passwordLink)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { link -> linkWithPassword.value = link },
                onError = { error ->
                    logWarning(error.message)
                }
            )
            .addTo(composite)
    }
}