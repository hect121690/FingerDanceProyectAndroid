package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.app.di.LoggingModule
import mega.privacy.android.app.di.settings.SettingsModule
import mega.privacy.android.app.di.settings.SettingsUseCases
import mega.privacy.android.app.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.app.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.app.domain.usecase.CanDeleteAccount
import mega.privacy.android.app.domain.usecase.FetchAutoAcceptQRLinks
import mega.privacy.android.app.domain.usecase.FetchMultiFactorAuthSetting
import mega.privacy.android.app.domain.usecase.GetAccountDetails
import mega.privacy.android.app.domain.usecase.GetChatImageQuality
import mega.privacy.android.app.domain.usecase.GetPreference
import mega.privacy.android.app.domain.usecase.GetStartScreen
import mega.privacy.android.app.domain.usecase.InitialiseLogging
import mega.privacy.android.app.domain.usecase.IsCameraSyncEnabled
import mega.privacy.android.app.domain.usecase.IsChatLoggedIn
import mega.privacy.android.app.domain.usecase.IsHideRecentActivityEnabled
import mega.privacy.android.app.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.app.domain.usecase.MonitorAutoAcceptQRLinks
import mega.privacy.android.app.domain.usecase.PutPreference
import mega.privacy.android.app.domain.usecase.RefreshPasscodeLockPreference
import mega.privacy.android.app.domain.usecase.RequestAccountDeletion
import mega.privacy.android.app.domain.usecase.ResetSdkLogger
import mega.privacy.android.app.domain.usecase.SetChatImageQuality
import mega.privacy.android.app.domain.usecase.SetChatLogsEnabled
import mega.privacy.android.app.domain.usecase.SetSdkLogsEnabled
import mega.privacy.android.app.domain.usecase.ToggleAutoAcceptQRLinks
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import test.mega.privacy.android.app.TEST_USER_ACCOUNT

/**
 * Test settings module
 *
 * Provides test dependencies for Settings tests
 */
@TestInstallIn(
    replaces = [SettingsModule::class, SettingsUseCases::class, LoggingModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestSettingsModule {
    val canDeleteAccount = mock<CanDeleteAccount> { on { invoke(any()) }.thenReturn(true) }
    val getStartScreen = mock<GetStartScreen> { on { invoke() }.thenReturn(emptyFlow()) }
    val isMultiFactorAuthAvailable =
        mock<IsMultiFactorAuthAvailable> { on { invoke() }.thenReturn(true) }
    val fetchAutoAcceptQRLinks =
        mock<FetchAutoAcceptQRLinks> { onBlocking { invoke() }.thenReturn(false) }
    val fetchMultiFactorAuthSetting =
        mock<FetchMultiFactorAuthSetting> { on { invoke() }.thenReturn(emptyFlow()) }
    val getAccountDetails =
        mock<GetAccountDetails> { onBlocking { invoke(any()) }.thenReturn(TEST_USER_ACCOUNT) }
    val shouldHideRecentActivity =
        mock<IsHideRecentActivityEnabled> { on { invoke() }.thenReturn(emptyFlow()) }
    val getChatImageQuality = mock<GetChatImageQuality> { on { invoke() }.thenReturn(emptyFlow()) }
    val setChatImageQuality = mock<SetChatImageQuality>()
    val getOfflineThumbnailFileWrapper = mock<GetOfflineThumbnailFileWrapper>()

    @Provides
    fun provideGetAccountDetails(): GetAccountDetails = getAccountDetails

    @Provides
    fun provideCanDeleteAccount(): CanDeleteAccount = canDeleteAccount

    @Provides
    fun provideRefreshPasscodeLockPreference(): RefreshPasscodeLockPreference =
        mock()

    @Provides
    fun provideIsLoggingEnabled(): AreSdkLogsEnabled =
        mock { on { invoke() }.thenReturn(flowOf(true)) }

    @Provides
    fun provideIsChatLoggingEnabled(): AreChatLogsEnabled =
        mock { on { invoke() }.thenReturn(flowOf(true)) }

    @Provides
    fun provideIsCameraSyncEnabled(): IsCameraSyncEnabled = mock()

    @Provides
    fun provideIsMultiFactorAuthAvailable(): IsMultiFactorAuthAvailable =
        isMultiFactorAuthAvailable


    @Provides
    fun provideFetchContactLinksOption(): FetchAutoAcceptQRLinks =
        fetchAutoAcceptQRLinks


    @Provides
    fun provideFetchMultiFactorAuthSetting(): FetchMultiFactorAuthSetting =
        fetchMultiFactorAuthSetting


    @Provides
    fun provideGetStartScreen(): GetStartScreen = getStartScreen


    @Provides
    fun provideShouldHideRecentActivity(): IsHideRecentActivityEnabled =
        shouldHideRecentActivity

    @Provides
    fun provideToggleAutoAcceptQRLinks(): ToggleAutoAcceptQRLinks =
        mock()

    @Provides
    fun provideRequestAccountDeletion(): RequestAccountDeletion = mock()


    @Provides
    fun provideIsChatLoggedIn(): IsChatLoggedIn = mock { on { invoke() }.thenReturn(flowOf(true)) }

    @Provides
    fun provideSetLoggingEnabled(): SetSdkLogsEnabled = mock()

    @Provides
    fun provideSetChatLoggingEnabled(): SetChatLogsEnabled = mock()

    @Provides
    fun provideInitialiseLogging(): InitialiseLogging = mock()

    @Provides
    fun provideResetSdkLogger(): ResetSdkLogger = mock()

    @Provides
    fun provide(): MonitorAutoAcceptQRLinks = mock {
        on { invoke() }.thenReturn(
            flowOf(true)
        )
    }

    @Provides
    fun provideGetChatImageQuality(): GetChatImageQuality = getChatImageQuality

    @Provides
    fun provideSetChatImageQuality(): SetChatImageQuality = setChatImageQuality

    @Provides
    fun providePutStringPreference(): PutPreference<String> =
        mock()

    @Provides
    fun providePutStringSetPreference(): PutPreference<MutableSet<String>> =
        mock()

    @Provides
    fun providePutIntPreference(): PutPreference<Int> =
        mock()

    @Provides
    fun providePutLongPreference(): PutPreference<Long> =
        mock()

    @Provides
    fun providePutFloatPreference(): PutPreference<Float> =
        mock()

    @Provides
    fun providePutBooleanPreference(): PutPreference<Boolean> =
        mock()

    @Provides
    fun provideGetStringPreference(): GetPreference<String?> =
        mock { on { invoke(anyOrNull(), anyOrNull()) }.thenReturn(emptyFlow()) }

    @Provides
    fun provideGetStringSetPreference(): GetPreference<MutableSet<String>?> =
        mock { on { invoke(anyOrNull(), anyOrNull()) }.thenReturn(emptyFlow()) }

    @Provides
    fun provideGetIntPreference(): GetPreference<Int> =
        mock { on { invoke(anyOrNull(), any()) }.thenAnswer{ flowOf(it.arguments[1])} }

    @Provides
    fun provideGetLongPreference(): GetPreference<Long> =
        mock { on { invoke(anyOrNull(), any()) }.thenAnswer{ flowOf(it.arguments[1])} }

    @Provides
    fun provideGetFloatPreference(): GetPreference<Float> =
        mock { on { invoke(anyOrNull(), any()) }.thenAnswer{ flowOf(it.arguments[1])} }

    @Provides
    fun provideGetBooleanPreference(): GetPreference<Boolean> =
        mock { on { invoke(anyOrNull(), any()) }.thenAnswer{ flowOf(it.arguments[1])} }
}