import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AndroidSdkSpecTest {
    @Test
    fun `parses numeric SDK levels`() {
        assertEquals(AndroidSdkSpec.ApiLevel(37), parseAndroidSdkSpec("37"))
        assertEquals(AndroidSdkSpec.ApiLevel(37), parseAndroidSdkSpec("android-37"))
    }

    @Test
    fun `parses preview SDK codenames`() {
        assertEquals(AndroidSdkSpec.Preview("Baklava"), parseAndroidSdkSpec("android-Baklava"))
        assertEquals(AndroidSdkSpec.Preview("Baklava"), parseAndroidSdkSpec("Baklava"))
    }

    @Test
    fun `parses minor API levels`() {
        assertEquals(AndroidSdkSpec.Minor(37, 1), parseAndroidSdkSpec("37.1"))
        assertEquals(AndroidSdkSpec.Minor(37, 2), parseAndroidSdkSpec("android-37.2"))
    }

    @Test
    fun `parses beta platforms`() {
        assertEquals(AndroidSdkSpec.Beta(37, 2, 3), parseAndroidSdkSpec("37.2-beta3"))
        assertEquals(AndroidSdkSpec.Beta(37, 2, 3), parseAndroidSdkSpec("android-37.2-beta3"))
    }

    @Test
    fun `rejects blank SDK values`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseAndroidSdkSpec("  ")
        }
    }
}
