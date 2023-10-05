package net.dodian.utilities.cache.model

import com.displee.compress.CompressionType
import com.displee.compress.decompress
import com.github.michaelbull.logging.InlineLogger
import net.dodian.utilities.cache.model.decoders.ModelDecoder
import net.dodian.utilities.cache.objectMapper
import net.dodian.utilities.cache.services.CacheService
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

private val logger = InlineLogger()

class ModelDumper(private val cacheService: CacheService) {

    fun dumpModels() {
        val cache = cacheService.cache

        val dumpsDir = Path(cache.path).resolve("dumps").resolve("models")
        if (dumpsDir.notExists())
            dumpsDir.createDirectories()

        val ifType = cacheService.ifTypes.filter { it.modelId != -1 }.first()
        val item = cacheService.objTypes.single { it.id == 4151 }
        val modelFile = cache.index(1).readArchiveSector(ifType.modelId) ?: error("Nooo :(")

        //val modelData = if (modelFile.compressionType != CompressionType.NONE)
        //    modelFile.decompress()
        //else modelFile.data

        val model = ModelDecoder(modelFile.data).decode(ifType.modelId)
        objectMapper.writeValue(dumpsDir.resolve("${model.modelId}.json").toFile(), model)
    }
}

fun main() {
    ModelDumper(CacheService("./dodian-backend/dodian-server/data/cache")).dumpModels()
}