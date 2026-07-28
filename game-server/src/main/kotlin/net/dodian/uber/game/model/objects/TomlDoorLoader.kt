package net.dodian.uber.game.model.objects

import java.nio.file.Files
import java.nio.file.Path
import net.dodian.uber.game.engine.config.ServerPaths

object TomlDoorLoader : DoorDefinitionLoader {

    private val DOOR_PATHS = listOf(
        ServerPaths.definition("doors", "doors.toml"),
    )

    override fun load(): List<DoorDefinition> {
        val doors = ArrayList<DoorDefinition>()
        for (path in DOOR_PATHS) {
            if (!Files.exists(path)) continue
            parseToml(Files.readString(path), doors)
        }
        return doors
    }

    private fun parseToml(content: String, doors: MutableList<DoorDefinition>) {
        var x = 0
        var y = 0
        var id = 0
        var faceOpen = 0
        var faceClosed = 0
        var face = 0
        var state = 0
        var height = 0
        var inDoor = false
        var hasAllFields = false

        for (line in content.lines()) {
            val trimmed = line.trim()

            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue

            if (trimmed == "[[door]]") {
                if (inDoor && hasAllFields) {
                    doors += DoorDefinition(x, y, id, height, faceOpen, faceClosed, face, state)
                }
                x = 0; y = 0; id = 0
                faceOpen = 0; faceClosed = 0
                face = 0; state = 0; height = 0
                inDoor = true
                hasAllFields = true
                continue
            }

            if (inDoor && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0].trim()
                val value = parts[1].trim().replace("#.*".toRegex(), "").trim()
                if (value.isEmpty()) {
                    hasAllFields = false
                    continue
                }
                try {
                    when (key) {
                        "x" -> x = value.toInt()
                        "y" -> y = value.toInt()
                        "id" -> id = value.toInt()
                        "faceOpen" -> faceOpen = value.toInt()
                        "faceClosed" -> faceClosed = value.toInt()
                        "face" -> face = value.toInt()
                        "state" -> state = value.toInt()
                        "height" -> height = value.toInt()
                    }
                } catch (_: NumberFormatException) {
                    hasAllFields = false
                }
            }
        }

        if (inDoor && hasAllFields) {
            doors += DoorDefinition(x, y, id, height, faceOpen, faceClosed, face, state)
        }
    }
}
