package net.dodian.utilities.cache.services

import com.displee.cache.CacheLibrary
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.ConfigType
import net.dodian.utilities.cache.extensions.typeBuffer
import net.dodian.utilities.cache.extensions.typeMeta
import net.dodian.utilities.cache.types.Type
import net.dodian.utilities.cache.types.TypeBuilder
import kotlin.reflect.KFunction3

class TypeDecoderService(private val cache: CacheLibrary) {

    fun <T : Type> unpack(type: ConfigType): List<T> {
        val types = mutableListOf<T>()

        val data = cache.typeBuffer(type)
        val meta = if (type.meta != null) cache.typeMeta(type) else null
        val count = when (meta != null) {
            true -> meta.readUnsignedShort()
            false -> data.readUnsignedShort()
        }



        return types
    }

    private fun <T : Type> readType(buf: ByteBuf, callback: KFunction3<ByteBuf, TypeBuilder<T>, Int, Boolean>): T {

    }
}