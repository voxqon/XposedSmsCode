internal data class DebugOptimization(
    val isMinifyEnabled: Boolean,
    val isShrinkResources: Boolean,
    val isDebuggable: Boolean,
)

internal fun debugOptimization(minifyDebugProperty: String?): DebugOptimization {
    val minifyDebug = when {
        minifyDebugProperty == null -> false
        minifyDebugProperty.isBlank() -> true
        minifyDebugProperty.equals("true", ignoreCase = true) -> true
        minifyDebugProperty == "1" -> true
        else -> false
    }
    return DebugOptimization(
        isMinifyEnabled = minifyDebug,
        isShrinkResources = minifyDebug,
        isDebuggable = !minifyDebug,
    )
}
