package net.dodian.uber.game.engine.systems.objectexamines

import com.google.gson.GsonBuilder
import net.dodian.uber.game.engine.config.ServerPaths
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

data class ObjectExamine(
    val id: Int,
    val examine: String
)

object ObjectExamines {
    private val logger = LoggerFactory.getLogger(ObjectExamines::class.java)
    private val examinesMap = ConcurrentHashMap<Int, String>()

    @JvmStatic
    fun load() {
        val file = ServerPaths.definition("objects", "object_examines.json").toFile()
        if (!file.exists()) {
            logger.warn("Object examines file not found at: {}", file.absolutePath)
            return
        }
        try {
            val gson = GsonBuilder().create()
            val examinesArray = gson.fromJson(file.readText(), Array<ObjectExamine>::class.java)
            examinesMap.clear()
            val duplicateIds = examinesArray.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys.sorted()
            if (duplicateIds.isNotEmpty()) {
                logger.warn(
                    "Object examine data contains duplicate IDs; preserving existing last-entry-wins behavior for: {}",
                    duplicateIds.joinToString(),
                )
            }
            for (entry in examinesArray) {
                val processedExamine = entry.examine.replace("%SERVER_NAME%", "Dodian")
                examinesMap[entry.id] = processedExamine
            }
            logger.info("Loaded {} object examines.", examinesMap.size)
        } catch (e: Exception) {
            logger.error("Failed to load object examines", e)
        }
    }

    @JvmStatic
    fun getExamine(id: Int): String? {
        return examinesMap[id]
    }
}
