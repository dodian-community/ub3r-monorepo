package net.dodian.uber.game.engine.systems.world.item

import java.nio.file.Files
import java.nio.file.Path
import net.dodian.uber.game.engine.config.ServerPaths
import net.dodian.uber.game.model.Position

object TomlGroundItemSpawnLoader {
    @JvmStatic
    fun load(path: String = ServerPaths.definition("items", "ground_item_spawns.toml").toString()): List<GroundItemSpawn> {
        val file = Path.of(path)
        if (!Files.isRegularFile(file)) {
            return emptyList()
        }

        val entries = mutableListOf<GroundItemSpawn>()
        var x = -1
        var y = -1
        var z = 0
        var itemId = -1
        var amount = 1
        var displayTime = 0
        var inEntry = false
        var hasX = false
        var hasY = false
        var hasItemId = false

        fun flush() {
            if (inEntry && hasX && hasY && hasItemId) {
                entries += GroundItemSpawn(Position(x, y, z), itemId, amount, displayTime)
            }
            x = -1
            y = -1
            z = 0
            itemId = -1
            amount = 1
            displayTime = 0
            inEntry = false
            hasX = false
            hasY = false
            hasItemId = false
        }

        for (line in Files.readAllLines(file)) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue

            if (trimmed == "[[spawn]]") {
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
                        "x" -> {
                            x = value.toInt()
                            hasX = true
                        }
                        "y" -> {
                            y = value.toInt()
                            hasY = true
                        }
                        "z" -> z = value.toInt()
                        "itemId" -> {
                            itemId = value.toInt()
                            hasItemId = true
                        }
                        "amount" -> amount = value.toInt()
                        "displayTime" -> displayTime = value.toInt()
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
