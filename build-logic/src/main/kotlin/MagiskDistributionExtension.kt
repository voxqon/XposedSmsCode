import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * The capability flags one distribution flavor contributes to a module's BuildConfig.
 *
 * Deliberately plain data rather than callbacks: the plugin decides when the values are
 * materialized (AGP resolves `buildConfigFields` at configuration time), the consuming
 * repository decides what they say. Keeping it declarative also lets a test assert the
 * whole matrix without running AGP.
 */
abstract class MagiskDistributionFlavorSpec @Inject constructor(
    val name: String,
) {
    internal abstract val booleanFields: MapProperty<String, Boolean>

    internal abstract val stringFields: MapProperty<String, String>

    /** BuildConfig String fields whose value is resolved from a catalog version key. */
    internal abstract val versionFields: MapProperty<String, String>

    fun buildConfigBoolean(name: String, value: Boolean) {
        booleanFields.put(name, value)
    }

    fun buildConfigString(name: String, value: String) {
        stringFields.put(name, value)
    }

    fun buildConfigVersion(name: String, catalogKey: String) {
        versionFields.put(name, catalogKey)
    }
}

/**
 * Root-project declaration of a repository's distribution matrix.
 *
 * The matrix is product data — which flavors ship and what each one compiles in — while
 * the shared convention owns only the mechanism that turns this table into AGP flavors.
 * Before this existed, the matrix was restated in two app-specific convention plugins
 * and again inside shared library modules; each restatement could drift on its own, and
 * a library declaring a flavor the app did not have fell back through
 * `missingDimensionStrategy` silently instead of failing.
 */
abstract class MagiskDistributionExtension @Inject constructor(
    private val objects: ObjectFactory,
) {
    /** Flavor dimension to create. */
    abstract val dimension: Property<String>

    internal abstract val flavorSpecs: ListProperty<MagiskDistributionFlavorSpec>

    /**
     * Flavors allowed to build dynamic-feature variants. Empty disables every
     * dynamic-feature variant, which is the right default: Play Feature Delivery is a
     * store capability, so flavors that never ship to that store would otherwise
     * schedule a manifest merge that fails on empty feature metadata.
     */
    abstract val dynamicFeatureFlavors: ListProperty<String>

    /** Prefer an injected commit hash (CI variable or -P) over a git lookup. */
    abstract val preferInjectedCommitHash: Property<Boolean>

    fun flavor(name: String, configure: MagiskDistributionFlavorSpec.() -> Unit = {}) {
        flavorSpecs.add(
            objects.newInstance(MagiskDistributionFlavorSpec::class.java, name).apply(configure),
        )
    }
}
