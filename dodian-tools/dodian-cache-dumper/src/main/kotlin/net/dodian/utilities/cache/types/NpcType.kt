package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.*

private val logger = InlineLogger()

class NpcTypeList(
    private val types: MutableList<NpcType> = mutableListOf()
) : MutableList<NpcType> by types

data class NpcType(
    val id: Int,
    val uid: Long,
    val name: String,
    val examine: String,
    val options: List<String>,
    val level: Int,
    val important: Boolean,
    val showOnMinimap: Boolean,
    val interactAble: Boolean,
    val lightAmbient: Int,
    val lightAttenuation: Int,
    val colorSrc: List<Int>,
    val colorDst: List<Int>,
    val modelIds: List<Int>,
    val headModelIds: List<Int>,
    val headIcon: Int,
    val size: Int,
    val scaleXY: Int,
    val scaleZ: Int,
    val seqTurnLeftId: Int,
    val seqTurnRightId: Int,
    val seqTurnAroundId: Int,
    val seqStandId: Int,
    val seqWalkId: Int,
    val turnSpeed: Int,
    val varpId: Int,
    val varbitId: Int,
    val overrides: List<Int>,
) : Type

data class NpcTypeBuilder(
    var id: Int,
    var uid: Long = -1L,
    var name: String = "null",
    var examine: String = "I don't anything about this NPC.",
    var options: MutableList<String> = mutableListOf(),
    var level: Int = -1,
    var important: Boolean = false,
    var showOnMinimap: Boolean = true,
    var interactAble: Boolean = true,
    var lightAmbient: Int = 0,
    var lightAttenuation: Int = 0,
    var colorSrc: MutableList<Int> = mutableListOf(),
    var colorDst: MutableList<Int> = mutableListOf(),
    var modelIds: MutableList<Int> = mutableListOf(),
    var headModelIds: MutableList<Int> = mutableListOf(),
    var headIcon: Int = -1,
    var size: Int = 1,
    var scaleXY: Int = 128,
    var scaleZ: Int = 128,
    var seqTurnLeftId: Int = -1,
    var seqTurnRightId: Int = -1,
    var seqTurnAroundId: Int = -1,
    var seqStandId: Int = -1,
    var seqWalkId: Int = -1,
    var turnSpeed: Int = 32,
    var varpId: Int = -1,
    var varbitId: Int = -1,
    var overrides: MutableList<Int> = mutableListOf()
) : TypeBuilder<NpcType> {

    override fun build() = NpcType(
        id = id,
        uid = uid,
        name = name,
        examine = examine,
        options = options,
        level = level,
        important = important,
        showOnMinimap = showOnMinimap,
        interactAble = interactAble,
        lightAmbient = lightAmbient,
        lightAttenuation = lightAttenuation,
        colorSrc = colorSrc,
        colorDst = colorDst,
        modelIds = modelIds,
        headModelIds = headModelIds,
        headIcon = headIcon,
        size = size,
        scaleXY = scaleXY,
        scaleZ = scaleZ,
        seqTurnLeftId = seqTurnLeftId,
        seqTurnRightId = seqTurnRightId,
        seqTurnAroundId = seqTurnAroundId,
        seqStandId = seqStandId,
        seqWalkId = seqWalkId,
        turnSpeed = turnSpeed,
        varpId = varpId,
        varbitId = varbitId,
        overrides = overrides
    )
}

object NpcTypeLoader : TypeLoader<NpcType> {

    override fun load(cache: CacheLibrary): List<NpcType> {
        val types = mutableListOf<NpcType>()

        val data = cache.typeBuffer(ConfigType.Npc)
        val meta = cache.typeMeta(ConfigType.Npc)

        val count = meta.readUnsignedShort()
        val indices = IntArray(count)
        logger.info { "Loading $count NpcTypes..." }

        var index = ARCHIVE_CONFIG
        for (i in 0 until count) {
            indices[i] = index
            index += meta.readShort().toInt()
        }

        for (typeId in 0 until count) {
            data.readerIndex(indices[typeId])
            types += readType(data, typeId)
        }

        logger.info { "Loaded $count NpcTypes..." }
        println()
        return types
    }

    private fun readType(buf: ByteBuf, id: Int): NpcType {
        val builder = NpcTypeBuilder(id)

        var reading = true
        while (reading) {
            reading = readBuffer(buf, builder, buf.readUnsignedByte().toInt())
        }

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: NpcTypeBuilder, instruction: Int) = with(builder) {
        //logger.debug { "Decoding instruction: $instruction, for NPC ${builder.id}" }

        when (instruction) {
            1 -> {
                val count = buf.readUnsignedByte().toInt()
                modelIds = IntArray(count).toMutableList()
                for (i in 0 until count) {
                    modelIds[i] = buf.readUnsignedShort()
                }
            }

            2 -> name = buf.readString()
            3 -> examine = buf.readString()
            12 -> size = buf.readByte().toInt()
            13 -> seqStandId = buf.readUnsignedShort()
            14 -> seqWalkId = buf.readUnsignedShort()
            17 -> {
                seqWalkId = buf.readUnsignedShort()
                seqTurnAroundId = buf.readUnsignedShort()
                seqTurnLeftId = buf.readUnsignedShort()
                seqTurnRightId = buf.readUnsignedShort()
            }

            in 30..39 -> {
                options = Array(5) { "" }.toMutableList()
                options[instruction - 30] = buf.readString()
                if (options[instruction - 30].lowercase() == "hidden")
                    options[instruction - 30] = ""
            }

            40 -> {
                val count = buf.readUnsignedByte().toInt()
                val colorSrc = IntArray(count).toMutableList()
                val colorDst = IntArray(count).toMutableList()
                for (i in 0 until count) {
                    colorSrc[i] = buf.readUnsignedShort()
                    colorDst[i] = buf.readUnsignedShort()
                }
            }

            60 -> {
                val count = buf.readUnsignedByte().toInt()
                headModelIds = IntArray(count).toMutableList()
                for (i in 0 until count)
                    headModelIds[i] = buf.readUnsignedShort()
            }

            90, 91, 92 -> buf.readUnsignedShort()
            93 -> showOnMinimap = false
            95 -> level = buf.readUnsignedShort()
            97 -> scaleXY = buf.readUnsignedShort()
            98 -> scaleZ = buf.readUnsignedShort()
            99 -> important = true
            100 -> lightAmbient = buf.readByte().toInt()
            101 -> lightAttenuation = buf.readByte().toInt()
            102 -> headIcon = buf.readUnsignedShort()
            103 -> turnSpeed = buf.readUnsignedShort()
            106 -> {
                varbitId = buf.readUnsignedShort()
                if (varbitId == 65535)
                    varbitId = -1

                varpId = buf.readUnsignedShort()
                if (varpId == 65535)
                    varpId = -1

                val overrideCount = buf.readUnsignedByte().toInt()
                overrides = IntArray(overrideCount + 1).toMutableList()

                for (i in 0 .. overrideCount) {
                    overrides[i] = buf.readUnsignedShort()
                    if (overrides[i] == 65535)
                        overrides[i] = -1
                }
            }

            107 -> interactAble = false
            0 -> return@with false
            else -> {
                logger.debug { builder }
                error("Unrecognized instruction: $instruction")
            }
        }

        return@with true
    }
}

/*class NpcTypeLoader(
    cache: CacheLibrary
) : TypeLoader<NpcType, NpcTypeBuilder>(
    cache = cache,
    configType = ConfigType.Npc,
    typeList = NpcTypeList(),
    typeBuilder = NpcTypeBuilder::class.java
) {

    override fun decoder(builder: NpcTypeBuilder, data: ByteBuf): Boolean = with(data) {
        when (val code = readByte().toInt()) {
            1 -> {
                val count = readUnsignedByte().toInt()
                for (i in 0 until count) {
                    readUnsignedShort()
                }
            }

            2 -> builder.name = readString()
            3 -> builder.examine = readString()
            12 -> builder.size = readByte().toInt()
            13 -> readUnsignedShort()
            14 -> readUnsignedShort()
            17 -> {
                readUnsignedShort()
                readUnsignedShort()
                readUnsignedShort()
                readUnsignedShort()
            }

            in 30..39 -> {
                val options = Array(5) { "" }
                options[code - 30] = readString()
                if (options[code - 30].lowercase() == "hidden")
                    options[code - 30] = ""

                builder.options = options.asList()
            }

            40 -> {
                val count = readUnsignedByte().toInt()
                for (i in 0 until count) {
                    readUnsignedShort()
                    readUnsignedShort()
                }
            }

            60 -> {
                val count = readUnsignedByte().toInt()
                for (i in 0 until count)
                    readUnsignedShort()
            }

            90, 91, 92 -> readUnsignedShort()
            93 -> {}
            95 -> readUnsignedShort()
            97 -> readUnsignedShort()
            98 -> readUnsignedShort()
            99 -> {}
            100 -> readByte()
            101 -> readByte()
            102 -> readByte()
            103 -> readUnsignedShort()
            106 -> {
                readUnsignedShort()
                readUnsignedShort()
                val count = readUnsignedByte().toInt()

                for (i in 0 until count + 1) {
                    readUnsignedShort()
                }
            }

            107 -> {}

            0 -> return false
            else -> return false
        }

        return true
    }
}*/

