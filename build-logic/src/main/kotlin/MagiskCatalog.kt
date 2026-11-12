import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal fun Project.libsCatalog(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.catalogVersion(name: String): String = libsCatalog()
    .findVersion(name)
    .orElseThrow { IllegalArgumentException("Version '$name' not found in consumer libs catalog") }
    .requiredVersion

internal fun Project.catalogVersionOrNull(name: String): String? = libsCatalog()
    .findVersion(name)
    .orElse(null)
    ?.requiredVersion

internal fun Project.propertyOrCatalogVersion(property: String, catalogKey: String): String =
    providers.gradleProperty(property).getOrElse(catalogVersion(catalogKey))

internal fun Project.catalogIntOrNull(name: String): Int? = catalogVersionOrNull(name)?.toIntOrNull()

internal fun Project.catalogLibrary(alias: String): Provider<MinimalExternalModuleDependency> {
    val catalog = libsCatalog()
    val dependency = catalog.findLibrary(alias)
    if (dependency.isPresent) {
        return dependency.get()
    }
    throw IllegalArgumentException(
        "Library alias not found in consumer libs catalog: $alias",
    )
}

internal fun Project.magiskJavaTarget(): String = catalogVersion("javaBytecode")

internal fun Project.magiskKotlinJvmTarget(): JvmTarget {
    val javaTarget = magiskJavaTarget()
    return JvmTarget.fromTarget(javaTarget)
}
