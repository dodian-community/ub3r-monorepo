package net.dodian.uber.game.combat

import java.nio.file.Files
import java.nio.file.Path
import net.dodian.uber.game.engine.config.ServerPaths

data class ProjectileConfigEntry(
    val name: String,
    val id: Int,
    val def: ProjectileDef
)

object TomlProjectileLoader {
    @JvmStatic
    fun load(path: String = ServerPaths.definition("combat", "projectiles.toml").toString()): List<ProjectileConfigEntry> {
        val file = Path.of(path)
        if (!Files.isRegularFile(file)) {
            return emptyList()
        }

        val entries = mutableListOf<ProjectileConfigEntry>()
        var name = ""
        var id = -1
        var startHeight = 43
        var endHeight = 31
        var delay = 51
        var slope = 16
        var inEntry = false
        var hasName = false
        var hasId = false

        fun flush() {
            if (inEntry && hasName && hasId) {
                entries += ProjectileConfigEntry(
                    name = name,
                    id = id,
                    def = ProjectileDef(
                        startHeight = startHeight,
                        endHeight = endHeight,
                        delay = delay,
                        slope = slope
                    )
                )
            }
            name = ""
            id = -1
            startHeight = 43
            endHeight = 31
            delay = 51
            slope = 16
            inEntry = false
            hasName = false
            hasId = false
        }

        for (line in Files.readAllLines(file)) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue

            if (trimmed == "[[projectile]]") {
                flush()
                inEntry = true
                continue
            }

            if (inEntry && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0].trim()
                val value = parts[1].trim().replace("#.*".toRegex(), "").trim().trim('"')
                if (value.isEmpty()) continue

                try {
                    when (key) {
                        "name" -> {
                            name = value
                            hasName = true
                        }
                        "id" -> {
                            id = value.toInt()
                            hasId = true
                        }
                        "startHeight" -> startHeight = value.toInt()
                        "endHeight" -> endHeight = value.toInt()
                        "delay" -> delay = value.toInt()
                        "slope" -> slope = value.toInt()
                    }
                } catch (_: NumberFormatException) {
                    // Ignore malformed fields
                }
            }
        }
        flush()
        return entries
    }
}
