package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.ConfigType
import net.dodian.utilities.cache.extensions.typeBuffer

private val logger = InlineLogger()

data class SpotAnimType(
    val id: Int,
    val modelId: Int,
    val scaleXY: Int,
    val scaleZ: Int,
    val rotation: Int,
    val lightAmbient: Int,
    val lightAttenuation: Int,
    val seqId: Int = -1,
    val colorSrc: List<Int> = emptyList(),
    val colorDst: List<Int> = emptyList()
) : Type

data class SpotAnimTypeBuilder(
    var id: Int? = null,
    var modelId: Int? = null,
    var scaleXY: Int = 128,
    var scaleZ: Int = 128,
    var rotation: Int = 0,
    var lightAmbient: Int = 0,
    var lightAttenuation: Int = 0,
    var seqId: Int = -1,
    var colorSrc: MutableList<Int> = IntArray(6).toMutableList(),
    var colorDst: MutableList<Int> = IntArray(6).toMutableList()
) : TypeBuilder<SpotAnimType> {

    override fun build() = SpotAnimType(
        id = id ?: error("No value set for 'id'"),
        modelId = modelId ?: error("No value set for 'modelId'"),
        rotation = rotation,
        lightAmbient = lightAmbient,
        lightAttenuation = lightAttenuation,
        scaleXY = scaleXY,
        scaleZ = scaleZ,
        seqId = seqId,
        colorSrc = colorSrc,
        colorDst = colorDst
    )
}

object SpotAnimTypeLoader : TypeLoader<SpotAnimType> {

    override fun load(cache: CacheLibrary): List<SpotAnimType> {
        val types = mutableListOf<SpotAnimType>()

        val data = cache.typeBuffer(ConfigType.SpotAnim)
        val count = data.readUnsignedShort()

        logger.info { "Loading $count SpotAnimTypes..." }

        for (i in 0 until count)
            types += readType(data, i)

        logger.info { "Loaded $count SpotAnimTypes..." }
        println()
        return types
    }

    private fun readType(buf: ByteBuf, id: Int): SpotAnimType {
        val builder = SpotAnimTypeBuilder(id)

        var reading = true
        while (reading)
            reading = readBuffer(buf, builder, buf.readUnsignedByte().toInt())

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: SpotAnimTypeBuilder, instruction: Int) = with(builder) {
        when (instruction) {
            1 -> modelId = buf.readUnsignedShort()
            2 -> seqId = buf.readUnsignedShort()
            4 -> scaleXY = buf.readUnsignedShort()
            5 -> scaleZ = buf.readUnsignedShort()
            6 -> rotation = buf.readUnsignedShort()
            7 -> lightAmbient = buf.readUnsignedByte().toInt()
            8 -> lightAttenuation = buf.readUnsignedByte().toInt()
            in 40 until 50 -> colorSrc[instruction - 40] = buf.readUnsignedShort()
            in 50 until 60 -> colorDst[instruction - 50] = buf.readUnsignedShort()
            0 -> return@with false
            else -> {
                logger.debug { builder }
                error("Unrecognized instruction: $instruction")
            }
        }

        return@with true
    }
}