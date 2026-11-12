import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DebugOptimizationTest {
    @Test
    fun `keeps ordinary debug builds debuggable`() {
        listOf(null, "false", "0", "yes").forEach { property ->
            assertEquals(
                DebugOptimization(
                    isMinifyEnabled = false,
                    isShrinkResources = false,
                    isDebuggable = true,
                ),
                debugOptimization(property),
                "property=$property",
            )
        }
    }

    @Test
    fun `makes minified debug builds non-debuggable`() {
        listOf("", " ", "true", "TRUE", "1").forEach { property ->
            assertEquals(
                DebugOptimization(
                    isMinifyEnabled = true,
                    isShrinkResources = true,
                    isDebuggable = false,
                ),
                debugOptimization(property),
                "property=$property",
            )
        }
    }
}
