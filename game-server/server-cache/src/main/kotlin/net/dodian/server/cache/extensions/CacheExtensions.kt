package net.dodian.server.cache.extensions

import com.displee.cache.CacheLibrary
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled


const val ARCHIVE_TITLE = 1
const val ARCHIVE_CONFIG = 2
const val ARCHIVE_INTERFACES = 3
const val ARCHIVE_GRAPHICS = 4
const val ARCHIVE_UPDATE = 5
const val ARCHIVE_TEXTURES = 6
const val ARCHIVE_CHAT = 7
const val ARCHIVE_SOUND = 8

enum class ConfigType(val data: String, val meta: String) {
    Npc("npc.dat", "npc.idx"),
    Item("obj.dat", "obj.idx")
}

fun CacheLibrary.typeMeta(config: ConfigType) = data(0, ARCHIVE_CONFIG, config.meta)?.toByteBuf()
    ?: error("Couldn't find meta... (config=$config)")
fun CacheLibrary.typeBuffer(config: ConfigType) = data(0, ARCHIVE_CONFIG, config.data)?.toByteBuf()
    ?: error("Couldn't find data... (config=$config)")

private fun ByteArray.toByteBuf() = Unpooled.wrappedBuffer(this)


const val STRING_TERMINATOR = 10
fun ByteBuf.readString(): String {
    val builder = StringBuilder()

    var character: Int = -1
    while (isReadable && readUnsignedByte().toInt().also { character = it } != STRING_TERMINATOR)
        builder.append(character.toChar())

    return builder.toString()
}