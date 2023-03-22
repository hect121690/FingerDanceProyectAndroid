package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Use case for broadcast fetch nodes finish.
 */
class BroadcastFetchNodesFinishUseCase @Inject constructor(private val loginRepository: LoginRepository) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = loginRepository.broadcastFetchNodesFinish()
}