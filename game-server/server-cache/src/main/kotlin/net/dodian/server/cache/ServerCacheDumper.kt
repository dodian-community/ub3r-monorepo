package net.dodian.server.cache

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import net.dodian.server.cache.extensions.ARCHIVE_CONFIG
import net.dodian.server.cache.types.ItemTypeLoader
import net.dodian.server.cache.types.NpcType
import net.dodian.server.cache.types.NpcTypeBuilder
import net.dodian.server.cache.types.NpcTypeLoader

private val logger = InlineLogger()

class CacheService(
    private val path: String = "./data/cache",
    val cache: CacheLibrary = CacheLibrary(path)
)

fun main() {
    val service = CacheService("./game-server/data/cache")
    val cache = service.cache

    cache.index(0).cache()

    val npcTypes = NpcTypeLoader.load(cache)
    println("Loaded & Decoded ${npcTypes.toList().size} NPC Types!")
    //println("===============================")
    //npcTypes.toList().forEach {
    //    println("${it.id} = ${it.name}")
    //}
    //
    //println()
    //println()

    val itemTypes = ItemTypeLoader.load(cache)
    println("Loaded & Decoded ${itemTypes.size} Item Types!")
    //println("===============================")
    //itemTypes.filter {
    //    it.linkedId == -1
    //}.forEach {
    //    println("${it.id} = ${it.name}")
    //}
}