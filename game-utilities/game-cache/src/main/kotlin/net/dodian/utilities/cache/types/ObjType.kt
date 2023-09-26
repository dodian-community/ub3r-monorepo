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
    var name: String? = null,
    var examine: String? = null,
    var linkedId: Int? = null
) : TypeBuilder<ObjType> {

    override fun build() = ObjType(
        id = id,
        name = name ?: "null (F)",
        examine = examine ?: "",
        linkedId = linkedId ?: -1
    )
}

object ObjTypeLoader : TypeLoader<ObjType> {

    override fun load(cache: CacheLibrary): List<ObjType> {
        val types = mutableListOf<ObjType>()

        val data = cache.typeBuffer(ConfigType.Obj)
        val meta = cache.typeMeta(ConfigType.Obj)

        val count = meta.readUnsignedShort()
        val indices = IntArray(count)

        var index = ARCHIVE_CONFIG
        for (i in 0 until count) {
            indices[i] = index
            index += meta.readShort().toInt()
        }

        for (typeId in 0 until count) {
            data.readerIndex(indices[typeId])
            types += readType(data, typeId)
        }

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

    private fun readBuffer(buf: ByteBuf, builder: ObjTypeBuilder, instruction: Int): Boolean = with(buf) {
        //logger.debug { "Decoding instruction: $instruction, for Obj ${builder.id}" }

        when (instruction) {
            1 -> readUnsignedShort()
            2 -> builder.name = readString()
            3 -> builder.examine = readString()
            4 -> readUnsignedShort()
            5 -> readUnsignedShort()
            6 -> readUnsignedShort()
            7 -> readUnsignedShort()
            8 -> readUnsignedShort()
            10 -> readUnsignedShort()
            11 -> {}
            12 -> readInt()
            16 -> {}
            23 -> {
                readUnsignedShort()
                readByte()
            }
            24 -> readUnsignedShort()
            25 -> {
                readUnsignedShort()
                readByte()
            }
            26 -> readUnsignedShort()
            in 30..34 -> {
                readString()
            }
            in 35..39 -> {
                readString()
            }
            40 -> {
                val count = readUnsignedByte().toInt()
                for (i in 0 until count) {
                    readUnsignedShort()
                    readUnsignedShort()
                }
            }
            78 -> readUnsignedShort()
            79 -> readUnsignedShort()
            90 -> readUnsignedShort()
            91 -> readUnsignedShort()
            92 -> readUnsignedShort()
            93 -> readUnsignedShort()
            95 -> readUnsignedShort()
            97 -> builder.linkedId = readUnsignedShort()
            98 -> readUnsignedShort()
            in 100..109 -> {
                readUnsignedShort()
                readUnsignedShort()
            }
            110 -> readUnsignedShort()
            111 -> readUnsignedShort()
            112 -> readUnsignedShort()
            113 -> readByte()
            114 -> readByte()
            115 -> readUnsignedByte()

            0 -> return false
            else -> {
                logger.debug { builder }
                error("Unrecognized instruction: $instruction")
            }
        }

        return true
    }
}