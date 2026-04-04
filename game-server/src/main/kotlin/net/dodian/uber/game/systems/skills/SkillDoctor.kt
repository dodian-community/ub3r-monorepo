package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.content.ContentModuleIndex
import net.dodian.uber.game.systems.policy.PolicyPreset
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
    private val gameplaySkills: Set<Skill> = setOf(
        Skill.MINING,
        Skill.WOODCUTTING,
        Skill.FISHING,
        Skill.AGILITY,
        Skill.COOKING,
        Skill.CRAFTING,
        Skill.FARMING,
        Skill.FIREMAKING,
        Skill.FLETCHING,
        Skill.HERBLORE,
        Skill.PRAYER,
        Skill.RUNECRAFTING,
        Skill.SLAYER,
        Skill.SMITHING,
        Skill.THIEVING,
    )

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
        val findings = mutableListOf<SkillDoctorFinding>()

        try {
            SkillPluginRegistry.current()
        } catch (exception: Exception) {
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

        return SkillDoctorReport(findings)
    }

    @JvmStatic
    fun validateOrThrow() {
        val report = snapshot()
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
        val callNames = listOf("objectClick", "npcClick", "itemOnItem", "button")

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
}
