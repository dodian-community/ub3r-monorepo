package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.ConfigType
import net.dodian.utilities.cache.extensions.typeBuffer

private val logger = InlineLogger()

data class IdkType(
    val type: Int,
    val selectable: Boolean,
    val srcColor: List<Int>,
    val dstColor: List<Int>,
    val modelIds: List<Int>,
    val headModelIds: List<Int>
) : Type

data class IdkTypeBuilder(
    var type: Int? = null,
    var selectable: Boolean = false,
    var srcColor: MutableList<Int> = IntArray(6).toMutableList(),
    var dstColor: MutableList<Int> = IntArray(6).toMutableList(),
    var modelIds: MutableList<Int> = mutableListOf(),
    var headModelIds: MutableList<Int> = IntArray(5) { -1 }.toMutableList()
) : TypeBuilder<IdkType> {

    override fun build() = IdkType(
        type = type ?: error("No value set for 'type'"),
        selectable = selectable,
        srcColor = srcColor,
        dstColor = dstColor,
        modelIds = modelIds,
        headModelIds = headModelIds
    )
}

object IdkTypeLoader : TypeLoader<IdkType> {

    override fun load(cache: CacheLibrary): List<IdkType> {
        val types = mutableListOf<IdkType>()

        val data = cache.typeBuffer(ConfigType.Idk)
        val count = data.readUnsignedShort()

        for (i in 0 until count)
            types += readType(data)

        return types
    }

    private fun readType(buf: ByteBuf): IdkType {
        val builder = IdkTypeBuilder()

        var reading = true
        while (reading)
            reading = readBuffer(buf, builder, buf.readUnsignedByte().toInt())

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: IdkTypeBuilder, instruction: Int) = with(builder) {
        //logger.debug { "Decoding instruction: $instruction, for IdkType ${builder.type}" }

        when (instruction) {
            1 -> type = buf.readUnsignedByte().toInt()
            2 -> {
                val count = buf.readUnsignedByte().toInt()
                modelIds = IntArray(count).toMutableList()
                for (model in 0 until count)
                    modelIds[model] = buf.readUnsignedShort()
            }
            3 -> selectable = true
            in 40 until 50 -> srcColor[instruction - 40] = buf.readUnsignedShort()
            in 50 until 60 -> dstColor[instruction - 50] = buf.readUnsignedShort()
            in 60 until 70 -> headModelIds[instruction - 60] = buf.readUnsignedShort()
            0 -> return@with false
            else -> {
                logger.debug { builder }
                error("Unrecognized instruction: $instruction")
            }
        }

        return@with true
    }
}