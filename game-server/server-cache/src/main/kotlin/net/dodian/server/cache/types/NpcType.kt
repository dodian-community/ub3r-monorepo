package net.dodian.server.cache.types

import com.displee.cache.CacheLibrary
import io.netty.buffer.ByteBuf
import net.dodian.server.cache.extensions.*

class NpcTypeList(
    private val types: MutableList<NpcType> = mutableListOf()
) : MutableList<NpcType> by types

data class NpcType(
    val id: Int,
    val name: String,
    val examine: String,
    val options: List<String>,
    val size: Int,
    val modelIds: List<Int>
) : Type

data class NpcTypeBuilder(
    var id: Int,
    var name: String? = null,
    var examine: String? = null,
    var options: List<String>? = null,
    var size: Int? = null,
    var modelIds: List<Int>? = null
) : TypeBuilder<NpcType> {

    override fun build() = NpcType(
        id = id,
        name = name ?: "null",
        examine = examine ?: "",
        options = options ?: emptyList(),
        size = size ?: 1,
        modelIds = modelIds ?: emptyList()
    )
}

object NpcTypeLoader {

    fun load(cache: CacheLibrary): List<NpcType> {
        val types = mutableListOf<NpcType>()

        val data = cache.typeBuffer(ConfigType.Npc)
        val meta = cache.typeMeta(ConfigType.Npc)

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

    private fun readType(buf: ByteBuf, id: Int): NpcType {
        val builder = NpcTypeBuilder(id)

        var reading = true
        while (reading) {
            reading = readBuffer(buf, builder, buf.readByte().toInt())
        }

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: NpcTypeBuilder, instruction: Int): Boolean = with(buf) {
        when (instruction) {
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
                options[instruction - 30] = readString()
                if (options[instruction - 30].lowercase() == "hidden")
                    options[instruction - 30] = ""

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

