package net.dodian.uber.game.rscm

import java.io.File
import org.slf4j.LoggerFactory

object RSCM {
    private val logger = LoggerFactory.getLogger(RSCM::class.java)
    private val mappings = mutableMapOf<String, MutableMap<String, Int>>()
    @Volatile private var mappingsLoaded = false

    @JvmStatic
    fun load(directory: File) {
        if (!directory.exists() || !directory.isDirectory) {
            logger.warn("RSCM mappings directory not found or is not a directory: ${directory.absolutePath}")
            return
        }
        mappings.clear()
        loadRscmFiles(directory)
        mappingsLoaded = true
        logger.info(
            "rscm_ready tables={} mappings={}",
            mappings.size,
            mappings.values.sumOf { it.size },
        )
    }

    private fun loadRscmFiles(directory: File) {
        directory.listFiles { f -> f.isFile && f.name.endsWith(".rscm") }?.forEach { file ->
            val type = file.nameWithoutExtension
            val table = mappings.getOrPut(type) { mutableMapOf() }
            var count = 0
            file.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = if (trimmed.contains("=")) trimmed.split("=") else trimmed.split(":")
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().toIntOrNull()
                    if (value != null) {
                        table[key] = value
                        count++
                    }
                }
            }
            logger.debug("Loaded {} RSCM mappings for table '{}'", count, type)
        }
    }

    private fun ensureLoaded() {
        if (mappingsLoaded) return
        synchronized(this) {
            if (mappingsLoaded) return
            val dir = File("content/mappings")
            if (dir.isDirectory) {
                loadRscmFiles(dir)
            }
            mappingsLoaded = true
        }
    }

    @JvmStatic
    fun get(type: String, key: String): Int {
        ensureLoaded()
        val cleanKey = key.removePrefix("$type.")
        return mappings[type]?.get(cleanKey) ?: -1
    }

    @JvmStatic
    fun getReverse(type: String, id: Int): String? {
        ensureLoaded()
        return mappings[type]?.entries?.firstOrNull { it.value == id }?.key
    }
}

fun String.asRscm(): Int {
    val dotIndex = this.indexOf('.')
    if (dotIndex == -1) return -1
    val type = this.substring(0, dotIndex)
    val key = this.substring(dotIndex + 1)
    return RSCM.get(type, key)
}

fun String.asRscm(type: String): Int = RSCM.get(type, this)
fun String.asRscmNpc(): Int = RSCM.get("npc", this)
fun String.asRscmObj(): Int = RSCM.get("obj", this)
fun String.asRscmLoc(): Int = RSCM.get("loc", this)
fun String.asRscmSpotAnim(): Int = RSCM.get("spotanim", this)
fun String.asRscmSeq(): Int = RSCM.get("seq", this)
fun String.asRscmProjAnim(): Int = RSCM.get("projanim", this)
fun String.asRscmInterface(): Int = RSCM.get("interface", this)
fun String.asRscmComponent(): Int = RSCM.get("component", this)



