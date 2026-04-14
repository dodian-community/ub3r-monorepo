package net.dodian.uber.game.model.objects

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldObjectModelTest {
    @Test
    fun `world object stores explicit plane-aware runtime state`() {
        val attachment = Any()
        val objectUnderTest = WorldObject(id = 100, x = 3200, y = 3201, z = 2, type = 10, face = 2, oldId = 99)

        objectUnderTest.setAttachment(attachment)

        assertEquals(100, objectUnderTest.id)
        assertEquals(3200, objectUnderTest.x)
        assertEquals(3201, objectUnderTest.y)
        assertEquals(2, objectUnderTest.z)
        assertEquals(10, objectUnderTest.type)
        assertEquals(2, objectUnderTest.face)
        assertEquals(99, objectUnderTest.oldId)
        assertSame(attachment, objectUnderTest.getAttachment())
    }

    @Test
    fun `legacy object model files are removed`() {
        assertFalse(Files.exists(Path.of("src/main/kotlin/net/dodian/uber/game/model/objects/Object.kt")))
        assertFalse(Files.exists(Path.of("src/main/kotlin/net/dodian/uber/game/model/objects/RS2Object.kt")))
    }

    @Test
    fun `source tree does not import legacy object model names`() {
        val forbiddenImports =
            listOf(
                "import net.dodian.uber.game.model.object.Object;",
                "import net.dodian.uber.game.model.object.RS2Object;",
                "import net.dodian.uber.game.model.`object`.Object",
                "import net.dodian.uber.game.model.`object`.RS2Object",
            )
        val violations = mutableListOf<String>()

        listOf(Path.of("src/main"), Path.of("src/test"))
            .filter { Files.exists(it) }
            .forEach { root ->
                Files.walk(root).use { paths ->
                    paths.filter { Files.isRegularFile(it) }
                        .filter { it.toString().endsWith(".kt") || it.toString().endsWith(".java") }
                        .forEach { file ->
                            Files.readAllLines(file).forEachIndexed { index, line ->
                                val trimmed = line.trim()
                                if (forbiddenImports.any { trimmed.startsWith(it) }) {
                                    violations += "${file}:${index + 1} -> $trimmed"
                                }
                            }
                        }
                }
            }

        assertTrue(violations.isEmpty(), "Legacy object model imports must stay removed.\n${violations.joinToString("\n")}")
    }
}



