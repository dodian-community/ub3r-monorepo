package net.dodian.utilities.cache.extensions

import com.displee.cache.CacheLibrary
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

const val ARCHIVE_TITLE = 1
const val ARCHIVE_CONFIG = 2
const val ARCHIVE_INTERFACE = 3
const val ARCHIVE_GRAPHICS = 4
const val ARCHIVE_UPDATE = 5
const val ARCHIVE_TEXTURES = 6
const val ARCHIVE_CHAT = 7
const val ARCHIVE_SOUND = 8

enum class ConfigType(val data: String, val meta: String? = null) {
    Npc("npc.dat", "npc.idx"),
    Obj("obj.dat", "obj.idx"),
    Loc("loc.dat", "loc.idx"),
    SpotAnim("spotanim.dat"),
    Varbit("varbit.dat"),
    Varp("varp.dat"),
    Seq("seq.dat"),
    Flo("flo.dat"),
    Idk("idk.dat")
    ;
}

fun CacheLibrary.typeMeta(config: ConfigType) = if (config.meta != null)
        data(0, ARCHIVE_CONFIG, config.meta)?.toByteBuf() ?: error("Couldn't find idx (meta) file for '$config'")
    else error("'$config' does not have an idx (meta) file")

fun CacheLibrary.typeBuffer(config: ConfigType) = data(0, ARCHIVE_CONFIG, config.data)?.toByteBuf()
    ?: error("Couldn't find data file for '$config'")

fun ByteArray.toByteBuf() = Unpooled.wrappedBuffer(this)


const val STRING_TERMINATOR = 10
fun ByteBuf.readString(): String {
    val builder = StringBuilder()

    var character: Int = -1
    while (isReadable && readUnsignedByte().toInt().also { character = it } != STRING_TERMINATOR)
        builder.append(character.toChar())

    return builder.toString()
}