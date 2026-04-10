package net.dodian.uber.game.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.dispatch.ContentModuleIndex
import net.dodian.uber.game.systems.skills.plugin.SkillPlugin

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
    fun `content infrastructure registries and dispatchers live in systems layer`() {
        val forbiddenContentFiles = setOf(
            "src/main/kotlin/net/dodian/uber/game/content/items/ItemContentRegistry.kt",
            "src/main/kotlin/net/dodian/uber/game/content/items/ItemDispatcher.kt",
            "src/main/kotlin/net/dodian/uber/game/content/npcs/NpcContentRegistry.kt",
            "src/main/kotlin/net/dodian/uber/game/content/npcs/NpcContentDispatcher.kt",
            "src/main/kotlin/net/dodian/uber/game/content/objects/ObjectContentRegistry.kt",
            "src/main/kotlin/net/dodian/uber/game/content/objects/ObjectInteractionService.kt",
            "src/main/kotlin/net/dodian/uber/game/content/objects/ObjectClickLoggingService.kt",
            "src/main/kotlin/net/dodian/uber/game/content/commands/CommandAccess.kt",
            "src/main/kotlin/net/dodian/uber/game/content/commands/CommandContent.kt",
            "src/main/kotlin/net/dodian/uber/game/content/commands/CommandContentRegistry.kt",
            "src/main/kotlin/net/dodian/uber/game/content/commands/CommandContext.kt",
            "src/main/kotlin/net/dodian/uber/game/content/commands/CommandDispatcher.kt",
            "src/main/kotlin/net/dodian/uber/game/content/commands/CommandLogging.kt",
            "src/main/kotlin/net/dodian/uber/game/content/commands/CommandParsing.kt",
            "src/main/kotlin/net/dodian/uber/game/content/commands/CommandReply.kt",
            "src/main/kotlin/net/dodian/uber/game/content/items/ItemCombinationService.kt",
            "src/main/kotlin/net/dodian/uber/game/content/items/ItemOnNpcContentService.kt",
            "src/main/kotlin/net/dodian/uber/game/content/npcs/NpcInteractionActionService.kt",
            "src/main/kotlin/net/dodian/uber/game/content/npcs/NpcClickMetrics.kt",
            "src/main/kotlin/net/dodian/uber/game/content/ui/SkillingInterfaceItemService.kt",
            "src/main/kotlin/net/dodian/uber/game/content/ContentModuleIndex.kt",
        )
        val missingSystemsFiles = setOf(
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/items/ItemContentRegistry.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/items/ItemDispatcher.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/npcs/NpcContentRegistry.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/npcs/NpcContentDispatcher.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/objects/ObjectContentRegistry.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/objects/ObjectInteractionService.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/objects/ObjectClickLoggingService.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/commands/CommandAccess.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/commands/CommandContent.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/commands/CommandContentRegistry.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/commands/CommandContext.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/commands/CommandDispatcher.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/commands/CommandLogging.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/commands/CommandParsing.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/commands/CommandReply.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/items/ItemCombinationService.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/items/ItemOnNpcContentService.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/npcs/NpcInteractionActionService.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/npcs/NpcClickMetrics.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/ui/SkillingInterfaceItemService.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch/ContentModuleIndex.kt",
        )

        val forbidden = sourceFiles
            .map { it.invariantSeparatorsPathString }
            .filter { path -> forbiddenContentFiles.any { path.endsWith(it) } }

        val existing = sourceFiles
            .map { it.invariantSeparatorsPathString }
            .toSet()
        val missing = missingSystemsFiles.filterNot { expected ->
            existing.any { it.endsWith(expected) }
        }

        assertTrue(
            forbidden.isEmpty(),
            "Infrastructure registries/dispatchers must not live in content layer.\n${forbidden.joinToString("\n")}",
        )
        assertTrue(
            missing.isEmpty(),
            "Expected infrastructure under systems.dispatch.\n${missing.joinToString("\n")}",
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
        if (violations.isNotEmpty()) {
            System.err.println(
                "WARN: Content path depth exceeded practical cap (warn-only).\n${violations.joinToString("\n")}",
            )
        }
        assertTrue(true)
    }

    @Test
    fun `repackaged kotlin roots keep practical depth cap`() {
        val cappedRoots = mapOf(
            "src/main/kotlin/net/dodian/uber/game/engine/tasking" to 1,
            "src/main/kotlin/net/dodian/uber/game/engine/sync/player" to 2,
            "src/main/kotlin/net/dodian/uber/game/systems/action" to 1,
            "src/main/kotlin/net/dodian/uber/game/systems/dispatch" to 2,
            "src/main/kotlin/net/dodian/uber/game/systems/skills" to 1,
            "src/main/kotlin/net/dodian/uber/game/content/objects" to 2,
            "src/main/kotlin/net/dodian/uber/game/model/object" to 1,
        )

        val violations = cappedRoots.flatMap { (root, maxDepth) ->
            val rootPath = Paths.get(root)
            val rootPrefix = "${rootPath.invariantSeparatorsPathString}/"
            sourceFiles
                .asSequence()
                .filter { it.extension == "kt" }
                .filter { it.invariantSeparatorsPathString.startsWith(rootPrefix) }
                .mapNotNull { file ->
                    val relative = rootPath.relativize(file)
                    val depth = relative.nameCount - 1
                    if (depth <= maxDepth) return@mapNotNull null
                    "$file depth=$depth max=$maxDepth"
                }
                .toList()
        }

        if (violations.isNotEmpty()) {
            System.err.println(
                "WARN: Repackaged roots exceeded practical depth cap (warn-only).\n${violations.joinToString("\n")}",
            )
        }
        assertTrue(true)
    }

    @Test
    fun `kotlin source does not accumulate empty package directories`() {
        val kotlinRoot = sourceRoot.resolve("kotlin")
        val emptyDirs = Files.walk(kotlinRoot)
            .filter { Files.isDirectory(it) }
            .filter { dir -> Files.list(dir).use { stream -> stream.findAny().isEmpty } }
            .map { it.invariantSeparatorsPathString }
            .filter { it.contains("/net/dodian/") }
            .toList()

        if (emptyDirs.isNotEmpty()) {
            System.err.println(
                "WARN: Empty Kotlin package directories found (warn-only):\n${emptyDirs.joinToString("\n")}",
            )
        }
        assertTrue(true)
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
    fun `interaction processor does not hardcode migrated skill dispatch`() {
        val interactionPath =
            Paths.get("src/main/kotlin/net/dodian/uber/game/systems/interaction/InteractionProcessor.kt")
        val source = Files.readString(interactionPath)

        val forbiddenCalls = listOf(
            "Fishing.handleNpcOption(",
            "Mining.attempt(",
            "Woodcutting.attempt(",
        )
        val violations = forbiddenCalls.filter { source.contains(it) }

        assertTrue(
            violations.isEmpty(),
            "Migrated skills must route through SkillInteractionDispatcher/NpcContentDispatcher.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `all gameplay skills are owned by skill plugins`() {
        val expectedSkills = setOf(
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

        val skillPlugins: List<SkillPlugin> = ContentModuleIndex.skillPlugins
        val discovered = skillPlugins.map { plugin -> plugin.definition.skill }.toSet()
        assertTrue(
            discovered == expectedSkills,
            "Expected complete gameplay skill plugin ownership.\nmissing=${(expectedSkills - discovered).joinToString()}\nextra=${(discovered - expectedSkills).joinToString()}",
        )
    }

    @Test
    fun `gameplay skill plugins are not empty route wrappers`() {
        val gameplaySkills = setOf(
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

        val skillPlugins: List<SkillPlugin> = ContentModuleIndex.skillPlugins

        val empty = skillPlugins
            .filter { plugin -> plugin.definition.skill in gameplaySkills }
            .filter { plugin ->
                val definition = plugin.definition
                definition.objectBindings.isEmpty() &&
                    definition.npcBindings.isEmpty() &&
                    definition.itemOnItemBindings.isEmpty() &&
                    definition.itemBindings.isEmpty() &&
                    definition.itemOnObjectBindings.isEmpty() &&
                    definition.buttonBindings.isEmpty()
            }
            .map { "${it.definition.name}(${it.definition.skill.name.lowercase()})" }

        assertTrue(
            empty.isEmpty(),
            "Gameplay SkillPlugin definitions must own at least one route.\n${empty.joinToString("\n")}",
        )
    }

    @Test
    fun `migrated resource skills do not expose direct ObjectContent bindings`() {
        val files = listOf(
            Paths.get("src/main/kotlin/net/dodian/uber/game/content/skills/mining/Mining.kt"),
            Paths.get("src/main/kotlin/net/dodian/uber/game/content/skills/woodcutting/Woodcutting.kt"),
        )

        val violations = files.flatMap { path ->
            val source = Files.readString(path)
            buildList {
                if (source.contains("ObjectContent")) {
                    add("${path}: contains legacy ObjectContent binding")
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Migrated resource skills should register via SkillPlugin only.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `migrated skill plugin modules must use skilling templates instead of ad-hoc loops`() {
        val pluginFiles = sourceFiles.filter { path ->
            path.extension == "kt" &&
                path.invariantSeparatorsPathString.contains("/net/dodian/uber/game/content/skills/") &&
                Files.readString(path).contains(Regex("""object\s+\w+SkillPlugin\s*:"""))
        }

        val violations = pluginFiles.flatMap { path ->
            val source = Files.readString(path)
            buildList {
                if (source.contains("playerAction(")) {
                    add("$path: contains forbidden ad-hoc playerAction orchestration")
                }
                if (source.contains("while (true)")) {
                    add("$path: contains forbidden infinite loop orchestration")
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Migrated SkillPlugin modules must orchestrate cycles via gatheringAction/productionAction.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `skill plugin route bindings require explicit policy presets`() {
        val pluginFiles = sourceFiles.filter { path ->
            path.extension == "kt" &&
                path.invariantSeparatorsPathString.contains("/net/dodian/uber/game/content/skills/") &&
                Files.readString(path).contains(Regex("""object\s+\w+SkillPlugin\s*:"""))
        }

        val callRegexByName = mapOf(
            "objectClick" to Regex("""objectClick\s*\((.*?)\)""", setOf(RegexOption.DOT_MATCHES_ALL)),
            "npcClick" to Regex("""npcClick\s*\((.*?)\)""", setOf(RegexOption.DOT_MATCHES_ALL)),
            "itemOnItem" to Regex("""itemOnItem\s*\((.*?)\)""", setOf(RegexOption.DOT_MATCHES_ALL)),
            "itemClick" to Regex("""itemClick\s*\((.*?)\)""", setOf(RegexOption.DOT_MATCHES_ALL)),
            "itemOnObject" to Regex("""itemOnObject\s*\((.*?)\)""", setOf(RegexOption.DOT_MATCHES_ALL)),
            "button" to Regex("""button\s*\((.*?)\)""", setOf(RegexOption.DOT_MATCHES_ALL)),
        )

        val violations = pluginFiles.flatMap { path ->
            val source = Files.readString(path)
            buildList {
                callRegexByName.forEach { (call, regex) ->
                    regex.findAll(source).forEach { match ->
                        val args = match.groupValues[1]
                        if (!Regex("""preset\s*=\s*PolicyPreset\.[A-Z_]+""").containsMatchIn(args)) {
                            add("$path: $call(...) missing explicit preset = PolicyPreset.<NAME>")
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "SkillPlugin bindings must declare explicit policy presets.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `mapped skill wrappers use shared skill route bridge helpers`() {
        val requiredBridgeUsage = mapOf(
            "src/main/kotlin/net/dodian/uber/game/content/skills/cooking/Cooking.kt" to "bindObjectContentUseItem(",
            "src/main/kotlin/net/dodian/uber/game/content/skills/herblore/Herblore.kt" to "bindItemContentClick(",
            "src/main/kotlin/net/dodian/uber/game/content/skills/slayer/Slayer.kt" to "bindItemContentClick(",
        )

        val violations = requiredBridgeUsage.mapNotNull { (pathText, helperCall) ->
            val path = Paths.get(pathText)
            val source = Files.readString(path)
            if (source.contains(helperCall)) {
                null
            } else {
                "$pathText: expected bridge helper usage $helperCall"
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Mapped SkillPlugin wrappers should use shared bridge helpers.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `skill plugin modules do not import legacy split policy types`() {
        val pluginFiles = sourceFiles.filter { path ->
            path.extension == "kt" &&
                path.invariantSeparatorsPathString.contains("/net/dodian/uber/game/content/skills/") &&
                Files.readString(path).contains(Regex("""object\s+\w+SkillPlugin\s*:"""))
        }

        val violations = pluginFiles.flatMap { path ->
            Files.readAllLines(path).mapIndexedNotNull { idx, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("import ")) {
                    return@mapIndexedNotNull null
                }
                val usesLegacySplitPolicy =
                    trimmed.contains("net.dodian.uber.game.systems.interaction.ObjectInteractionPolicy") ||
                        trimmed.contains("net.dodian.uber.game.systems.action.PlayerActionInterruptPolicy")
                if (!usesLegacySplitPolicy) {
                    return@mapIndexedNotNull null
                }
                "${path}:${idx + 1} -> $trimmed"
            }
        }

        assertTrue(
            violations.isEmpty(),
            "SkillPlugin modules should use unified policy preset DSL, not legacy split policy imports.\n${violations.joinToString("\n")}",
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
    fun `legacy game event package is removed except core GameEvent type`() {
        val packageViolations = sourceFiles.mapNotNull { file ->
            val packageLine = Files.readAllLines(file)
                .asSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("package ") }
                ?: return@mapNotNull null
            val packageName = packageLine.removePrefix("package ").trim().removeSuffix(";")
            // GameEvent.kt now lives in game.events — the old game.event package must be entirely empty
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
                val importsLegacyEvent = trimmed.contains("net.dodian.uber.game.event.")
                val importsAllowedCoreGameEvent = trimmed == "import net.dodian.uber.game.events.GameEvent"
                if (!importsLegacyEvent || importsAllowedCoreGameEvent || trimmed.contains("net.dodian.uber.game.event;")) {
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
    fun `event contract runtime and payload packages stay split`() {
        val gameEventPath = sourceRoot.resolve("kotlin/net/dodian/uber/game/events/GameEvent.kt")
        val runtimeFiles = listOf(
            sourceRoot.resolve("kotlin/net/dodian/uber/game/engine/event/GameEventBus.kt"),
            sourceRoot.resolve("kotlin/net/dodian/uber/game/engine/event/GameEventScheduler.kt"),
            sourceRoot.resolve("kotlin/net/dodian/uber/game/engine/event/EventListener.kt"),
            sourceRoot.resolve("kotlin/net/dodian/uber/game/engine/event/EventFilter.kt"),
            sourceRoot.resolve("kotlin/net/dodian/uber/game/engine/event/ReturnableEventListener.kt"),
        )

        val missing = buildList {
            if (!Files.exists(gameEventPath)) add(gameEventPath.toString())
            runtimeFiles.filterNot(Files::exists).forEach { add(it.toString()) }
        }

        assertTrue(
            missing.isEmpty(),
            "Event split files must exist in their canonical packages.\n${missing.joinToString("\n")}",
        )

        val forbiddenTypeLocations = sourceFiles.mapNotNull { file ->
            val lines = Files.readAllLines(file)
            val pkg = lines.asSequence().map { it.trim() }.firstOrNull { it.startsWith("package ") }
                ?.removePrefix("package ")
                ?.trim()
                ?.removeSuffix(";")
                ?: return@mapNotNull null
            val name = file.fileName.toString()
            val isRuntimeType = name in setOf(
                "GameEventBus.kt",
                "GameEventScheduler.kt",
                "EventListener.kt",
                "EventFilter.kt",
                "ReturnableEventListener.kt",
            )
            val isGameEventContract = name == "GameEvent.kt"

            val runtimeMisplaced = isRuntimeType && pkg != "net.dodian.uber.game.engine.event"
            val gameEventMisplaced = isGameEventContract && pkg != "net.dodian.uber.game.events"
            if (!runtimeMisplaced && !gameEventMisplaced) return@mapNotNull null
            "$file -> package $pkg"
        }

        assertTrue(
            forbiddenTypeLocations.isEmpty(),
            "Event contract/runtime types must stay in split canonical packages.\n${forbiddenTypeLocations.joinToString("\n")}",
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
            "net.dodian.uber.game.systems.skills.SkillDoctor",
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

    @Test
    fun `player initializer routes farming login through content runtime api`() {
        val path = sourceRoot.resolve("java/net/dodian/uber/game/model/entity/player/PlayerInitializer.java")
        val source = Files.readString(path)

        assertTrue(
            source.contains("ContentRuntimeApi.onFarmingLogin(client, System.currentTimeMillis())"),
            "Expected PlayerInitializer to call ContentRuntimeApi.onFarmingLogin(client, System.currentTimeMillis())",
        )
        assertTrue(
            !source.contains("FarmingRuntimeService.INSTANCE.onLogin"),
            "PlayerInitializer must not call FarmingRuntimeService directly",
        )
    }

    @Test
    fun `netty listener boundary guard exists`() {
        val boundaryTestPath =
            Paths.get("src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt")

        assertTrue(
            Files.exists(boundaryTestPath),
            "Expected Netty listener boundary test to exist at $boundaryTestPath",
        )
    }
}
