package net.dodian.utilities.cache

import com.displee.cache.CacheLibrary
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.logging.InlineLogger
import net.dodian.utilities.cache.extensions.ARCHIVE_CONFIG
import net.dodian.utilities.cache.services.*
import net.dodian.utilities.cache.types.*
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

    val decoderService = TypeDecoderService(cache)
    val types = decoderService.load(TypeDecoderDefinition(
        dataFile = "obj.dat",
        metaFile = "obj.idx",
        cacheIndex = 0,
        cacheArchive = ARCHIVE_CONFIG,
        startPosition = ARCHIVE_CONFIG,
        opcodes = mutableMapOf(
            0 to null,
            1 to TypeDecoderInstructions("modelId", DecoderMethod.SHORT, unsigned = true),
            2 to TypeDecoderInstructions("name", DecoderMethod.STRING),
            3 to TypeDecoderInstructions("examine", DecoderMethod.STRING, defaultValue = "I don't know anything about this item."),
            4 to TypeDecoderInstructions("iconZoom", DecoderMethod.INT, defaultValue = 2_000),
            5 to TypeDecoderInstructions("iconPitch", DecoderMethod.INT, defaultValue = 0),
            6 to TypeDecoderInstructions("iconYaw", DecoderMethod.INT, defaultValue = 0),
        )
    ))

    objectMapper.writeValue(Path("./data/dumps/config/obj_test.json").toFile(), types)
}