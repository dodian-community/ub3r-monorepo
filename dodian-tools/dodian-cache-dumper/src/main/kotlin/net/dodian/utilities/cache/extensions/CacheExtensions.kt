package net.dodian.utilities.cache.extensions

import com.displee.cache.CacheLibrary
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.awt.Color
import java.util.*
import kotlin.math.floor

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

val String.hashCode
    get(): Long {
        val s = this.uppercase(Locale.getDefault())
        var hash = 0L
        for (i in s.indices) {
            hash = ((hash * 61L) + (s[i].code.toLong())) - 32L
            hash = (hash + (hash shr 56)) and 0xffffffffffffffL
        }
        return hash
    }


data class RGBColor(
    val rsHSB: Int = 0,
    val r: Int = 0,
    val g: Int = 0,
    val b: Int = 0,
    val a: Int = 0,
)

val Int.asRGB
    get(): RGBColor {
        val awtColor = Color(this)

        return RGBColor(
            rsHSB = this,
            r = awtColor.red,
            g = awtColor.green,
            b = awtColor.blue,
            a = floor((awtColor.alpha / 255.0) * 100).toInt()
        )
    }

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val yellow = 16776960
    println(yellow.toHexString())
    println("1=${yellow shl 8}, 2=${yellow shl 16}, 3=${yellow and 0xff}")
    println()

    println(16776960.asRGB)
    println()
    println(7496785.asRGB)
}