package net.dodian.server.cache.types

import com.displee.cache.CacheLibrary
import io.netty.buffer.ByteBuf
import net.dodian.server.cache.extensions.*

class ItemTypeList(
    private val types: MutableList<ItemType> = mutableListOf()
) : MutableList<ItemType> by types

data class ItemType(
    val id: Int,
    val name: String,
    val examine: String,
    val linkedId: Int
) : Type

data class ItemTypeBuilder(
    var id: Int,
    var name: String? = null,
    var examine: String? = null,
    var linkedId: Int? = null
) : TypeBuilder<ItemType> {

    override fun build() = ItemType(
        id = id,
        name = name ?: "null (F)",
        examine = examine ?: "",
        linkedId = linkedId ?: -1
    )
}

object ItemTypeLoader {

    fun load(cache: CacheLibrary): List<ItemType> {
        val types = mutableListOf<ItemType>()

        val data = cache.typeBuffer(ConfigType.Item)
        val meta = cache.typeMeta(ConfigType.Item)

        val count = meta.readShort().toInt()
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

    private fun readType(buf: ByteBuf, id: Int): ItemType {
        val builder = ItemTypeBuilder(id)

        var reading = true
        while (reading) {
            reading = readBuffer(buf, builder, buf.readUnsignedByte().toInt())
        }

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: ItemTypeBuilder, instruction: Int): Boolean = with(buf) {
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
            else -> return false
        }

        return true
    }
}