package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Use case for monitoring logouts.
 */
class MonitorLogoutUseCase @Inject constructor(private val loginRepository: LoginRepository) {

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = loginRepository.monitorLogout()
}