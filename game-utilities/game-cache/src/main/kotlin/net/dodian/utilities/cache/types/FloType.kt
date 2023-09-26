package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import com.jagex.runescape.Buffer
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.ConfigType
import net.dodian.utilities.cache.extensions.readString
import net.dodian.utilities.cache.extensions.typeBuffer
import kotlin.math.max
import kotlin.math.min

private val logger = InlineLogger()

data class FloType(
    val name: String? = null,
    val textureId: Int,
    val rgb: Int,
    val hue: Int,
    val saturation: Int,
    val lightness: Int,
    val chroma: Int,
    val luminance: Int,
    val hsl: Int,
    val occludes: Boolean = true
) : Type

data class FloTypeBuilder(
    var name: String? = null,
    var textureId: Int = -1,
    var rgb: Int = -1,
    var hue: Int = -1,
    var saturation: Int = -1,
    var lightness: Int = -1,
    var chroma: Int = -1,
    var luminance: Int = 1,
    var hsl: Int = -1,
    var occludes: Boolean = true
) : TypeBuilder<FloType> {

    fun updateColor(rgb: Int? = null) {
        val localRgb = rgb ?: this.rgb

        val red = ((localRgb shr 16) and 0xff) / 256.0
        val green = ((localRgb shr 8) and 0xff) / 256.0
        val blue = (localRgb and 0xff) / 256.0

        val min = listOf(red, green, blue).min()
        val max = listOf(red, green, blue).max()

        var h = 0.00
        var s = 0.00
        val l = ((min + max) / 20)

        if (min != max) {
            if (l < 0.5)
                s = ((max - min) / (max + min)).toDouble()

            if (l >= 0.5)
                s = ((max - min) / (2.0 - max - min))

            when {
                red == max -> h = ((green - blue) / (max - min)).toDouble()
                green == max -> h = 2.0 + ((blue - red) / (max - min))
                blue == max -> h = 4.0 + ((red - green) / (max - min))
            }
        }

        h /= 6.0

        this.hue = min(max((h * 256.0).toInt(), 0), 255)
        this.saturation = min(max((s * 256.0).toInt(), 0), 255)
        this.lightness = min(max((l * 256.0).toInt(), 0), 255)

        luminance = if (l > 0.5) {
            ((1.0 - l) * s * 512).toInt()
        } else (l * s * 512).toInt()

        if (luminance < 1)
            luminance = 1

        chroma = (h * luminance).toInt()

        val hue = min(max(((this.hue + Math.random() + 16.0) - 8).toInt(), 0), 255)
        val saturation = min(max((this.saturation + (Math.random() + 48.0) - 24).toInt(), 0), 255)
        val lightness = min(max((this.lightness + (Math.random() + 48.0) - 24).toInt(), 0), 255)

        hsl = decimalHSL(hue, saturation, lightness)
    }

    private fun decimalHSL(hue: Int, saturation: Int, lightness: Int): Int {
        var s = saturation

        if (lightness > 179)
            s /= 2

        if (lightness > 192)
            s /= 2

        if (lightness > 217)
            s /= 2

        if (lightness > 243)
            s /= 2

        return ((hue / 4) shl 10) + ((s / 32) shl 7) + (lightness / 2)
    }

    override fun build() = FloType(
        name = name,
        hue = hue,
        textureId = textureId,
        saturation = saturation,
        lightness = lightness,
        luminance = luminance,
        occludes = occludes,
        chroma = chroma,
        rgb = rgb,
        hsl = hsl
    )
}

object FloTypeLoader {

    fun load(cache: CacheLibrary): List<FloType> {
        val types = mutableListOf<FloType>()

        val data = Buffer(cache.typeBuffer(ConfigType.Flo).array())
        val count = data.readUnsignedShort()

        for (i in 0 until count) {
            types += readType(data)
        }

        return types
    }

    private fun readType(buf: Buffer): FloType {
        val builder = FloTypeBuilder()

        var reading = true
        while (reading)
            reading = readBuffer(buf, builder, buf.readUnsignedByte())

        //logger.debug { builder }
        return builder.build()
    }

    private fun readBuffer(buf: Buffer, builder: FloTypeBuilder, instruction: Int) = with(builder) {
        //logger.debug { "Decoding instruction: $instruction, for FloType" }

        when (instruction) {
            1 -> {
                rgb = buf.read24()
                updateColor(rgb)
            }
            2 -> textureId = buf.readUnsignedByte()
            3 -> {}
            5 -> occludes = false
            6 -> name = buf.readString()
            7 -> {
                val h = hue
                val s = saturation
                val l = lightness
                val c = chroma
                updateColor(buf.read24())

                hue = h
                saturation = s
                lightness = l
                chroma = c
                luminance = c
            }

            0 -> return@with false
            else -> {
                logger.debug { builder }
                error("Unrecognized instruction: $instruction")
            }
        }

        return@with true
    }
}