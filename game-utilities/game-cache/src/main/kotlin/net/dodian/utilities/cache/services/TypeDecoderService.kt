package net.dodian.utilities.cache.services

import com.displee.cache.CacheLibrary
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.readString
import net.dodian.utilities.cache.extensions.toByteBuf
import net.dodian.utilities.cache.objectMapper
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

private val logger = InlineLogger()

class TypeDecoderService(
    private val cache: CacheLibrary,
) {

    fun load(decoder: TypeDecoderDefinition): List<ObjectNode> {
        val types = mutableListOf<ObjectNode>()

        val data = cache.data(decoder.cacheIndex, decoder.cacheArchive, decoder.dataFile)?.toByteBuf()
            ?: error("File not found. (file=${decoder.dataFile}, archive=${decoder.cacheArchive}, index=${decoder.cacheIndex})")

        val meta = if (decoder.metaFile == null) null
        else cache.data(decoder.cacheIndex, decoder.cacheArchive, decoder.metaFile)?.toByteBuf()
            ?: error("File not found. (file=${decoder.metaFile}, archive=${decoder.cacheArchive}, index=${decoder.cacheIndex})")

        val count = meta?.readUnsignedShort() ?: data.readUnsignedShort()
        val indices = IntArray(count)

        if (meta != null) {
            var index = decoder.startPosition
            for (i in 0 until count) {
                indices[i] = index
                index += meta.readShort().toInt()
            }
        }

        for (typeId in 0 until count) {
            if (meta != null) data.readerIndex(indices[typeId])
            types += readType(data, decoder, if (meta == null) null else typeId)
        }

        return types
    }

    private fun readType(
        data: ByteBuf,
        instructions: Map<Int, TypeDecoderInstructions?>,
        typeId: Int? = null
    ): ObjectNode {
        val node = objectMapper.createObjectNode()
        if (typeId != null)
            node.put("id", typeId)

        while (true) {
            val ins = instructions[data.readUnsignedByte().toInt()]
                ?: return node

            val decoder = ins.decoder
            val method = if (ins.unsigned)
                decoder.unsigned ?: decoder.signed
            else decoder.signed

            node.put(ins.fieldName, method.call(data) ?: ins.defaultValue, decoder.type)
        }
    }
}

private fun ObjectNode.put(fieldName: String, value: Any?, type: KClass<*>) {
    if (value == null) return

    val string = value.toString()

    when (type) {
        Int::class -> this.put(fieldName, string.toInt())
        Double::class -> this.put(fieldName, string.toDouble())
        Boolean::class -> this.put(fieldName, string.toBoolean())
        else -> this.put(fieldName, string)
    }
}

class TypeDecoderDefinition(
    val dataFile: String,
    val metaFile: String? = null,
    val cacheIndex: Int,
    val cacheArchive: Int,
    val startPosition: Int = 0,
    val opcodes: MutableMap<Int, TypeDecoderInstructions?> = mutableMapOf()
) : MutableMap<Int, TypeDecoderInstructions?> by opcodes

class TypeDecoderInstructions(
    val fieldName: String,
    val decoder: DecoderMethod,
    val unsigned: Boolean = false,
    val defaultValue: Any? = null
)

enum class DecoderMethod(val type: KClass<*>, val signed: KFunction<*>, val unsigned: KFunction<*>? = null) {
    BOOLEAN(Boolean::class, ByteBuf::readByte, ByteBuf::readUnsignedByte),
    BYTE(Int::class, ByteBuf::readByte, ByteBuf::readUnsignedByte),
    INT(Int::class, ByteBuf::readInt, ByteBuf::readUnsignedInt),
    SHORT(Int::class, ByteBuf::readShort, ByteBuf::readUnsignedShort),
    LONG(Long::class, ByteBuf::readLong),
    STRING(String::class, ByteBuf::readString)
    ;
}