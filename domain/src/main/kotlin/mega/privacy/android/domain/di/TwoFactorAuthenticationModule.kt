package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

/**
 * Domain Dagger module for Use Cases in TwoFactorAuthentication Activity
 */
@Module(includes = [InternalTwoFactorAuthenticationModule::class])
@DisableInstallInCheck
abstract class TwoFactorAuthenticationModule