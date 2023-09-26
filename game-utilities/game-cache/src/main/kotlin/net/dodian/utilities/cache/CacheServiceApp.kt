package net.dodian.utilities.cache

import com.displee.cache.CacheLibrary
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.logging.InlineLogger
import net.dodian.utilities.cache.services.CacheService
import net.dodian.utilities.cache.types.*
import java.nio.file.Files
import kotlin.io.path.Path

private val logger = InlineLogger()

val objectMapper = ObjectMapper()
    .findAndRegisterModules()
    .registerKotlinModule()
    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    .enable(SerializationFeature.INDENT_OUTPUT)

fun Map<String, TypeLoader<*>>.dumpToJson(cache: CacheLibrary) {
    this.forEach { (name, loader) ->
        val types = loader.load(cache)
        if (types.isEmpty())
            return@forEach

        objectMapper.writeValue(Path("./data/dumps/config/$name.json").toFile(), types)
        logger.info { "Wrote ${types.size} ${types.first()::class.simpleName}s to file..." }
    }
}

fun main() {
    logger.debug { "Debug logging is enabled!" }
    logger.info { "Starting cache service app..." }
    val service = CacheService("./data/cache_317")
    val cache = service.cache

    logger.info { "Loaded game cache..." }

    cache.index(0).cache()
    logger.info { "Cached up index 0 of the game cache" }

    mapOf(
        "flo" to FloTypeLoader,
        "idk" to IdkTypeLoader,
        "loc" to LocTypeLoader,
        "npc" to NpcTypeLoader,
        "obj" to ObjTypeLoader,
        "seq" to SeqTypeLoader,
        "spotanim" to SpotAnimTypeLoader,
        "varbit" to VarbitTypeLoader,
        "varp" to VarpTypeLoader,
    ).dumpToJson(cache)
}