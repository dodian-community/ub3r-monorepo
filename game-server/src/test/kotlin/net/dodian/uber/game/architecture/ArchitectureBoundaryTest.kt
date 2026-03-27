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
        val temporaryAllowListByFile = mapOf(
            "src/main/kotlin/net/dodian/uber/game/content/commands/dev/DevDebugCommands.kt" to setOf(
                "import net.dodian.uber.game.engine.tasking.coroutine.gameClock",
                "import net.dodian.uber.game.engine.tasking.coroutine.npcTaskCoroutine",
                "import net.dodian.uber.game.engine.tasking.coroutine.playerTaskCoroutine",
                "import net.dodian.uber.game.engine.tasking.coroutine.worldTaskCoroutine",
            ),
            "src/main/kotlin/net/dodian/uber/game/content/objects/services/PersonalObjectService.kt" to setOf(
                "import net.dodian.uber.game.engine.scheduler.QueueTask",
                "import net.dodian.uber.game.engine.scheduler.QueueTaskService",
            ),
            "src/main/kotlin/net/dodian/uber/game/content/skills/agility/AgilityCourseService.kt" to setOf(
                "import net.dodian.uber.game.engine.loop.GameThreadTimers",
            ),
            "src/main/kotlin/net/dodian/uber/game/content/skills/core/runtime/GatheringTask.kt" to setOf(
                "import net.dodian.uber.game.engine.scheduler.QueueTaskHandle",
                "import net.dodian.uber.game.engine.tasking.GameTaskRuntime",
                "import net.dodian.uber.game.engine.tasking.TaskPriority",
            ),
            "src/main/kotlin/net/dodian/uber/game/content/skills/mining/MiningService.kt" to setOf(
                "import net.dodian.uber.game.engine.loop.GameCycleClock",
            ),
            "src/main/kotlin/net/dodian/uber/game/content/skills/woodcutting/WoodcuttingService.kt" to setOf(
                "import net.dodian.uber.game.engine.loop.GameCycleClock",
            ),
        )
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
            "Content must not import engine internals (except explicit temporary allow-list).\n${violations.joinToString("\n")}",
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
}
