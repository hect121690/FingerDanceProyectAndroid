package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.di.ApplicationScope
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.domain.repository.AvatarRepository
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Default [AvatarRepository] implementation
 *
 * @param megaApiGateway
 * @param cacheFolderGateway
 * @param sharingScope scope for share flow
 * @param ioDispatcher coroutine dispatcher to execute
 */
class DefaultAvatarRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val cacheFolderGateway: CacheFolderGateway,
    @ApplicationScope private val sharingScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AvatarRepository {

    private val monitorMyAvatarFile = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
        .mapNotNull { it.users?.find { user -> user.isOwnChange <= 0 && user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) && user.email == megaApiGateway.accountEmail } }
        .map { user ->
            deleteAvatarFile(user)
            loadAvatarFile(user)
        }
        .catch { Timber.e(it) }
        .flowOn(ioDispatcher)
        .stateIn(sharingScope, SharingStarted.WhileSubscribed(), null)

    override fun monitorMyAvatarFile() = monitorMyAvatarFile

    private fun deleteAvatarFile(user: MegaUser) {
        val oldFile =
            cacheFolderGateway.buildAvatarFile(user.email + FileUtil.JPG_EXTENSION) ?: return
        if (oldFile.exists()) {
            oldFile.delete()
        }
    }

    private suspend fun loadAvatarFile(user: MegaUser): File? {
        val avatarFile =
            cacheFolderGateway.buildAvatarFile(user.email + FileUtil.JPG_EXTENSION) ?: return null
        megaApiGateway.getUserAvatar(user, avatarFile.absolutePath)
        return avatarFile
    }
}