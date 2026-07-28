package net.dodian.uber.game.api.plugin

data class PluginModuleMetadata(
    val name: String,
    val description: String,
    val version: String = "1.0.0",
    val owner: String = "unspecified",
)

interface PluginModuleMetadataProvider {
    val pluginMetadata: PluginModuleMetadata
}

enum class ContentMaturity { LEGACY, ALPHA, BETA, STABLE }

data class ContentModuleManifest(
    val id: String,
    val owner: String,
    val version: String,
    val featureFlag: String = ALWAYS_ENABLED,
    val maturity: ContentMaturity = ContentMaturity.BETA,
    val declaredRouteKeys: Set<String> = emptySet(),
) {
    val featureFlagKey: String get() = featureFlag

    init {
        require(Regex("[a-z][a-z0-9_.-]{2,127}").matches(id)) { "Invalid content module id '$id'" }
        require(owner.isNotBlank() && version.isNotBlank() && featureFlag.isNotBlank())
    }

    companion object {
        const val ALWAYS_ENABLED = "system.always_enabled"
    }

    val isLegacy get() = maturity == ContentMaturity.LEGACY
}

interface ContentModuleManifestProvider {
    val contentManifest: ContentModuleManifest
}

object ContentModuleFeatureState {
    @Volatile private var disabledForTests: Set<String> = emptySet()

    fun isEnabled(manifest: ContentModuleManifest): Boolean =
        manifest.featureFlag == ContentModuleManifest.ALWAYS_ENABLED ||
            (manifest.featureFlag !in disabledForTests && isFlagEnabled(manifest.featureFlag))

    fun setDisabledForTests(disabledKeys: Set<String>) {
        disabledForTests = disabledKeys
    }

    fun disabledModuleIds(): Set<String> = ContentPlatformCatalog.snapshot().let { snap ->
        snap.modules.map { it.id }.toSet() - snap.enabledModuleIds
    }

    private fun isFlagEnabled(flagKey: String): Boolean {
        val sysProp = System.getProperty(flagKey)
        if (sysProp != null) return sysProp.toBoolean()
        val envVar = System.getenv(flagKey.uppercase().replace('.', '_'))
        if (envVar != null) return envVar.toBoolean()
        return true
    }
}

data class ContentPlatformSnapshot(
    val modules: List<ContentModuleManifest>,
    val enabledModuleIds: Set<String>,
    val fingerprint: String,
) {
    val enabledCount get() = enabledModuleIds.size
    val disabledCount get() = modules.size - enabledModuleIds.size
    fun module(id: String): ContentModuleManifest? = modules.firstOrNull { it.id == id }
}

/** Immutable, deterministic module inventory used by readiness and staff diagnostics. */
object ContentPlatformCatalog {
    @Volatile private var current = ContentPlatformSnapshot(emptyList(), emptySet(), fingerprint(emptyList()))

    fun publish(manifests: Collection<ContentModuleManifest>) {
        val sorted = manifests.sortedBy { it.id }
        val dupes = sorted.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(dupes.isEmpty()) { "Duplicate content module manifest id: $dupes" }
        current = ContentPlatformSnapshot(
            modules = sorted,
            enabledModuleIds = sorted.filter(ContentModuleFeatureState::isEnabled).mapTo(linkedSetOf()) { it.id },
            fingerprint = fingerprint(sorted),
        )
    }

    fun snapshot(): ContentPlatformSnapshot = current

    private fun fingerprint(manifests: Collection<ContentModuleManifest>): String {
        if (manifests.isEmpty()) return "0"
        return manifests.joinToString(";") { "${it.id}:${it.version}:${it.maturity.name}" }.hashCode().toString()
    }
}
