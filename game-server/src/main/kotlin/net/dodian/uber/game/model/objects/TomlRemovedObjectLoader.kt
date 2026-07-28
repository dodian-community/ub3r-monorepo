package net.dodian.uber.game.model.objects

import java.nio.file.Files
import java.nio.file.Path
import net.dodian.uber.game.engine.config.ServerPaths

data class RemovedObjectEntry(
    val x: Int,
    val y: Int,
    val z: Int,
    val type: Int?,
)

object TomlRemovedObjectLoader {
    @JvmStatic
    fun load(path: String = ServerPaths.definition("objects", "removed.toml").toString()): List<RemovedObjectEntry> {
        val file = Path.of(path)
        if (!Files.isRegularFile(file)) {
            return emptyList()
        }

        val result = mutableListOf<RemovedObjectEntry>()
        var x: Int? = null
        var xRange: IntRange? = null
        var y: Int? = null
        var yRange: IntRange? = null
        var z = 0
        var type: Int? = null
        var inEntry = false

        fun flush() {
            if (!inEntry) return
            val fx = x; val fy = y
            val fxr = xRange; val fyr = yRange
            when {
                fx != null && fy != null -> {
                    result += RemovedObjectEntry(fx, fy, z, type)
                }
                fxr != null && fyr != null -> {
                    for (rx in fxr) {
                        for (ry in fyr) {
                            result += RemovedObjectEntry(rx, ry, z, type)
                        }
                    }
                }
            }
            x = null; xRange = null; y = null; yRange = null; z = 0; type = null
            inEntry = false
        }

        for (line in Files.readAllLines(file)) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue

            if (trimmed == "[[remove]]") {
                flush()
                inEntry = true
                continue
            }

            if (inEntry && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0].trim()
                val raw = parts[1].trim().trim('"')
                when (key) {
                    "x" -> {
                        if (raw.contains("..")) {
                            val (from, to) = raw.split("..", limit = 2)
                            xRange = from.trim().toInt()..to.trim().toInt()
                        } else {
                            x = raw.toInt()
                        }
                    }
                    "y" -> {
                        if (raw.contains("..")) {
                            val (from, to) = raw.split("..", limit = 2)
                            yRange = from.trim().toInt()..to.trim().toInt()
                        } else {
                            y = raw.toInt()
                        }
                    }
                    "z" -> z = raw.toInt()
                    "type" -> type = raw.toInt()
                }
            }
        }
        flush()
        return result
    }
}
