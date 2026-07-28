package net.dodian.uber.game.api.content

import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.engine.config.ServerPaths
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.nio.file.Files
import java.nio.file.Path

class ContentApiBoundaryTest {
    @Test
    fun `public content player contract has no protocol player type`() {
        val clientTypeName = "net.dodian.uber.game.model.entity.player.Client"
        val publicTypes = listOf(ContentPlayer::class.java, SkillPlayer::class.java)

        publicTypes.forEach { type ->
            assertFalse(type.declaredMethods.any { method ->
                method.returnType.name == clientTypeName || method.parameterTypes.any { it.name == clientTypeName }
            }, "${type.name} must not expose Client")
        }
    }

    @Test
    fun `client skill adapter remains engine internal`() {
        val adapter = Class.forName("net.dodian.uber.game.engine.systems.skills.ClientSkillPlayerAdapter")
        assertTrue(adapter.name.contains(".engine.systems.skills."))
    }

    @Test
    fun `skill interactions expose named payloads without destructuring or protocol escape hatches`() {
        val interactionTypes = listOf(
            "SkillObjectInteraction", "SkillNpcInteraction", "SkillItemOnItemInteraction",
            "SkillItemInteraction", "SkillItemOnObjectInteraction", "SkillMagicOnObjectInteraction",
            "SkillButtonInteraction",
        ).map { Class.forName("net.dodian.uber.game.api.plugin.skills.$it") }
        interactionTypes.forEach { type ->
            assertFalse(type.declaredMethods.any { it.name.matches(Regex("component[1-9]")) }, type.name)
        }

        val sourceRoots = listOf(
            Path.of("src/main/kotlin/net/dodian/uber/game/api/plugin/skills"),
            Path.of("src/main/kotlin/net/dodian/uber/game/skill/runtime/action"),
            Path.of("src/main/kotlin/net/dodian/uber/game/skill/runtime/requirements"),
        )
        val sources = mutableListOf<Path>()
        sourceRoots.filter(Files::exists).forEach { root ->
            Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .forEach(sources::add)
            }
        }
        sources.forEach { path ->
            val text = Files.readString(path)
            assertFalse("protocolClient(" in text, path.toString())
            assertFalse("Client.() ->" in text, path.toString())
        }
    }

    @Test
    fun `independent skill modules do not import protocol player or engine bridges`() {
        val root = ServerPaths.gameServerRoot().resolve("plugins/skills")
        val forbidden = listOf(
            "net.dodian.uber.game.model.entity.player.Client",
            "SkillEngineAccess",
            ".send(",
            "playerItems",
            "playerItemsN",
        )
        Files.walk(root).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().contains("/src/main/") && it.toString().endsWith(".kt") }
                .forEach { path ->
                    val text = Files.readString(path)
                    forbidden.forEach { token -> assertFalse(token in text, "$path contains forbidden '$token'") }
                }
        }
    }
}
