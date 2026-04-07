package net.dodian.uber.game.systems.skills.parity

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.cooking.RangeObjects
import net.dodian.uber.game.content.skills.herblore.GrimyHerbItems
import net.dodian.uber.game.content.skills.herblore.HerbloreSuppliesItems
import net.dodian.uber.game.content.skills.slayer.SlayerGemItems
import net.dodian.uber.game.content.skills.slayer.SlayerMaskItems
import net.dodian.uber.game.systems.dispatch.ContentModuleIndex
import net.dodian.uber.game.systems.action.PolicyPreset
import net.dodian.uber.game.systems.skills.plugin.SkillPluginDefinition
import net.dodian.uber.game.systems.skills.plugin.SkillPluginRegistry
import net.dodian.uber.game.systems.skills.plugin.SkillPluginSnapshot
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class SkillDoctorFinding(
    val code: String,
    val message: String,
)

data class SkillDoctorReport(
    val findings: List<SkillDoctorFinding>,
) {
    val isClean: Boolean
        get() = findings.isEmpty()
}

object SkillDoctor {
    private val gameplaySkills: Set<Skill> = LegacyContentParityCatalog.default.requiredSkillCoverage

    private val legacyRouteBypassChecks: Map<String, List<String>> = mapOf(
        "systems/interaction/InteractionProcessor.kt" to listOf(
            "Fishing.handleNpcOption(",
            "Mining.attempt(",
            "Woodcutting.attempt(",
        ),
    )

    private val bannedPluginPatterns = listOf("playerAction(", "while (true)")

    @JvmStatic
    fun snapshot(): SkillDoctorReport {
        return snapshot(registrySnapshot = null)
    }

    internal fun snapshot(registrySnapshot: SkillPluginSnapshot?): SkillDoctorReport {
        val findings = mutableListOf<SkillDoctorFinding>()
        val activeSnapshot = registrySnapshot

        try {
            activeSnapshot ?: SkillPluginRegistry.current()
        } catch (exception: IllegalArgumentException) {
            findings += SkillDoctorFinding(
                code = "duplicate-ownership",
                message = exception.message ?: "Duplicate skill interaction ownership detected.",
            )
        }

        val discoveredSkills = ContentModuleIndex.skillPlugins.map { it.definition.skill }.toSet()
        val missingSkills = gameplaySkills - discoveredSkills
        if (missingSkills.isNotEmpty()) {
            findings += SkillDoctorFinding(
                code = "missing-plugin-coverage",
                message = "Missing SkillPlugin coverage for gameplay skills: ${missingSkills.joinToString { it.name.lowercase() }}",
            )
        }
        val emptyGameplayPlugins = ContentModuleIndex.skillPlugins
            .filter { it.definition.skill in gameplaySkills }
            .filterNot { it.definition.hasAnyRouteBindings() }
            .map { "${it.definition.name}(${it.definition.skill.name.lowercase()})" }
        if (emptyGameplayPlugins.isNotEmpty()) {
            findings += SkillDoctorFinding(
                code = "empty-skill-plugin",
                message = "Gameplay SkillPlugin definitions must own at least one route. Empty: ${emptyGameplayPlugins.joinToString()}",
            )
        }

        val sourceRoot = resolveSourceRoot()
        if (sourceRoot == null) {
            findings += SkillDoctorFinding(
                code = "source-root-missing",
                message = "Could not locate source root for SkillDoctor static checks.",
            )
            return SkillDoctorReport(findings)
        }

        findings += scanLegacyRouteBypasses(sourceRoot)
        findings += scanBannedPluginPatterns(sourceRoot)
        findings += scanPresetDeclarations(sourceRoot)
        findings += scanMappedRouteOwnership(activeSnapshot ?: SkillPluginRegistry.current())
        findings += ContentParityDoctor.scan(skillSnapshot = activeSnapshot ?: SkillPluginRegistry.current())

        return SkillDoctorReport(findings)
    }

    @JvmStatic
    fun validateOrThrow() {
        validateOrThrow(registrySnapshot = null)
    }

    internal fun validateOrThrow(registrySnapshot: SkillPluginSnapshot?) {
        val report = snapshot(registrySnapshot = registrySnapshot)
        if (report.isClean) {
            return
        }
        val rendered = report.findings.joinToString(separator = "\n") { finding ->
            "[${finding.code}] ${finding.message}"
        }
        throw IllegalStateException("SkillDoctor validation failed:\n$rendered")
    }

    private fun scanLegacyRouteBypasses(sourceRoot: Path): List<SkillDoctorFinding> {
        val findings = mutableListOf<SkillDoctorFinding>()
        legacyRouteBypassChecks.forEach { (relativeFile, markers) ->
            val file = sourceRoot.resolve("net/dodian/uber/game/$relativeFile")
            if (!Files.exists(file)) {
                return@forEach
            }
            val source = Files.readString(file)
            markers.forEach { marker ->
                if (source.contains(marker)) {
                    findings += SkillDoctorFinding(
                        code = "route-bypass",
                        message = "$relativeFile contains legacy skill dispatch marker: $marker",
                    )
                }
            }
        }
        return findings
    }

    private fun scanBannedPluginPatterns(sourceRoot: Path): List<SkillDoctorFinding> {
        val findings = mutableListOf<SkillDoctorFinding>()
        val skillsRoot = sourceRoot.resolve("net/dodian/uber/game/content/skills")
        if (!Files.exists(skillsRoot)) {
            return findings
        }

        Files.walk(skillsRoot).use { stream ->
            stream
                .filter { path -> path.toString().endsWith(".kt") }
                .forEach { path ->
                    val source = Files.readString(path)
                    if (!source.contains(Regex("""object\s+\w+SkillPlugin\s*:"""))) {
                        return@forEach
                    }
                    bannedPluginPatterns.forEach { pattern ->
                        if (source.contains(pattern)) {
                            findings += SkillDoctorFinding(
                                code = "banned-orchestration",
                                message = path.toString() + " contains forbidden pattern: $pattern",
                            )
                        }
                    }
                }
        }
        return findings
    }

    private fun scanPresetDeclarations(sourceRoot: Path): List<SkillDoctorFinding> {
        val findings = mutableListOf<SkillDoctorFinding>()
        val skillsRoot = sourceRoot.resolve("net/dodian/uber/game/content/skills")
        if (!Files.exists(skillsRoot)) {
            return findings
        }

        val validPresetNames = PolicyPreset.values().map { it.name }.toSet()
        val callNames = listOf("objectClick", "npcClick", "itemOnItem", "itemClick", "itemOnObject", "button")

        Files.walk(skillsRoot).use { stream ->
            stream
                .filter { path -> path.toString().endsWith(".kt") }
                .forEach { path ->
                    val source = Files.readString(path)
                    if (!source.contains(Regex("""object\s+\w+SkillPlugin\s*:"""))) {
                        return@forEach
                    }

                    callNames.forEach { callName ->
                        val callRegex = Regex("""$callName\s*\((.*?)\)""", setOf(RegexOption.DOT_MATCHES_ALL))
                        callRegex.findAll(source).forEach { match ->
                            val args = match.groupValues[1]
                            if (!Regex("""preset\s*=\s*PolicyPreset\.[A-Z_]+""").containsMatchIn(args)) {
                                findings += SkillDoctorFinding(
                                    code = "missing-policy-preset",
                                    message = "${path.toString()} has $callName(...) without explicit preset = PolicyPreset.<NAME>",
                                )
                            } else {
                                val presetMatch = Regex("""preset\s*=\s*PolicyPreset\.([A-Z_]+)""").find(args)
                                val presetName = presetMatch?.groupValues?.get(1)
                                if (presetName == null || presetName !in validPresetNames) {
                                    findings += SkillDoctorFinding(
                                        code = "invalid-policy-preset",
                                        message = "${path.toString()} has invalid policy preset on $callName(...): ${presetName ?: "unknown"}",
                                    )
                                }
                            }
                        }
                    }
                }
        }
        return findings
    }

    private fun resolveSourceRoot(): Path? {
        val candidates = listOf(
            Paths.get("src/main/kotlin"),
            Paths.get("game-server/src/main/kotlin"),
            Paths.get("../game-server/src/main/kotlin"),
        )
        return candidates.firstOrNull { Files.exists(it.resolve("net/dodian/uber/game")) }?.toAbsolutePath()?.normalize()
    }

    private fun scanMappedRouteOwnership(skillSnapshot: SkillPluginSnapshot): List<SkillDoctorFinding> {
        val findings = mutableListOf<SkillDoctorFinding>()

        RangeObjects.objectIds.forEach { objectId ->
            if (skillSnapshot.itemOnObjectBinding(objectId = objectId, itemId = -1) == null) {
                findings += SkillDoctorFinding(
                    code = "missing-route-ownership",
                    message = "Cooking route ownership missing item-on-object binding for range objectId=$objectId.",
                )
            }
        }

        GrimyHerbItems.itemIds.forEach { itemId ->
            if (skillSnapshot.itemBinding(option = 1, itemId = itemId) == null) {
                findings += SkillDoctorFinding(
                    code = "missing-route-ownership",
                    message = "Herblore route ownership missing item click option=1 binding for itemId=$itemId.",
                )
            }
        }
        HerbloreSuppliesItems.itemIds.forEach { itemId ->
            if (skillSnapshot.itemBinding(option = 1, itemId = itemId) == null) {
                findings += SkillDoctorFinding(
                    code = "missing-route-ownership",
                    message = "Herblore route ownership missing supplies click option=1 binding for itemId=$itemId.",
                )
            }
        }

        SlayerGemItems.itemIds.forEach { itemId ->
            for (option in 1..3) {
                if (skillSnapshot.itemBinding(option = option, itemId = itemId) == null) {
                    findings += SkillDoctorFinding(
                        code = "missing-route-ownership",
                        message = "Slayer route ownership missing gem click option=$option binding for itemId=$itemId.",
                    )
                }
            }
        }
        SlayerMaskItems.itemIds.forEach { itemId ->
            if (skillSnapshot.itemBinding(option = 3, itemId = itemId) == null) {
                findings += SkillDoctorFinding(
                    code = "missing-route-ownership",
                    message = "Slayer route ownership missing mask click option=3 binding for itemId=$itemId.",
                )
            }
        }

        return findings
    }
}

private fun SkillPluginDefinition.hasAnyRouteBindings(): Boolean {
    return objectBindings.isNotEmpty() ||
        npcBindings.isNotEmpty() ||
        itemOnItemBindings.isNotEmpty() ||
        itemBindings.isNotEmpty() ||
        itemOnObjectBindings.isNotEmpty() ||
        buttonBindings.isNotEmpty()
}
