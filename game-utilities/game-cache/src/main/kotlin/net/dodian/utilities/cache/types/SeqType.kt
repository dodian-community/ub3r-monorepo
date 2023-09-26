package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.ConfigType
import net.dodian.utilities.cache.extensions.typeBuffer

private val logger = InlineLogger()

data class SeqType(
    val frameCount: Int,
    val transformIds: List<Int>,
    val auxiliaryTransformIds: List<Int>,
    val frameDuration: List<Int>,
    val loopFrameCount: Int = -1,
    val mask: List<Int>,
    val forwardRenderPadding: Boolean = false,
    val priority: Int = 5,
    val rightHandOverride: Int = -1,
    val leftHandOverride: Int = -1,
    val loopCount: Int = 99,
    val moveStyle: Int = -1,
    val idleStyle: Int = -1,
    val replayStyle: Int = 1
) : Type

data class SeqTypeBuilder(
    var frameCount: Int? = null,
    var transformIds: MutableList<Int> = mutableListOf(),
    var auxiliaryTransformIds: MutableList<Int> = mutableListOf(),
    var frameDuration: MutableList<Int> = mutableListOf(),
    var loopFrameCount: Int = -1,
    var mask: MutableList<Int> = mutableListOf(),
    var forwardRenderPadding: Boolean = false,
    var priority: Int = 5,
    var rightHandOverride: Int = -1,
    var leftHandOverride: Int = -1,
    var loopCount: Int = 99,
    var moveStyle: Int = -1,
    var idleStyle: Int = -1,
    var replayStyle: Int = 1
) : TypeBuilder<SeqType> {

    override fun build() = SeqType(
        frameCount = frameCount ?: error("No value set for 'frameCount'"),
        transformIds = transformIds,
        auxiliaryTransformIds = auxiliaryTransformIds,
        frameDuration = frameDuration,
        loopFrameCount = loopFrameCount,
        mask = mask,
        forwardRenderPadding = forwardRenderPadding,
        priority = priority,
        rightHandOverride = rightHandOverride,
        leftHandOverride = leftHandOverride,
        loopCount = loopCount,
        moveStyle = moveStyle,
        idleStyle = idleStyle,
        replayStyle = replayStyle
    )
}

object SeqTypeLoader : TypeLoader<SeqType> {

    override fun load(cache: CacheLibrary): List<SeqType> {
        val types = mutableListOf<SeqType>()

        val data = cache.typeBuffer(ConfigType.Seq)
        val count = data.readUnsignedShort()

        for (i in 0 until count)
            types += readType(data)

        return types
    }

    private fun readType(buf: ByteBuf): SeqType {
        val builder = SeqTypeBuilder()

        var reading = true
        while (reading)
            reading = readBuffer(buf, builder, buf.readUnsignedByte().toInt())

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: SeqTypeBuilder, instruction: Int) = with(builder) {
        //logger.debug { "Decoding instruction: $instruction, for SeqType" }

        when (instruction) {
            1 -> {
                val count = buf.readUnsignedByte().toInt()
                frameCount = count
                transformIds = IntArray(count).toMutableList()
                auxiliaryTransformIds = IntArray(count).toMutableList()
                frameDuration = IntArray(count).toMutableList()
                for (f in 0 until count) {
                    transformIds[f] = buf.readUnsignedShort()
                    auxiliaryTransformIds[f] = buf.readUnsignedShort()
                    if (auxiliaryTransformIds[f] == 65535)
                        auxiliaryTransformIds[f] = -1

                    frameDuration[f] = buf.readUnsignedShort()
                }
            }
            2 -> loopFrameCount = buf.readUnsignedShort()
            3 -> {
                val count = buf.readUnsignedByte().toInt()
                mask = IntArray(count + 1).toMutableList()

                for (m in 0 until count)
                    mask[m] = buf.readUnsignedByte().toInt()

                mask[count] = 9999999
            }
            4 -> forwardRenderPadding = true
            5 -> priority = buf.readUnsignedByte().toInt()
            6 -> rightHandOverride = buf.readUnsignedShort()
            7 -> leftHandOverride = buf.readUnsignedShort()
            8 -> loopCount = buf.readUnsignedByte().toInt()
            9 -> moveStyle = buf.readUnsignedByte().toInt()
            10 -> idleStyle = buf.readUnsignedByte().toInt()
            11 -> replayStyle = buf.readUnsignedByte().toInt()
            12 -> logger.debug { "Instruction 12 = ${buf.readInt()}" }
            0 -> return@with false
            else -> {
                logger.debug { builder }
                error("Unrecognized instruction: $instruction")
            }
        }

        return@with true
    }
}