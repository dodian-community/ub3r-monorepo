package net.dodian.uber.game.objects

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.engine.config.ServerPaths
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.stream.Collectors

data class ObjectSpawnEntry(
    val id: Int,
    val name: String? = null,
    val x: Int,
    val y: Int,
    val z: Int = 0,
    val type: Int = 10,
    val rotation: String? = null,
    val face: Int? = null,
    val sizeX: Int? = null,
    val sizeY: Int? = null,
    val solid: Boolean? = null,
    val walkable: Boolean? = null,
)

object ObjectSpawnRepository {
    @JvmStatic
    fun resolveSpawnsPath(): Path {
        return ServerPaths.definition("objects", "spawns")
    }

    @JvmStatic
    @JvmOverloads
    fun load(root: Path = resolveSpawnsPath()): List<WorldObject> {
        if (!Files.isDirectory(root)) {
            return emptyList()
        }

        val files = Files.walk(root).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".toml") }
                .sorted()
                .collect(Collectors.toList())
        }

        val result = ArrayList<WorldObject>()
        for (file in files) {
            val content = Files.readString(file)
            val entries = parseToml(content, file.toString())
            for (entry in entries) {
                val rotationFace = parseRotation(entry.rotation) ?: entry.face ?: 0
                val worldObj = WorldObject(entry.id, entry.x, entry.y, entry.z, entry.type, rotationFace)
                result.add(worldObj)

                if (entry.sizeX != null || entry.sizeY != null || entry.solid != null || entry.walkable != null) {
                    val defaultData = GameObjectData.forId(entry.id)
                    val solid = entry.solid ?: defaultData.isSolid()
                    val customData = GameObjectData(
                        id = entry.id,
                        name = entry.name ?: defaultData.name,
                        description = defaultData.description,
                        sizeX = entry.sizeX ?: defaultData.sizeX,
                        sizeY = entry.sizeY ?: defaultData.sizeY,
                        solid = solid,
                        impenetrable = if (entry.walkable == true) false else defaultData.isImpenetrable(),
                        hasActionsFlag = defaultData.hasActions(),
                        decoration = defaultData.isDecoration(),
                        walkType = 2,
                        blockWalk = if (solid) 2 else 0,
                        blockRange = solid,
                        breakRouteFinding = defaultData.breakRouteFinding(),
                        walkingFlag = defaultData.walkingFlag,
                        varbitId = defaultData.varbitId,
                        varpId = defaultData.varpId,
                        childIds = defaultData.childIds,
                    )
                    GameObjectData.addDefinition(customData)
                }
            }
        }
        return result
    }

    private fun parseToml(content: String, filePath: String): List<ObjectSpawnEntry> {
        val entries = mutableListOf<ObjectSpawnEntry>()
        var id: Int? = null
        var name: String? = null
        var x: Int? = null
        var y: Int? = null
        var z = 0
        var type = 10
        var rotation: String? = null
        var face: Int? = null
        var sizeX: Int? = null
        var sizeY: Int? = null
        var solid: Boolean? = null
        var walkable: Boolean? = null
        var inObject = false
        var lineNumber = 0

        fun flush() {
            if (!inObject) return
            val effectiveId = id
            val effectiveX = x
            val effectiveY = y
            if (effectiveId == null || effectiveId < 0) {
                throw IllegalArgumentException("$filePath:$lineNumber: object block missing valid 'id' field")
            }
            if (effectiveX == null) {
                throw IllegalArgumentException("$filePath:$lineNumber: object block id=$effectiveId missing 'x' field")
            }
            if (effectiveY == null) {
                throw IllegalArgumentException("$filePath:$lineNumber: object block id=$effectiveId missing 'y' field")
            }

            entries += ObjectSpawnEntry(effectiveId, name, effectiveX, effectiveY, z, type, rotation, face, sizeX, sizeY, solid, walkable)

            id = null; name = null; x = null; y = null; z = 0; type = 10
            rotation = null; face = null; sizeX = null; sizeY = null; solid = null; walkable = null
            inObject = false
        }

        for (rawLine in content.lines()) {
            lineNumber++
            val trimmed = rawLine.trim()
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue

            if (trimmed == "[[object]]") {
                flush()
                inObject = true
                continue
            }

            if (inObject && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0].trim()
                val raw = parts[1].trim().trim('"')
                try {
                    when (key) {
                        "id" -> {
                            val parsed = raw.toInt()
                            if (parsed < 0) throw IllegalArgumentException("$filePath:$lineNumber: id must not be negative, got $raw")
                            id = parsed
                        }
                        "name" -> name = raw
                        "x" -> { x = raw.toInt() }
                        "y" -> { y = raw.toInt() }
                        "z" -> z = raw.toInt()
                        "type" -> type = raw.toInt()
                        "rotation" -> rotation = raw
                        "face" -> face = raw.toIntOrNull()
                        "sizeX" -> sizeX = raw.toIntOrNull()
                        "sizeY" -> sizeY = raw.toIntOrNull()
                        "solid" -> solid = raw.lowercase(Locale.ROOT).toBooleanStrictOrNull()
                        "walkable" -> walkable = raw.lowercase(Locale.ROOT).toBooleanStrictOrNull()
                        else -> throw IllegalArgumentException("$filePath:$lineNumber: unknown key '$key'")
                    }
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("$filePath:$lineNumber: invalid value for '$key': $raw", e)
                }
            }
        }
        flush()
        return entries
    }

    private fun parseRotation(raw: String?): Int? {
        if (raw == null) return null
        return when (raw.uppercase(Locale.ROOT)) {
            "SOUTH", "0" -> 0
            "WEST", "1" -> 1
            "NORTH", "2" -> 2
            "EAST", "3" -> 3
            else -> raw.toIntOrNull()
        }
    }
}
