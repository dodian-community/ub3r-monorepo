package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.ConfigType
import net.dodian.utilities.cache.extensions.readString
import net.dodian.utilities.cache.extensions.typeBuffer

private val logger = InlineLogger()

data class VarbitType(
    val varp: Int,
    val lsb: Int,
    val msb: Int
) : Type

data class VarbitTypeBuilder(
    var varp: Int? = null,
    var lsb: Int? = null,
    var msb: Int? = null
) : TypeBuilder<VarbitType> {

    override fun build() = VarbitType(
        varp = varp ?: error("No value defined for 'varp'"),
        lsb = lsb ?: error("No value defined for 'lsb'"),
        msb = msb ?: error("No value defined for 'msb'")
    )
}

object VarbitTypeLoader {

    fun load(cache: CacheLibrary): List<VarbitType> {
        val types = mutableListOf<VarbitType>()

        val data = cache.typeBuffer(ConfigType.Varbit)
        val count = data.readUnsignedShort()

        for (i in 0 until count) {
            types += readType(data)
        }

        return types
    }

    private fun readType(buf: ByteBuf): VarbitType {
        val builder = VarbitTypeBuilder()

        var reading = true
        while (reading)
            reading = readBuffer(buf, builder, buf.readUnsignedByte().toInt())

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: VarbitTypeBuilder, instruction: Int) = with(builder) {
        when (instruction) {
            1 -> {
                varp = buf.readUnsignedShort()
                lsb = buf.readUnsignedByte().toInt()
                msb = buf.readUnsignedByte().toInt()
            }
            10 -> logger.debug { "Received: ${buf.readString()}" }
            3 -> buf.readInt()
            4 -> buf.readInt()
            0 -> return@with false
            else -> error("Unrecognized instruction: $instruction")
        }

        return@with true
    }
}