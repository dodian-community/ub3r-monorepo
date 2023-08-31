package net.dodian.server.cache.types

import com.displee.cache.CacheLibrary
import io.netty.buffer.ByteBuf

data class NpcType(
    val id: Int,
    val name: String,
    val examine: String,
    val options: List<String>,
    val size: Int,
    val modelIds: List<Int>
) : Type

data class NpcTypeBuilder(
    var id: Int,
    var name: String? = null,
    var examine: String? = null,
    var options: List<String>? = null,
    var size: Int? = null,
    var modelIds: List<Int>? = null
) : TypeBuilder<NpcType> {

    override fun build() = NpcType(
        id = id,
        name = name ?: "N/A",
        examine = examine ?: "N/A",
        options = options ?: emptyList(),
        size = size ?: 1,
        modelIds = modelIds ?: emptyList()
    )
}

class NpcTypeLoader(private val cache: CacheLibrary) : Type {

    fun unpack() {

    }

    fun read(data: ByteBuf, id: Int) {

    }
}