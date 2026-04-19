package net.dodian.uber.game.skill.runtime.parity

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.herblore.GrimyHerbItems
import net.dodian.uber.game.skill.herblore.HerbloreSuppliesItems
import net.dodian.uber.game.skill.slayer.SlayerGemItems
import net.dodian.uber.game.skill.slayer.SlayerMaskItems
import net.dodian.uber.game.skill.prayer.BuryBonesItems
import net.dodian.uber.game.skill.thieving.ThievingObjectComponents
import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.api.plugin.skills.SkillPluginSnapshot
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
    private val cookingRangeObjectIds = intArrayOf(26181, 2728, 2781)

    @JvmStatic
    fun snapshot(): SkillDoctorReport {
        return snapshot(registrySnapshot = null)
    }

    internal fun snapshot(registrySnapshot: SkillPluginSnapshot?): SkillDoctorReport {
        val findings = mutableListOf<SkillDoctorFinding>()
        val activeSnapshot = registrySnapshot

        try {
            activeSnapshot ?: PluginRegistry.currentSkills()
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
        findings += scanMappedRouteOwnership(activeSnapshot ?: PluginRegistry.currentSkills())
        findings += ContentParityDoctor.scan(skillSnapshot = activeSnapshot ?: PluginRegistry.currentSkills())

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
        val skillsRoot = sourceRoot.resolve("net/dodian/uber/game/skill")
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
        val skillsRoot = sourceRoot.resolve("net/dodian/uber/game/skill")
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
                                    message = "$path has $callName(...) without explicit preset = PolicyPreset.<NAME>",
                                )
                            } else {
                                val presetMatch = Regex("""preset\s*=\s*PolicyPreset\.([A-Z_]+)""").find(args)
                                val presetName = presetMatch?.groupValues?.get(1)
                                if (presetName == null || presetName !in validPresetNames) {
                                    findings += SkillDoctorFinding(
                                        code = "invalid-policy-preset",
                                        message = "$path has invalid policy preset on $callName(...): ${presetName ?: "unknown"}",
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

        BuryBonesItems.itemIds.forEach { itemId ->
            if (skillSnapshot.itemBinding(option = 1, itemId = itemId) == null) {
                findings += SkillDoctorFinding(
                    code = "missing-route-ownership",
                    message = "Prayer route ownership missing bone click option=1 binding for itemId=$itemId.",
                )
            }
            if (skillSnapshot.itemOnObjectBinding(objectId = 409, itemId = itemId) == null) {
                findings += SkillDoctorFinding(
                    code = "missing-route-ownership",
                    message = "Prayer route ownership missing altar item-on-object binding for objectId=409 itemId=$itemId.",
                )
            }
        }

        cookingRangeObjectIds.forEach { objectId ->
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

        val thievingOptionOneObjectIds =
            (ThievingObjectComponents.chestObjects + ThievingObjectComponents.plunderObjects).distinct()
        thievingOptionOneObjectIds.forEach { objectId ->
            if (skillSnapshot.objectBinding(option = 1, objectId = objectId) == null) {
                findings += SkillDoctorFinding(
                    code = "missing-route-ownership",
                    message = "Thieving route ownership missing object click option=1 binding for objectId=$objectId.",
                )
            }
        }

        val thievingOptionTwoObjectIds =
            (
                ThievingObjectComponents.stallObjects +
                    ThievingObjectComponents.chestObjects +
                    ThievingObjectComponents.plunderObjects
            ).distinct()
        thievingOptionTwoObjectIds.forEach { objectId ->
            if (skillSnapshot.objectBinding(option = 2, objectId = objectId) == null) {
                findings += SkillDoctorFinding(
                    code = "missing-route-ownership",
                    message = "Thieving route ownership missing object click option=2 binding for objectId=$objectId.",
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
        magicOnObjectBindings.isNotEmpty() ||
        buttonBindings.isNotEmpty()
}
