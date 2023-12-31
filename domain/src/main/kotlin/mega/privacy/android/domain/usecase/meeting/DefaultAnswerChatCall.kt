package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Default get chat participants use case implementation.
 */
class DefaultAnswerChatCall @Inject constructor(
    private val callRepository: CallRepository,
) : AnswerChatCall {

    override suspend fun invoke(chatId: Long, video: Boolean, audio: Boolean): ChatCall? {
        if (chatId == -1L)
            return null

        return runCatching {
            callRepository.answerChatCall(
                chatId,
                video,
                audio
            )
        }.fold(
            onSuccess = { request -> callRepository.getChatCall(request.chatHandle) },
            onFailure = { null }
        )
    }
}