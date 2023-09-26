package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.ConfigType
import net.dodian.utilities.cache.extensions.readString
import net.dodian.utilities.cache.extensions.typeBuffer

private val logger = InlineLogger()

data class VarpType(
    val type: Int
) : Type

data class VarpTypeBuilder(
    var type: Int = 0
) : TypeBuilder<VarpType> {

    override fun build() = VarpType(type)
}

object VarpTypeLoader {

    fun load(cache: CacheLibrary): List<VarpType> {
        val types = mutableListOf<VarpType>()

        val data = cache.typeBuffer(ConfigType.Varp)
        val count = data.readUnsignedShort()

        for (i in 0 until count) {
            types += readType(data)
        }

        return types
    }

    private fun readType(buf: ByteBuf): VarpType {
        val builder = VarpTypeBuilder()

        var reading = true
        while (reading)
            reading = readBuffer(buf, builder, buf.readUnsignedByte().toInt())

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: VarpTypeBuilder, instruction: Int) = with(builder) {
        when (instruction) {
            1, 2 -> buf.readUnsignedByte()
            5 -> type = buf.readUnsignedShort()
            7, 12 -> buf.readInt()
            10 -> logger.debug { "Received: ${buf.readString()}" }
            0 -> return@with false
            else -> error("Unrecognized instruction: $instruction")
        }

        return@with true
    }
}