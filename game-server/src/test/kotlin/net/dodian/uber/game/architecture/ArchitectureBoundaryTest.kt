package net.dodian.uber.game.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString

class ArchitectureBoundaryTest {
    private val sourceRoot: Path = Paths.get("src/main")
    private val sourceFiles: List<Path> by lazy {
        Files.walk(sourceRoot)
            .filter { Files.isRegularFile(it) }
            .filter { it.extension == "kt" || it.extension == "java" }
            .toList()
    }

    @Test
    fun `content layer does not import engine sync or net internals`() {
        val temporaryAllowListByFile = emptyMap<String, Set<String>>()
        val violations = sourceFiles
            .filter { it.toString().contains("/net/dodian/uber/game/content/") }
            .flatMap { file ->
                val normalizedPath = file.invariantSeparatorsPathString
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null
                    if (!trimmed.contains("net.dodian.uber.game.engine.")) return@mapIndexedNotNull null
                    if (trimmed in (temporaryAllowListByFile[normalizedPath] ?: emptySet())) return@mapIndexedNotNull null
                    "${file}:${idx + 1} -> $trimmed"
                }
            }
        assertTrue(
            violations.isEmpty(),
            "Content must not import engine internals.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `systems layer does not import engine sync or net internals`() {
        val violations = sourceFiles
            .filter { it.toString().contains("/net/dodian/uber/game/systems/") }
            .flatMap { file ->
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null
                    val forbidden = trimmed.contains("net.dodian.uber.game.engine.sync") ||
                        trimmed.contains("net.dodian.uber.game.engine.net")
                    if (!forbidden) return@mapIndexedNotNull null
                    "${file}:${idx + 1} -> $trimmed"
                }
            }
        assertTrue(
            violations.isEmpty(),
            "Systems must not import engine sync/net internals.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `engine layer does not import persistence`() {
        val violations = sourceFiles
            .filter { file ->
                Files.readAllLines(file).any { line ->
                    val pkg = line.trim()
                    pkg.startsWith("package net.dodian.uber.game.engine.loop") ||
                        pkg.startsWith("package net.dodian.uber.game.engine.phases") ||
                        pkg.startsWith("package net.dodian.uber.game.engine.sync") ||
                        pkg.startsWith("package net.dodian.uber.game.engine.net") ||
                        pkg.startsWith("package net.dodian.uber.game.engine.tasking") ||
                        pkg.startsWith("package net.dodian.uber.game.engine.scheduler") ||
                        pkg.startsWith("package net.dodian.uber.game.engine.metrics")
                }
            }
            .flatMap { file ->
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null
                    if (!trimmed.contains("net.dodian.uber.game.persistence")) return@mapIndexedNotNull null
                    "${file}:${idx + 1} -> $trimmed"
                }
            }
        assertTrue(
            violations.isEmpty(),
            "Engine/runtime must not import persistence directly.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `source file path matches declared package`() {
        val violations = sourceFiles.mapNotNull { file ->
            val lines = Files.readAllLines(file)
            val packageLine = lines
                .asSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("package ") }
                ?: return@mapNotNull null

            val packageName = packageLine
                .removePrefix("package ")
                .trim()
                .removeSuffix(";")
                .replace("`", "")
            val packagePath = packageName.replace('.', '/')
            val filePath = file.invariantSeparatorsPathString
            val expectedPathSuffix = "$packagePath/${file.fileName}"
            if (filePath.endsWith(expectedPathSuffix)) return@mapNotNull null

            "$file -> declared '$packageName' expects '*$expectedPathSuffix'"
        }

        assertTrue(
            violations.isEmpty(),
            "Source file paths must align with declared package paths.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `legacy repackaged namespaces are removed`() {
        val removedToggleSymbols = setOf(
            "gameLoopEnabled",
            "interactionPipelineEnabled",
            "updatePrepEnabled",
            "synchronizationEnabled",
            "syncRootBlockCacheEnabled",
            "syncViewportSnapshotEnabled",
            "syncSkipEmptyNpcPacketEnabled",
            "syncPlayerActivityIndexEnabled",
            "syncSkipEmptyPlayerPacketEnabled",
            "syncPlayerTemplateCacheEnabled",
            "syncScratchBufferReuseEnabled",
            "syncAppearanceCacheEnabled",
            "playerSynchronizationEnabled",
            "syncPlayerRootDiffEnabled",
            "syncPlayerSelfOnlyEnabled",
            "syncPlayerIncrementalBuildEnabled",
            "syncPlayerFullRebuildFallbackEnabled",
            "syncPlayerReasonMetricsEnabled",
            "syncPlayerDesiredLocalsEnabled",
            "syncPlayerAdmissionQueueEnabled",
            "syncPlayerIncrementalAddsEnabled",
            "syncPlayerMovementFragmentCacheEnabled",
            "syncPlayerAllocationLightEnabled",
            "syncPlayerFragmentReuseEnabled",
            "syncPlayerStateValidationEnabled",
            "syncNpcActivityIndexEnabled",
            "farmingSchedulerEnabled",
            "zoneUpdateBatchingEnabled",
            "queueTasksEnabled",
            "opcode248HasExtra14ByteSuffix",
            "clientUiDeltaProcessorEnabled",
            "databaseConnectionProxyEnabled",
            "runtimePhaseTimingEnabled",
            "runtimeCycleLogEnabled",
            "clientUiTraceEnabled",
            "clientPacketTraceEnabled",
            "combatReactionDebugEnabled",
            "buttonTraceEnabled",
            "objectTraceEnabled",
            "smeltingTraceEnabled",
            "inboundOpcodeProfilingEnabled",
            "inboundOpcodeProfilingWarnMs",
        )
        val removedNpcManagerSymbols = setOf(
            "gnomeSpawn",
            "werewolfSpawn",
            "dagaRex",
            "dagaSupreme",
            "REQUIRED_HARDCODED_NPC_DEFINITIONS",
            "REQUIRED_HARDCODED_NPC_NAMES",
            "repairRequiredHardcodedDefinitions",
        )

        val legacyPackageViolations = sourceFiles.mapNotNull { file ->
            val packageLine = Files.readAllLines(file)
                .asSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("package ") }
                ?: return@mapNotNull null
            val packageName = packageLine.removePrefix("package ").trim().removeSuffix(";")
            val fileName = file.fileName.toString()
            val isLegacy =
                packageName.startsWith("net.dodian.uber.game.content.entities") ||
                    packageName.startsWith("net.dodian.uber.game.systems.ui.interfaces") ||
                    packageName.startsWith("net.dodian.uber.game.systems.ui.dialogue.modules") ||
                    (packageName == "net.dodian.uber.game.skills.farming" && fileName == "FarmingProcessor.kt") ||
                    (packageName == "net.dodian.uber.game.skills.thieving.plunder" && fileName == "PlunderDoorProcessor.kt") ||
                    packageName.startsWith("net.dodian.jobs") ||
                    (packageName == "net.dodian.utilities" && (
                        fileName == "Database.kt" ||
                            fileName == "DatabaseConfig.kt" ||
                            fileName == "DatabaseInitializer.kt" ||
                            fileName == "DotEnv.kt"
                        ))
            if (!isLegacy) return@mapNotNull null
            "${file} -> $packageName"
        }

        val legacyPathViolations = sourceFiles.mapNotNull { file ->
            val normalized = file.invariantSeparatorsPathString
            val isLegacyPath =
                    normalized.contains("/net/dodian/uber/game/content/entities/") ||
                    normalized.contains("/net/dodian/uber/game/systems/ui/interfaces/") ||
                    normalized.contains("/net/dodian/uber/game/systems/ui/dialogue/modules/") ||
                    normalized.endsWith("/net/dodian/uber/game/skills/farming/FarmingProcessor.kt") ||
                    normalized.endsWith("/net/dodian/uber/game/skills/thieving/plunder/PlunderDoorProcessor.kt") ||
                    normalized.contains("src/main/java/net/dodian/jobs/") ||
                    normalized.endsWith("/net/dodian/utilities/Database.kt") ||
                    normalized.endsWith("/net/dodian/utilities/DatabaseConfig.kt") ||
                    normalized.endsWith("/net/dodian/utilities/DatabaseInitializer.kt") ||
                    normalized.endsWith("/net/dodian/utilities/DotEnv.kt")
            if (!isLegacyPath) return@mapNotNull null
            normalized
        }

        val legacyReferenceViolations = sourceFiles.flatMap { file ->
            Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                val trimmed = line.trim()
                val isLegacyRef =
                    trimmed.contains("net.dodian.jobs.") ||
                        trimmed.contains("net.dodian.uber.game.skills.farming.FarmingProcessor") ||
                        trimmed.contains("net.dodian.uber.game.skills.thieving.plunder.PlunderDoorProcessor") ||
                        trimmed.contains("net.dodian.utilities.DatabaseKt") ||
                        trimmed.contains("net.dodian.utilities.DatabaseInitializerKt") ||
                        trimmed.contains("net.dodian.utilities.DotEnvKt") ||
                        removedNpcManagerSymbols.any { symbol ->
                            trimmed.contains(symbol)
                        } ||
                        removedToggleSymbols.any { symbol ->
                            trimmed.contains("import net.dodian.uber.game.config.$symbol") ||
                                trimmed.contains("import static net.dodian.uber.game.config.DotEnvKt.get${symbol.replaceFirstChar { c -> c.uppercaseChar() }}")
                        }
                if (!isLegacyRef) return@mapIndexedNotNull null
                "${file}:${idx + 1} -> $trimmed"
            }
        }

        val violations = legacyPackageViolations + legacyPathViolations + legacyReferenceViolations
        assertTrue(
            violations.isEmpty(),
            "Legacy repackaged namespaces/paths must not remain.\n${violations.joinToString("\n")}",
        )
    }
}
