package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.*

private val logger = InlineLogger()

data class ObjType(
    val id: Int,
    val name: String,
    val examine: String,
    val linkedId: Int
) : Type

data class ObjTypeBuilder(
    var id: Int,
    var name: String = "null",
    var examine: String = "I don't know anything about this item.",
    var price: Int = 1,
    var members: Boolean = false,
    var team: Int = 0,

    var options: MutableList<String?> = Array<String?>(5) { null }.toMutableList(),
    var optionsInventory: MutableList<String?> = Array<String?>(5) { null }.toMutableList(),

    var canStack: Boolean = false,
    var stackId: MutableList<Int> = IntArray(10).toMutableList(),
    var stackCount: MutableList<Int> = IntArray(10).toMutableList(),

    var linkedId: Int = -1,
    var certificateId: Int = -1,

    var lightAmbient: Int = 0,
    var lightAttenuation: Int = 0,
    var colorSrc: MutableList<Int> = mutableListOf(),
    var colorDst: MutableList<Int> = mutableListOf(),
    var scaleX: Int = 128,
    var scaleY: Int = 128,
    var scaleZ: Int = 128,

    var iconOffsetX: Int = 0,
    var iconOffsetY: Int = 0,
    var iconZoom: Int = 2000,
    var iconYaw: Int = 0,
    var iconPitch: Int = 0,
    var iconRoll: Int = 0,

    var femaleOffsetY: Int = 0,
    var femaleModelId0: Int = -1,
    var femaleModelId1: Int = -1,
    var femaleModelId2: Int = -1,
    var femaleHeadModelId0: Int = -1,
    var femaleHeadModelId1: Int = -1,

    var maleOffsetY: Int = 0,
    var maleModelId0: Int = -1,
    var maleModelId1: Int = -1,
    var maleModelId2: Int = -1,
    var maleHeadModelId0: Int = -1,
    var maleHeadModelId1: Int = -1,
) : TypeBuilder<ObjType> {

    override fun build() = ObjType(
        id = id,
        name = name,
        examine = examine,
        linkedId = linkedId
    )
}

object ObjTypeLoader : TypeLoader<ObjType> {

    override fun load(cache: CacheLibrary): List<ObjType> {
        val types = mutableListOf<ObjType>()

        val data = cache.typeBuffer(ConfigType.Obj)
        val meta = cache.typeMeta(ConfigType.Obj)

        val count = meta.readUnsignedShort()
        val indices = IntArray(count)
        logger.info { "Loading $count ObjTypes..." }

        var index = ARCHIVE_CONFIG
        for (i in 0 until count) {
            indices[i] = index
            index += meta.readShort().toInt()
        }

        for (typeId in 0 until count) {
            data.readerIndex(indices[typeId])
            types += readType(data, typeId)
        }

        logger.info { "Loaded $count ObjTypes..." }
        println()
        return types
    }

    private fun readType(buf: ByteBuf, id: Int): ObjType {
        val builder = ObjTypeBuilder(id)

        var reading = true
        while (reading) {
            reading = readBuffer(buf, builder, buf.readUnsignedByte().toInt())
        }

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: ObjTypeBuilder, instruction: Int): Boolean = with(builder) {
        //logger.debug { "Decoding instruction: $instruction, for Obj ${builder.id}" }

        when (instruction) {
            1 -> buf.readUnsignedShort()
            2 -> name = buf.readString()
            3 -> examine = buf.readString()
            4 -> iconZoom = buf.readUnsignedShort()
            5 -> iconPitch = buf.readUnsignedShort()
            6 -> iconYaw = buf.readUnsignedShort()
            7 -> {
                iconOffsetX = buf.readUnsignedShort()
                if (iconOffsetX > 32_767)
                    iconOffsetX -= 65_536
            }

            8 -> {
                iconOffsetY = buf.readUnsignedShort()
                if (iconOffsetY > 32_767)
                    iconOffsetY -= 65_536
            }

            10 -> buf.readUnsignedShort()
            11 -> canStack = true
            12 -> price = buf.readInt()
            16 -> members = true
            23 -> {
                maleModelId0 = buf.readUnsignedShort()
                maleOffsetY = buf.readByte().toInt()
            }

            24 -> maleModelId1 = buf.readUnsignedShort()
            25 -> {
                femaleModelId0 = buf.readUnsignedShort()
                femaleOffsetY = buf.readByte().toInt()
            }

            26 -> femaleModelId1 = buf.readUnsignedShort()
            in 30..34 -> {
                options[instruction - 30] = buf.readString()
                if (options[instruction - 30].equals("hidden", ignoreCase = true))
                    options[instruction - 30] = null
            }

            in 35..39 -> optionsInventory[instruction - 35] = buf.readString()

            40 -> {
                val count = buf.readUnsignedByte().toInt()
                colorSrc = IntArray(count).toMutableList()
                colorDst = IntArray(count).toMutableList()
                for (i in 0 until count) {
                    colorSrc[i] = buf.readUnsignedShort()
                    colorDst[i] = buf.readUnsignedShort()
                }
            }

            78 -> maleModelId2 = buf.readUnsignedShort()
            79 -> femaleModelId2 = buf.readUnsignedShort()
            90 -> maleHeadModelId0 = buf.readUnsignedShort()
            91 -> femaleHeadModelId0 = buf.readUnsignedShort()
            92 -> maleHeadModelId1 = buf.readUnsignedShort()
            93 -> femaleHeadModelId1 = buf.readUnsignedShort()
            95 -> iconRoll = buf.readUnsignedShort()
            97 -> builder.linkedId = buf.readUnsignedShort()
            98 -> certificateId = buf.readUnsignedShort()
            in 100..109 -> {
                stackId[instruction - 100] = buf.readUnsignedShort()
                stackCount[instruction - 100] = buf.readUnsignedShort()
            }

            110 -> scaleX = buf.readUnsignedShort()
            111 -> scaleZ = buf.readUnsignedShort()
            112 -> scaleY = buf.readUnsignedShort()
            113 -> lightAmbient = buf.readByte().toInt()
            114 -> lightAttenuation = buf.readByte().toInt()
            115 -> team = buf.readUnsignedByte().toInt()

            0 -> return false
            else -> {
                logger.debug { builder }
                error("Unrecognized instruction: $instruction")
            }
        }

        return true
    }
}