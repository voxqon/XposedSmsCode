import com.android.build.api.dsl.CompileSdkSpec
import org.gradle.api.Project

/**
 * Parses the shared `compileSdk` catalog value (plain level `37`, minor `37.1`,
 * beta `37.2-beta3`, or a preview codename) into an AGP compileSdk configuration
 * lambda.
 *
 * Consumers that configure `compileSdk` outside the magisk convention plugins
 * (e.g. Kotlin Multiplatform `android {}` targets) should call this instead of
 * `libs.versions.compileSdk.get().toInt()`, which only supports plain levels.
 */
fun Project.magiskCompileSdk(): (CompileSdkSpec) -> Unit {
    val raw = catalogVersion("compileSdk")
    return fun(spec: CompileSdkSpec) {
        when (val sdk = parseAndroidSdkSpec(raw)) {
            is AndroidSdkSpec.ApiLevel -> spec.version = spec.release(sdk.value)
            is AndroidSdkSpec.Minor -> spec.version =
                spec.release(sdk.apiLevel) { minorApiLevel = sdk.minorApiLevel }
            is AndroidSdkSpec.Beta -> spec.version = spec.beta(sdk.apiLevel) {
                minorApiLevel = sdk.minorApiLevel
                betaVersion = sdk.betaVersion
            }
            is AndroidSdkSpec.Preview -> spec.version = spec.preview(sdk.codename)
        }
    }
}
