import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SigningTaskPolicyTest {
    @Test
    fun `requires signing for artifact tasks targeting the application project`() {
        assertTrue(requiresReleaseSigning(listOf(":app:assembleGithubRelease"), ":app"))
        assertTrue(requiresReleaseSigning(listOf("app:bundlePlayRelease"), ":app"))
        assertFalse(requiresReleaseSigning(listOf("assembleDebug"), ":app"))
        assertTrue(requiresReleaseSigning(listOf(":app:packageGithubRelease"), ":app"))
    }

    @Test
    fun `local debug builds do not require upstream release keys`() {
        assertFalse(requiresReleaseSigning(listOf(":app:assembleGithubDebug"), ":app"))
        assertFalse(requiresReleaseSigning(listOf(":app:installPlayDebug"), ":app"))
        assertTrue(requiresReleaseSigning(listOf("build"), ":app"))
        assertTrue(requiresReleaseSigning(listOf(":app:assembleGithub"), ":app"))
    }

    @Test
    fun `ignores artifact tasks targeting sibling projects`() {
        assertFalse(requiresReleaseSigning(listOf(":manager:assembleDebug"), ":app"))
        assertFalse(requiresReleaseSigning(listOf(":xmsf:bundleReleaseAar"), ":app"))
    }

    @Test
    fun `ignores compilation and resource packaging tasks`() {
        assertFalse(requiresReleaseSigning(listOf(":app:compileGithubDebugKotlin"), ":app"))
        assertFalse(requiresReleaseSigning(listOf(":app:packageGithubDebugResources"), ":app"))
    }

    @Test
    fun `bundle-only mode follows the target application project`() {
        assertTrue(requestsOnlyApplicationBundles(listOf(":app:bundlePlayRelease"), ":app"))
        assertTrue(requestsOnlyApplicationBundles(listOf("bundlePlayRelease"), ":app"))
        assertFalse(requestsOnlyApplicationBundles(listOf(":feature:bundlePlayRelease"), ":app"))
        assertFalse(requestsOnlyApplicationBundles(listOf(":app:assembleGithubRelease"), ":app"))
        assertFalse(
            requestsOnlyApplicationBundles(
                listOf(":app:assembleGithubRelease", ":app:bundlePlayRelease"),
                ":app",
            ),
        )
    }

    @Test
    fun `any target bundle request disables APK splits`() {
        assertTrue(requestsApplicationBundle(listOf(":app:bundlePlayRelease"), ":app"))
        assertTrue(
            requestsApplicationBundle(
                listOf(":app:assembleGithubRelease", ":app:bundlePlayRelease"),
                ":app",
            ),
        )
        assertFalse(requestsApplicationBundle(listOf(":feature:bundlePlayRelease"), ":app"))
    }
}
