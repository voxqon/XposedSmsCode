import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.DynamicFeatureAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

plugins {
    id("magisk.android.common")
}

// The matrix is declared by the root project. Reaching here without it means the two
// plugins were applied in the wrong order; building on from an empty matrix would drop
// every flavor without saying so, which is exactly the failure this layout exists to
// prevent.
val distribution = rootProject.extensions.findByName("magiskDistribution") as? MagiskDistributionExtension
    ?: error(
        "Apply 'magisk.distribution.policy' in the root project to declare the distribution " +
            "matrix before applying 'magisk.android.distribution' in ${project.path}.",
    )

fun CommonExtension.configureMagiskDistributionFlavors() {
    val dimensionName = distribution.dimension.get()
    flavorDimensions += dimensionName
    productFlavors {
        distribution.flavorSpecs.get().forEach { spec ->
            maybeCreate(spec.name).apply { dimension = dimensionName }
        }
    }
}

fun configureMagiskDistributionBuildConfigFields(
    variant: Variant,
    commitHashField: Provider<BuildConfigField<java.io.Serializable>>,
) {
    val fields = variant.buildConfigFields ?: return
    fields.put("COMMIT_HASH", commitHashField)

    fun putBoolean(name: String, value: Boolean) =
        fields.put(name, BuildConfigField<java.io.Serializable>("boolean", value.toString(), null))

    fun putString(name: String, value: String) =
        // BuildConfigField values are emitted verbatim into the generated Java source,
        // so a String field has to carry its own literal quotes.
        fields.put(name, BuildConfigField<java.io.Serializable>("String", "\"$value\"", null))

    val dimensionName = distribution.dimension.get()
    val flavorName = variant.productFlavors.toMap()[dimensionName]
    val spec = distribution.flavorSpecs.get().firstOrNull { it.name == flavorName } ?: return

    spec.booleanFields.get().forEach { (name, value) -> putBoolean(name, value) }
    spec.stringFields.get().forEach { (name, value) -> putString(name, value) }
    spec.versionFields.get().forEach { (name, catalogKey) ->
        putString(name, project.catalogVersionOrNull(catalogKey).orEmpty())
    }
}

pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        configureMagiskDistributionFlavors()
    }
    val commitHashField = registerCommitHashBuildConfigField(
        preferInjectedHash = distribution.preferInjectedCommitHash.get(),
    )
    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            configureMagiskDistributionBuildConfigFields(variant, commitHashField)
        }
    }
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        configureMagiskDistributionFlavors()
    }
    val commitHashField = registerCommitHashBuildConfigField(
        preferInjectedHash = distribution.preferInjectedCommitHash.get(),
    )
    extensions.getByType<LibraryAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            configureMagiskDistributionBuildConfigFields(variant, commitHashField)
        }
    }
}

pluginManager.withPlugin("com.android.dynamic-feature") {
    extensions.configure<DynamicFeatureExtension> {
        configureMagiskDistributionFlavors()
    }
    // A dynamic-feature module only produces variants for flavors that actually ship
    // through a store with feature delivery. The rest stay disabled so a plain
    // assembleDebug at the root does not schedule their manifest merge, which fails on
    // the empty feature metadata.
    val supportedFlavors = distribution.dynamicFeatureFlavors.get().toSet()
    val components = extensions.getByType<DynamicFeatureAndroidComponentsExtension>()
    components.beforeVariants(components.selector().all()) { variantBuilder ->
        val flavorName = variantBuilder.productFlavors.toMap()[distribution.dimension.get()]
        if (flavorName !in supportedFlavors) {
            variantBuilder.enable = false
        }
    }
}
