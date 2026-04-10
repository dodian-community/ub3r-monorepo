package net.dodian.uber.game.systems.cache

import net.dodian.cache.`object`.GameObjectData

object ObjectDefinitionDecoder {
    data class Result(
        val definitions: Map<Int, GameObjectData>,
        val interactiveCount: Int,
        val blockingCount: Int,
    ) {
        val definitionCount: Int
            get() = definitions.size
    }

    @JvmStatic
    fun decode(store: CacheStore): Result {
        val locDat = store.readArchiveFile(CONFIG_STORE, CONFIG_ARCHIVE, "loc.dat") ?: return EMPTY_RESULT
        val locIdx = store.readArchiveFile(CONFIG_STORE, CONFIG_ARCHIVE, "loc.idx") ?: return EMPTY_RESULT
        if (locIdx.size < 2) {
            return EMPTY_RESULT
        }

        val idxReader = CacheBuffer(locIdx)
        val count = idxReader.readUnsignedShort()
        val indices = IntArray(count)
        var offset = 2
        for (id in 0 until count) {
            indices[id] = offset
            offset += idxReader.readUnsignedShort()
        }

        val dataReader = CacheBuffer(locDat)
        val definitions = HashMap<Int, GameObjectData>(count)
        var interactiveCount = 0
        var blockingCount = 0
        for (id in 0 until count) {
            dataReader.seek(indices[id])
            val definition = decodeEntry(id, dataReader)
            definitions[id] = definition
            if (definition.hasActions()) {
                interactiveCount++
            }
            if (definition.isSolid() && !definition.isWalkable()) {
                blockingCount++
            }
        }

        return Result(
            definitions = definitions,
            interactiveCount = interactiveCount,
            blockingCount = blockingCount,
        )
    }

    private fun decodeEntry(id: Int, data: CacheBuffer): GameObjectData {
        var name = "null"
        var description = "null"
        var sizeX = 1
        var sizeY = 1
        var solid = true
        var interactive = false
        var supportItems = -1
        var obstructive = false
        var hasModelData = false
        var firstModelType: Int? = null
        val interactions = arrayOfNulls<String>(9)

        while (true) {
            when (val opcode = data.readUnsignedByte()) {
                0 -> {
                    if (supportItems == -1) {
                        supportItems = if (solid) 1 else 0
                    }
                    val inferredInteractive =
                        name != "null" &&
                            hasModelData &&
                            (firstModelType == null || firstModelType == 10)
                    val hasActions = interactive || inferredInteractive || interactions.any { it != null }
                    return GameObjectData(
                        id = id,
                        name = name,
                        description = description,
                        sizeX = sizeX.coerceAtLeast(1),
                        sizeY = sizeY.coerceAtLeast(1),
                        solid = solid,
                        walkable = !solid,
                        hasActionsFlag = hasActions,
                        unknownValue = obstructive,
                        walkType = supportItems,
                    )
                }

                1 -> {
                    val amount = data.readUnsignedByte()
                    if (amount > 0) {
                        hasModelData = true
                    }
                    repeat(amount) { index ->
                        data.skip(2)
                        val modelType = data.readUnsignedByte()
                        if (index == 0) {
                            firstModelType = modelType
                        }
                    }
                }

                2 -> name = data.readString()
                3 -> description = data.readString()
                5 -> {
                    val amount = data.readUnsignedByte()
                    if (amount > 0) {
                        hasModelData = true
                        firstModelType = null
                    }
                    repeat(amount) {
                        data.skip(2)
                    }
                }

                14 -> sizeX = data.readUnsignedByte()
                15 -> sizeY = data.readUnsignedByte()
                17 -> solid = false
                18 -> Unit
                19 -> interactive = data.readBoolean()
                21, 22, 23, 27, 62, 64, 89 -> Unit
                24 -> data.skip(2)
                28, 29, 39, 69 -> data.skip(1)
                75, 81 -> supportItems = data.readUnsignedByte()
                in 30..38 -> {
                    val action = data.readString()
                    interactions[opcode - 30] = action.takeUnless { it.equals("hidden", ignoreCase = true) }
                }

                40 -> {
                    val amount = data.readUnsignedByte()
                    repeat(amount) {
                        data.skip(2)
                        data.skip(2)
                    }
                }

                41 -> {
                    val amount = data.readUnsignedByte()
                    repeat(amount) {
                        data.skip(2)
                        data.skip(2)
                    }
                }

                60, 61, 65, 66, 67, 68, 82 -> data.skip(2)
                70, 71, 72 -> data.readShort()
                73 -> obstructive = true
                74 -> solid = false

                77, 92 -> {
                    data.skip(2)
                    data.skip(2)
                    if (opcode == 92) {
                        data.skip(2)
                    }
                    val childCount = data.readUnsignedByte()
                    repeat(childCount + 1) {
                        data.skip(2)
                    }
                }

                78 -> {
                    data.skip(2)
                    data.skip(1)
                }

                79 -> {
                    data.skip(2)
                    data.skip(2)
                    data.skip(1)
                    val amount = data.readUnsignedByte()
                    repeat(amount) {
                        data.skip(2)
                    }
                }

                249 -> {
                    val amount = data.readUnsignedByte()
                    repeat(amount) {
                        val isString = data.readUnsignedByte() == 1
                        data.skip(3)
                        if (isString) {
                            data.readString()
                        } else {
                            data.skip(4)
                        }
                    }
                }

                else -> throw IllegalArgumentException("Unsupported loc.dat opcode $opcode for object $id")
            }
        }
    }

    private const val CONFIG_STORE = 0
    private const val CONFIG_ARCHIVE = 2
    private val EMPTY_RESULT = Result(emptyMap(), interactiveCount = 0, blockingCount = 0)
}


