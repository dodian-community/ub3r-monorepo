package net.dodian.server.cache

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.dodian.server.cache.extensions.ARCHIVE_CONFIG
import net.dodian.server.cache.types.NpcType
import net.dodian.server.cache.types.NpcTypeBuilder

private val logger = InlineLogger()

class CacheService(
    private val path: String = "./data/cache",
    val cache: CacheLibrary = CacheLibrary(path)
) {

    fun npcTypes(): List<NpcType> {
        return emptyList()
    }
}

const val STRING_TERMINATOR = 10
fun ByteBuf.readString(): String {
    val builder = StringBuilder()

    var character: Int = -1
    while (isReadable && readUnsignedByte().toInt().also { character = it } != STRING_TERMINATOR)
        builder.append(character.toChar())

    return builder.toString()
}

fun decode(id: Int, data: ByteBuf): NpcType = with(data) {
    val builder = NpcTypeBuilder(id)

    var reading = true

    while (reading) {
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
                val varbit = readUnsignedShort()
                val varp = readUnsignedShort()
                val count = readUnsignedByte().toInt()

                for (i in 0 until count + 1) {
                    readUnsignedShort()
                }
            }

            107 -> {}

            0 -> reading = false
            else -> reading = false
        }
    }

    return builder.build()
}

fun main() {
    val service = CacheService("./game-server/server-cache/data/cache")
    val cache = service.cache

    cache.index(0).cache()

    val data = Unpooled.wrappedBuffer(cache.data(0, ARCHIVE_CONFIG, "npc.dat"))
    val meta = Unpooled.wrappedBuffer(cache.data(0, ARCHIVE_CONFIG, "npc.idx"))

    val count = meta.readShort().toInt()
    val indices = IntArray(count)

    var index = 2
    for (i in 0 until count) {
        indices[i] = index
        index += meta.readShort().toInt()
    }

    val npcTypes = Array<NpcType?>(count) { null }
    for (i in 0 until count) {
        data.readerIndex(indices[i])
        npcTypes[i] = decode(i, data)
    }


    println("Loaded & Decoded ${npcTypes.filterNotNull().size} NPC Types!")
    println("===============================")
    npcTypes.filterNotNull().forEach {
        println("${it.id} = ${it.name}")
    }
}