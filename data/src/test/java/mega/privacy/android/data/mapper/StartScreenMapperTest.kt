package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.preference.StartScreen
import org.junit.Test

class StartScreenMapperTest {
    private val underTest: StartScreenMapper = { StartScreen(it) }

    @Test
    fun `test that null id returns null`() {
        assertThat(underTest(null)).isNull()
    }

    @Test
    fun `test that values are mapped by id`() {
        StartScreen.values().forEach {
            assertThat(underTest(it.id)).isEqualTo(it)
        }
    }

    @Test
    fun `test that value not found in ids maps to null`() {
        val notValid = StartScreen.values().maxOf { it.id } + 1
        assertThat(underTest(notValid)).isNull()
    }
}