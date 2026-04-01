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
        val violations = sourceFiles
            .filter { it.toString().contains("/net/dodian/uber/game/content/") }
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
            "Content must not import engine internals.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `content layer does not import netty or sql APIs`() {
        val violations = sourceFiles
            .filter { it.toString().contains("/net/dodian/uber/game/content/") }
            .flatMap { file ->
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null
                    val forbidden =
                        trimmed.contains("import io.netty") ||
                            trimmed.contains("import java.sql") ||
                            trimmed.contains("import javax.sql")
                    if (!forbidden) return@mapIndexedNotNull null
                    "${file}:${idx + 1} -> $trimmed"
                }
            }
        assertTrue(
            violations.isEmpty(),
            "Content must not import netty/sql APIs.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `content layer depth is capped during migration`() {
        val contentRoot = sourceRoot.resolve("kotlin/net/dodian/uber/game/content")
        val violations = sourceFiles
            .filter { it.toString().contains("/net/dodian/uber/game/content/") }
            .mapNotNull { file ->
                if (!file.toString().endsWith(".kt")) return@mapNotNull null
                val relative = contentRoot.relativize(file)
                val depth = relative.nameCount - 1
                if (depth <= 3) return@mapNotNull null
                "$file depth=$depth"
            }
        assertTrue(
            violations.isEmpty(),
            "Content path depth must stay <= 3 until flattening phases are complete.\n${violations.joinToString("\n")}",
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
        val removedSkillSymbols = setOf("SkillWIP", "skillById(", "skillByName(", "skillsEnabled(")

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
                    normalized.endsWith("/net/dodian/uber/game/SkillWIP.kt") ||
                    normalized.endsWith("/net/dodian/utilities/Database.kt") ||
                    normalized.endsWith("/net/dodian/utilities/DatabaseConfig.kt") ||
                    normalized.endsWith("/net/dodian/utilities/DatabaseInitializer.kt") ||
                    normalized.endsWith("/net/dodian/utilities/DotEnv.kt")
            if (!isLegacyPath) return@mapNotNull null
            normalized
        }

        val legacyReferenceViolations = sourceFiles.flatMap { file ->
            val normalized = file.invariantSeparatorsPathString
            Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                val trimmed = line.trim()
                val isLegacyLoopMarker =
                    (normalized.endsWith("/content/skills/woodcutting/WoodcuttingService.kt") ||
                        normalized.endsWith("/content/skills/mining/MiningService.kt")) &&
                        (trimmed.contains("nextSwingAnimationCycle") ||
                            trimmed.contains("nextResourceCycle") ||
                            trimmed.contains("PlayerActionController.start("))
                val isWave2LegacyLoopMarker =
                    normalized.endsWith("/systems/action/SkillingActionService.kt") &&
                        (trimmed.contains("type = PlayerActionType.FISHING") ||
                            trimmed.contains("type = PlayerActionType.FLETCHING") ||
                            trimmed.contains("type = PlayerActionType.COOKING"))
                val isLegacyPlayerArrayAccess =
                    trimmed.contains("PlayerHandler.players[") &&
                        !normalized.endsWith("/model/entity/player/Client.java")
                val isHardCutLegacyNaming =
                    trimmed.contains("PlayerHandler") ||
                        trimmed.contains("ShopHandler") ||
                        trimmed.contains("DoorHandler")
                val isLegacyFrameApiUsage =
                    (
                        trimmed.contains("sendFrame164(") ||
                            trimmed.contains("sendFrame200(") ||
                            trimmed.contains("sendFrame246(")
                        ) &&
                        !normalized.endsWith("/model/entity/player/Client.java")
                val isLegacyClientItemHelperUsage =
                    (
                        trimmed.contains("GetItemName(") ||
                            trimmed.contains("GetItemSlot(") ||
                            trimmed.contains("IsItemInBag(") ||
                            trimmed.contains("AreXItemsInBag(") ||
                            trimmed.contains("GetNotedItem(") ||
                            trimmed.contains("GetUnnotedItem(")
                        ) &&
                        !normalized.endsWith("/model/entity/player/Client.java")
                val isManualCoreSkillControllerMarker =
                    (normalized.endsWith("/systems/action/SmithingActionService.kt") ||
                        normalized.endsWith("/content/skills/smithing/SmeltingActionService.kt")) &&
                        trimmed.contains("PlayerActionController.start(")
                val isRemovedInteractionRuntimeSymbol =
                    trimmed.contains("WalkToTask") ||
                        trimmed.contains("walkToTask")
                val isRemovedLegacyActionTimerSymbol =
                    trimmed.contains("actionTimer")
                val isRemovedPlayerTickPosting =
                    trimmed.contains("post(PlayerTickEvent(") ||
                        trimmed.contains("GameEventBus.post(PlayerTickEvent(")
                val isLegacyRef =
                    trimmed.contains("net.dodian.jobs.") ||
                        trimmed.contains("net.dodian.uber.game.skills.farming.FarmingProcessor") ||
                        trimmed.contains("net.dodian.uber.game.skills.thieving.plunder.PlunderDoorProcessor") ||
                        trimmed.contains("net.dodian.utilities.DatabaseKt") ||
                        trimmed.contains("net.dodian.utilities.DatabaseInitializerKt") ||
                        trimmed.contains("net.dodian.utilities.DotEnvKt") ||
                        removedSkillSymbols.any { symbol ->
                            trimmed.contains(symbol)
                        } ||
                        isLegacyLoopMarker ||
                        removedNpcManagerSymbols.any { symbol ->
                            trimmed.contains(symbol)
                        } ||
                        removedToggleSymbols.any { symbol ->
                            trimmed.contains("import net.dodian.uber.game.config.$symbol") ||
                                trimmed.contains("import static net.dodian.uber.game.config.DotEnvKt.get${symbol.replaceFirstChar { c -> c.uppercaseChar() }}")
                        } ||
                        isWave2LegacyLoopMarker ||
                        isLegacyPlayerArrayAccess ||
                        isHardCutLegacyNaming ||
                        isLegacyFrameApiUsage ||
                        isLegacyClientItemHelperUsage
                        || isManualCoreSkillControllerMarker ||
                        isRemovedInteractionRuntimeSymbol ||
                        isRemovedLegacyActionTimerSymbol ||
                        isRemovedPlayerTickPosting
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

    @Test
    fun `plugin index generation uses top-level ksp processor`() {
        val repoRoot = Paths.get("..").normalize().toAbsolutePath()
        val rootSettings = repoRoot.resolve("settings.gradle.kts")
        val serverBuild = repoRoot.resolve("game-server/build.gradle.kts")
        val legacyModuleDir = repoRoot.resolve("game-plugin-index-processor")
        val generatedIndexSource = repoRoot.resolve("game-server/src/main/kotlin/net/dodian/uber/game/plugin/GeneratedPluginModuleIndex.kt")

        val settingsText = Files.readString(rootSettings)
        val serverBuildText = Files.readString(serverBuild)

        val violations = mutableListOf<String>()
        if (!settingsText.contains("include(\":ksp-processor\")")) {
            violations += "settings.gradle.kts must include :ksp-processor"
        }
        if (settingsText.contains("include(\":game-plugin-index-processor\")")) {
            violations += "settings.gradle.kts must not include :game-plugin-index-processor"
        }
        if (!serverBuildText.contains("id(\"com.google.devtools.ksp\")")) {
            violations += "game-server/build.gradle.kts must apply com.google.devtools.ksp"
        }
        if (!serverBuildText.contains("ksp(project(\":ksp-processor\"))")) {
            violations += "game-server/build.gradle.kts must depend on ksp(project(\":ksp-processor\"))"
        }
        if (serverBuildText.contains("generatePluginModuleIndex")) {
            violations += "legacy JavaExec generatePluginModuleIndex task must be removed"
        }
        if (Files.exists(legacyModuleDir)) {
            violations += "legacy module directory game-plugin-index-processor must not exist"
        }
        if (Files.exists(generatedIndexSource)) {
            violations += "GeneratedPluginModuleIndex.kt must not be hand-maintained under src/main"
        }

        assertTrue(
            violations.isEmpty(),
            "Plugin index generation must be KSP-driven.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `intellij tools module is not wired in repo`() {
        val repoRoot = Paths.get("..").normalize().toAbsolutePath()
        val rootSettings = repoRoot.resolve("settings.gradle.kts")
        val moduleDir = repoRoot.resolve("ub3r-intellij-tools")
        val settingsText = Files.readString(rootSettings)
        val violations = mutableListOf<String>()
        if (settingsText.contains("include(\":ub3r-intellij-tools\")")) {
            violations += "settings.gradle.kts must not include :ub3r-intellij-tools"
        }
        if (Files.exists(moduleDir)) {
            violations += "ub3r-intellij-tools directory must not exist"
        }
        assertTrue(
            violations.isEmpty(),
            "IntelliJ tools rollback must be complete.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `combat preemption is centralized in cancellation service`() {
        val cancellationService = sourceRoot.resolve("kotlin/net/dodian/uber/game/systems/action/PlayerActionCancellationService.kt")
        val interactionScheduler = sourceRoot.resolve("kotlin/net/dodian/uber/game/systems/interaction/scheduler/InteractionTaskScheduler.kt")

        val cancellationSource = Files.readString(cancellationService)
        val schedulerSource = Files.readString(interactionScheduler)
        val violations = mutableListOf<String>()

        if (!cancellationSource.contains("CombatPreemptionPolicy.preemptCombatIfNeeded(player, reason)")) {
            violations += "PlayerActionCancellationService must call CombatPreemptionPolicy.preemptCombatIfNeeded"
        }
        if (!schedulerSource.contains("if (intent.option == 5)")) {
            violations += "InteractionTaskScheduler must preserve NPC attack option behavior when choosing cancel reason"
        }

        assertTrue(
            violations.isEmpty(),
            "Combat preemption routing must stay centralized and attack-safe.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `engine loop and netty listeners avoid direct database access`() {
        val violations = sourceFiles
            .filter { file ->
                val normalized = file.invariantSeparatorsPathString
                normalized.contains("/net/dodian/uber/game/engine/loop/") ||
                    normalized.contains("/net/dodian/uber/game/engine/processing/") ||
                    normalized.contains("/net/dodian/uber/game/netty/listener/")
            }
            .flatMap { file ->
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    if (
                        trimmed.contains("getDbConnection(") ||
                        trimmed.contains("dbConnection")
                    ) {
                        "${file}:${idx + 1} -> $trimmed"
                    } else {
                        null
                    }
                }
            }

        assertTrue(
            violations.isEmpty(),
            "Hot paths must not access database directly.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `legacy game config package is removed`() {
        val packageViolations = sourceFiles.mapNotNull { file ->
            val packageLine = Files.readAllLines(file)
                .asSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("package ") }
                ?: return@mapNotNull null
            val packageName = packageLine.removePrefix("package ").trim().removeSuffix(";")
            if (!packageName.startsWith("net.dodian.uber.game.config")) {
                return@mapNotNull null
            }
            "${file} -> $packageName"
        }

        val importViolations = sourceFiles.flatMap { file ->
            Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("import ")) {
                    return@mapIndexedNotNull null
                }
                if (!trimmed.contains("net.dodian.uber.game.config")) {
                    return@mapIndexedNotNull null
                }
                "${file}:${idx + 1} -> $trimmed"
            }
        }

        val violations = packageViolations + importViolations
        assertTrue(
            violations.isEmpty(),
            "Legacy config namespace must not be used.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `legacy skill core events package is removed`() {
        val violations = sourceFiles.flatMap { file ->
            Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                val trimmed = line.trim()
                val referencesLegacySkillEvents = trimmed.contains("net.dodian.uber.game.content.skills.core.events")
                if (!referencesLegacySkillEvents) {
                    return@mapIndexedNotNull null
                }
                "${file}:${idx + 1} -> $trimmed"
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Legacy skill event package must not be used.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `low-level event listener internals are infra-only`() {
        val violations = sourceFiles.flatMap { file ->
            val normalized = file.invariantSeparatorsPathString
            val isInfraFile =
                normalized.contains("/net/dodian/uber/game/engine/event/")
            Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                val trimmed = line.trim()
                val usesLowLevelListenerInternals =
                    trimmed.contains("import net.dodian.uber.game.engine.event.EventListener") ||
                        trimmed.contains("import net.dodian.uber.game.engine.event.EventFilter") ||
                        trimmed.contains("import net.dodian.uber.game.engine.event.ReturnableEventListener")
                if (!usesLowLevelListenerInternals || isInfraFile) {
                    return@mapIndexedNotNull null
                }
                "${file}:${idx + 1} -> $trimmed"
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Event listener/filter internals should stay inside event infrastructure packages.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `legacy game event package is removed`() {
        val packageViolations = sourceFiles.mapNotNull { file ->
            val packageLine = Files.readAllLines(file)
                .asSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("package ") }
                ?: return@mapNotNull null
            val packageName = packageLine.removePrefix("package ").trim().removeSuffix(";")
            val isLegacyEventPackage =
                packageName == "net.dodian.uber.game.event" ||
                    packageName.startsWith("net.dodian.uber.game.event.")
            if (!isLegacyEventPackage) {
                return@mapNotNull null
            }
            "${file} -> $packageName"
        }

        val importViolations = sourceFiles.flatMap { file ->
            Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("import ")) {
                    return@mapIndexedNotNull null
                }
                if (
                    !trimmed.contains("net.dodian.uber.game.event.") &&
                    !trimmed.contains("net.dodian.uber.game.event;")
                ) {
                    return@mapIndexedNotNull null
                }
                "${file}:${idx + 1} -> $trimmed"
            }
        }

        val violations = packageViolations + importViolations
        assertTrue(
            violations.isEmpty(),
            "Legacy event namespace must not be used.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `events package stays payload only`() {
        val violations = sourceFiles
            .filter { it.invariantSeparatorsPathString.contains("/net/dodian/uber/game/events/") }
            .flatMap { file ->
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    val hasBusWiringLogic =
                        trimmed.contains("GameEventBus.") ||
                            trimmed.contains("EventListener(") ||
                            trimmed.contains("EventFilter(") ||
                            trimmed.contains("ReturnableEventListener(")
                    if (!hasBusWiringLogic) {
                        return@mapIndexedNotNull null
                    }
                    "${file}:${idx + 1} -> $trimmed"
                }
            }

        assertTrue(
            violations.isEmpty(),
            "game.events should contain payload definitions, not event bus wiring.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `no source imports removed core skills runtime package`() {
        val violations = sourceFiles
            .flatMap { file ->
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) {
                        return@mapIndexedNotNull null
                    }
                    if (!trimmed.contains("net.dodian.uber.game.content.skills.core.runtime")) {
                        return@mapIndexedNotNull null
                    }
                    "${file}:${idx + 1} -> $trimmed"
                }
            }

        assertTrue(
            violations.isEmpty(),
            "Legacy core runtime package must not be imported.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `no source imports removed core skills progression package`() {
        val violations = sourceFiles
            .flatMap { file ->
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) {
                        return@mapIndexedNotNull null
                    }
                    if (!trimmed.contains("net.dodian.uber.game.content.skills.core.progression")) {
                        return@mapIndexedNotNull null
                    }
                    "${file}:${idx + 1} -> $trimmed"
                }
            }

        assertTrue(
            violations.isEmpty(),
            "Legacy core progression package must not be imported.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `no source imports removed core skills resource package`() {
        val violations = sourceFiles
            .flatMap { file ->
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import ")) {
                        return@mapIndexedNotNull null
                    }
                    if (!trimmed.contains("net.dodian.uber.game.content.skills.core.resource")) {
                        return@mapIndexedNotNull null
                    }
                    "${file}:${idx + 1} -> $trimmed"
                }
            }

        assertTrue(
            violations.isEmpty(),
            "Legacy core resource package must not be imported.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `java callers use only approved systems skills wrappers`() {
        val approvedJavaSkillApis = setOf(
            "net.dodian.uber.game.systems.skills.ProgressionService",
            "net.dodian.uber.game.systems.skills.RuneCostService",
            "net.dodian.uber.game.systems.skills.SkillingRandomEventService",
            "net.dodian.uber.game.systems.skills.SkillAdminService",
            "net.dodian.uber.game.systems.skills.SkillReadService",
        )
        val violations = sourceFiles
            .filter { it.invariantSeparatorsPathString.endsWith(".java") }
            .flatMap { file ->
                Files.readAllLines(file).mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("import net.dodian.uber.game.systems.skills.")) {
                        return@mapIndexedNotNull null
                    }
                    val importedType = trimmed.removePrefix("import ").removeSuffix(";").trim()
                    if (importedType in approvedJavaSkillApis) {
                        return@mapIndexedNotNull null
                    }
                    "${file}:${idx + 1} -> $trimmed"
                }
            }

        assertTrue(
            violations.isEmpty(),
            "Java interop must use explicit systems.skills wrapper APIs only.\n${violations.joinToString("\n")}",
        )
    }
}
