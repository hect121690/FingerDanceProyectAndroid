package mega.privacy.android.data.facade

import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.qualifier.MegaApiFolder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mega api folder facade
 *
 * Implements [MegaApiFolderGateway] and provides a facade over [MegaApiAndroid]
 *
 * @property megaApiFolder
 */
internal class MegaApiFolderFacade @Inject constructor(
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
) : MegaApiFolderGateway {

    override var accountAuth: String
        get() = megaApiFolder.accountAuth
        set(value) {
            megaApiFolder.accountAuth = value
        }

    override suspend fun authorizeNode(handle: Long): MegaNode? =
        megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(handle))
}