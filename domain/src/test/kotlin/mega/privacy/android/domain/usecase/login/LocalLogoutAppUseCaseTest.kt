package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.StopAudioService
import mega.privacy.android.domain.usecase.StopCameraUpload
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class LocalLogoutAppUseCaseTest {

    private lateinit var underTest: LocalLogoutAppUseCase

    private val loginRepository = mock<LoginRepository>()
    private val accountRepository = mock<AccountRepository>()
    private val transferRepository = mock<TransferRepository>()
    private val pushesRepository = mock<PushesRepository>()
    private val billingRepository = mock<BillingRepository>()
    private val stopCameraUpload = mock<StopCameraUpload>()
    private val stopAudioService = mock<StopAudioService>()

    @Before
    fun setUp() {
        underTest = LocalLogoutAppUseCase(
            loginRepository = loginRepository,
            accountRepository = accountRepository,
            transferRepository = transferRepository,
            pushesRepository = pushesRepository,
            billingRepository = billingRepository,
            stopCameraUpload = stopCameraUpload,
            stopAudioService = stopAudioService
        )
    }

    @Test
    fun `test that all required functionalities are invoked`() = runTest {
        val clearPsa = mock<ClearPsa>()
        underTest.invoke(clearPsa)

        verify(transferRepository).cancelTransfers()
        verify(accountRepository).resetAccountAuth()
        verify(accountRepository).cancelAllNotifications()
        verify(accountRepository).clearAppDataAndCache()
        verify(accountRepository).clearAccountPreferences()
        verify(accountRepository).clearSharedPreferences()
        verify(accountRepository).resetAccountInfo()
        verify(pushesRepository).clearPushToken()
        verify(billingRepository).clearCache()
        verify(loginRepository).broadcastLogout()
        verify(stopCameraUpload).invoke()
        verify(stopAudioService).invoke()
        verify(clearPsa).invoke()
    }
}