import com.android.build.api.variant.BuildConfigField
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

internal abstract class GitCommitHashValueSource :
    ValueSource<String, GitCommitHashValueSource.Params> {
    interface Params : ValueSourceParameters {
        val gitProjectDir: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        return runCatching {
            val stdout = ByteArrayOutputStream()
            val result = execOperations.exec {
                commandLine(
                    "git",
                    "-C",
                    parameters.gitProjectDir.get(),
                    "rev-parse",
                    "--short",
                    "HEAD",
                )
                standardOutput = stdout
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                stdout.toString(Charsets.UTF_8.name()).trim().ifEmpty { "unknown" }
            } else {
                "unknown"
            }
        }.getOrElse { "unknown" }
    }
}

internal fun Project.registerCommitHashBuildConfigField(
    preferInjectedHash: Boolean = false,
): Provider<BuildConfigField<java.io.Serializable>> {
    // AGP materializes buildConfigFields while the configuration cache entry is
    // being built, so the value must resolve at configuration time; a
    // task-generated file is always too late for a fresh checkout. ValueSource
    // keeps the git lookup configuration-cache safe.
    val gitHashProvider = providers.of(GitCommitHashValueSource::class.java) {
        parameters.gitProjectDir.set(projectDir.absolutePath)
    }
    val hashProvider: Provider<String> = if (preferInjectedHash) {
        // CI environments and an explicit -PgitCommitHash=... take precedence
        // over the git lookup below.
        providers.environmentVariable("CI_COMMIT_SHORT_SHA")
            .orElse(providers.environmentVariable("GIT_COMMIT"))
            .orElse(providers.gradleProperty("gitCommitHash"))
            .orElse(gitHashProvider)
    } else {
        gitHashProvider
    }
    return hashProvider.map { hash ->
        BuildConfigField<java.io.Serializable>(
            "String",
            "\"$hash\"",
            "Git commit hash recorded at build time",
        )
    }
}
