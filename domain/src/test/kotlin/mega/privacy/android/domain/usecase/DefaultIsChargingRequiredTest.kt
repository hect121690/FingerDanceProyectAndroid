package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

/**
 * Test class for [DefaultIsChargingRequired]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultIsChargingRequiredTest {
    private lateinit var underTest: IsChargingRequired

    private val getVideoCompressionSizeLimitUseCase = mock<GetVideoCompressionSizeLimitUseCase>()
    private val isChargingRequiredForVideoCompressionUseCase =
        mock<IsChargingRequiredForVideoCompressionUseCase>()

    @Before
    fun setUp() {
        underTest = DefaultIsChargingRequired(
            getVideoCompressionSizeLimitUseCase = getVideoCompressionSizeLimitUseCase,
            isChargingRequiredForVideoCompressionUseCase = isChargingRequiredForVideoCompressionUseCase,
        )
    }

    @Test
    fun `test that false is returned if isChargingRequiredForVideoCompression is set to false`() =
        runTest {
            isChargingRequiredForVideoCompressionUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

            assertThat(underTest(5)).isFalse()
        }

    @Test
    fun `test that false is returned if queue is smaller than the limit`() = runTest {
        val queueSize = 5

        whenever(getVideoCompressionSizeLimitUseCase()).thenReturn(queueSize + 1)
        isChargingRequiredForVideoCompressionUseCase.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        assertThat(underTest(queueSize.toLong())).isFalse()
    }

    @Test
    fun `test that true is returned if queue is larger than the limit`() = runTest {
        val queueSize = 5

        whenever(getVideoCompressionSizeLimitUseCase()).thenReturn(queueSize - 1)
        isChargingRequiredForVideoCompressionUseCase.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        assertThat(underTest(queueSize.toLong())).isTrue()
    }
}