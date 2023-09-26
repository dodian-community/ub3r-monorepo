package net.dodian.utilities.cache

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.logging.InlineLogger
import net.dodian.utilities.cache.services.CacheService
import net.dodian.utilities.cache.types.*
import kotlin.io.path.Path

private val logger = InlineLogger()

fun main() {
    logger.debug { "Debug logging is enabled!" }
    logger.info { "Starting cache service app..." }
    val service = CacheService()
    val cache = service.cache

    logger.info { "Loaded game cache..." }

    cache.index(0).cache()
    logger.info { "Cached up index 0 of the game cache" }

    val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .registerKotlinModule()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .enable(SerializationFeature.INDENT_OUTPUT)

    println()

    val npcTypes = NpcTypeLoader.load(cache)
    logger.info { "Loaded ${npcTypes.size} NPC types..." }
    objectMapper.writeValue(Path("./data/dumps/config/npc.json").toFile(), npcTypes)
    logger.info { "Wrote ${npcTypes.size} NPC types to file..." }

    println()

    val itemTypes = ObjTypeLoader.load(cache)
    logger.info { "Loaded ${itemTypes.size} Obj types..." }
    objectMapper.writeValue(Path("./data/dumps/config/obj.json").toFile(), itemTypes)
    logger.info { "Wrote ${itemTypes.size} Obj types to file..." }

    //println()

    //val varpTypes = VarpTypeLoader.load(cache)
    //logger.info { "Loaded ${varpTypes.size} varp types..." }
    //objectMapper.writeValue(Path("./data/dumps/config/varp.json").toFile(), varpTypes)
    //logger.info { "Wrote ${varpTypes.size} varp types to file..." }

    println()

    val varbitTypes = VarbitTypeLoader.load(cache)
    logger.info { "Loaded ${varbitTypes.size} VarBit types..." }
    objectMapper.writeValue(Path("./data/dumps/config/varbit.json").toFile(), varbitTypes)
    logger.info { "Wrote ${varbitTypes.size} VarBit types to file..." }

    //println()

    //val spotAnimTypes = SpotAnimTypeLoader.load(cache)
    //logger.info { "Loaded ${spotAnimTypes.size} spotanim types..." }
    //objectMapper.writeValue(Path("./data/dumps/config/spotanim.json").toFile(), spotAnimTypes)
    //logger.info { "Wrote ${spotAnimTypes.size} spotanim types to file..." }

    println()

    val floTypes = FloTypeLoader.load(cache)
    logger.info { "Loaded ${floTypes.size} Flo types..." }
    objectMapper.writeValue(Path("./data/dumps/config/flo.json").toFile(), floTypes)
    logger.info { "Wrote ${floTypes.size} Flo types to file..." }

    println()

    val seqTypes = SeqTypeLoader.load(cache)
    logger.info { "Loaded ${seqTypes.size} Seq types..." }
    objectMapper.writeValue(Path("./data/dumps/config/seq.json").toFile(), seqTypes)
    logger.info { "Wrote ${seqTypes.size} Seq types to file..." }

    println()

    val locTypes = LocTypeLoader.load(cache)
    logger.info { "Loaded ${locTypes.size} Loc types..." }
    objectMapper.writeValue(Path("./data/dumps/config/loc.json").toFile(), locTypes)
    logger.info { "Wrote ${locTypes.size} Loc types to file..." }

    println()

    val idkTypes = IdkTypeLoader.load(cache)
    logger.info { "Loaded ${idkTypes.size} Idk types..." }
    objectMapper.writeValue(Path("./data/dumps/config/idk.json").toFile(), idkTypes)
    logger.info { "Wrote ${idkTypes.size} Idk types to file..." }
}