package net.dodian.uber.game.engine.systems.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.dodian.uber.game.rscm.Namer
import java.io.File
import java.security.MessageDigest

/**
 * Generates developer-reference mappings from a pinned OSRS dump.  The output is
 * intentionally not read by the game server: gameplay owns raw cache IDs.
 */
object RscmGenerator {
    private val configNamespaces = linkedMapOf(
        "loc" to "loc", "obj" to "obj", "npc" to "npc", "seq" to "seq",
        "spot" to "spotanim", "varbit" to "varbit", "varp" to "varp",
    )
    private val json = Gson()
    private val prettyJson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    fun main(args: Array<String>) {
        val options = parseArgs(args)
        val dump = File(options.getValue("dump"))
        val output = File(options["output"] ?: defaultOutput().path)
        val sourceRevision = options["source-revision"] ?: dump.name.substringAfterLast('-')
        require(dump.isDirectory) { "OSRS dump directory does not exist: ${dump.absolutePath}" }

        val records = mutableMapOf<String, List<ReferenceRecord>>()
        val consumed = mutableListOf<File>()
        configNamespaces.forEach { (dumpType, namespace) ->
            val source = File(dump, "config/dump.$dumpType")
            require(source.isFile) { "Required dump source is missing: ${source.path}" }
            consumed += source
            records[namespace] = parseConfig(source, namespace)
        }
        listOf("interface" to "interface", "component" to "component").forEach { (symbolType, namespace) ->
            val source = File(dump, "symbols/$symbolType.sym")
            require(source.isFile) { "Required dump symbol file is missing: ${source.path}" }
            consumed += source
            records[namespace] = if (namespace == "component") parseComponents(source) else parseSymbols(source, namespace)
        }

        options["cache"]?.let { cachePath -> verifyAgainstCache(File(cachePath), records) }
        val rendered = render(records, sourceRevision, consumed)
        if (options.containsKey("check")) {
            val differences = rendered.filter { (relative, content) ->
                val target = File(output, relative)
                !target.isFile || target.readText() != content
            }.keys
            check(differences.isEmpty()) { "Generated reference mappings differ: ${differences.sorted().joinToString()}" }
            println("Cache reference mappings are current under ${output.absolutePath}")
            return
        }
        rendered.forEach { (relative, content) ->
            val target = File(output, relative)
            target.parentFile.mkdirs()
            target.writeText(content)
        }
        println("Generated ${records.values.sumOf { it.size }} reference records under ${output.absolutePath}")
    }

    private fun defaultOutput(): File =
        if (File("game-server/content/mappings").isDirectory) File("game-server/content/mappings") else File("content/mappings")

    private fun parseArgs(args: Array<String>): Map<String, String> {
        val options = linkedMapOf<String, String>()
        var index = 0
        while (index < args.size) {
            val option = args[index]
            require(option.startsWith("--")) { "Unexpected argument: $option" }
            val key = option.removePrefix("--")
            if (key == "check") options[key] = "true"
            else {
                val value = args.getOrNull(++index) ?: error("Missing value for --$key")
                options[key] = value
            }
            index++
        }
        require("dump" in options) { "Usage: --dump <path> [--output <path>] [--cache <path>] [--source-revision <id>] [--check]" }
        return options
    }

    private fun parseConfig(file: File, namespace: String): List<ReferenceRecord> {
        val records = mutableListOf<ReferenceRecord>()
        var currentId: Int? = null
        var currentSymbol = ""
        var properties = linkedMapOf<String, MutableList<String>>()
        fun flush() {
            val id = currentId ?: return
            records += ReferenceRecord(namespace, id, currentSymbol, properties.mapValues { it.value.toList() })
            properties = linkedMapOf()
        }
        file.forEachLine { raw ->
            val line = raw.trim()
            if (line.startsWith("[") && line.endsWith("]")) {
                flush()
                currentSymbol = line.removeSurrounding("[", "]")
                currentId = currentSymbol.substringAfterLast('_').toIntOrNull()
                require(currentId != null) { "Could not read ID from $currentSymbol in ${file.name}" }
            } else if (currentId != null && line.isNotBlank() && !line.startsWith("//")) {
                val separator = line.indexOf('=')
                if (separator > 0) properties.getOrPut(line.substring(0, separator)) { mutableListOf() } += line.substring(separator + 1)
            }
        }
        flush()
        return records.sortedBy { it.id }
    }

    private fun parseSymbols(file: File, namespace: String): List<ReferenceRecord> =
        file.readLines().mapNotNull { line ->
            val parts = line.split('\t', limit = 2)
            val id = parts.firstOrNull()?.toIntOrNull() ?: return@mapNotNull null
            ReferenceRecord(namespace, id, parts.getOrElse(1) { "${namespace}_$id" }, emptyMap())
        }.sortedBy { it.id }

    /** Components are symbolized as `parent:child`; use the cache's packed ID for reference. */
    private fun parseComponents(file: File): List<ReferenceRecord> =
        file.readLines().mapNotNull { line ->
            val parts = line.split('\t', limit = 2)
            val coordinates = parts.firstOrNull()?.split(':', limit = 2) ?: return@mapNotNull null
            val parent = coordinates.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val child = coordinates.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val packedId = (parent shl 16) or child
            ReferenceRecord(
                namespace = "component",
                id = packedId,
                symbol = parts.getOrElse(1) { "interface_${parent}:component_$child" },
                properties = mapOf("parent" to listOf(parent.toString()), "child" to listOf(child.toString())),
            )
        }.sortedBy { it.id }

    private fun render(
        input: Map<String, List<ReferenceRecord>>,
        sourceRevision: String,
        consumed: List<File>,
    ): Map<String, String> {
        val rendered = linkedMapOf<String, String>()
        val resolved = input.mapValues { (_, records) -> resolveKeys(records) }
        resolved.forEach { (namespace, records) ->
            rendered["$namespace.rscm"] = records.sortedBy { it.key }.joinToString("\n", postfix = "\n") { "${it.key}=${it.id}" }
            rendered["metadata/$namespace.jsonl"] = records.sortedBy { it.id }.joinToString("\n", postfix = "\n") { record ->
                json.toJson(record.toJson())
            }
        }
        val manifest = JsonObject().apply {
            addProperty("sourceRevision", sourceRevision)
            addProperty("format", "cache-reference-v1")
            add("sources", JsonArray().also { array -> consumed.sortedBy { it.path }.forEach { file ->
                array.add(JsonObject().apply { addProperty("path", file.name); addProperty("sha256", sha256(file)) })
            } })
            add("namespaces", JsonObject().also { namespaces -> resolved.toSortedMap().forEach { (name, records) ->
                namespaces.addProperty(name, records.size)
            } })
        }
        rendered["metadata/manifest.json"] = prettyJson.toJson(manifest) + "\n"
        return rendered
    }

    private fun resolveKeys(records: List<ReferenceRecord>): List<ResolvedReferenceRecord> {
        val candidates = records.map { record ->
            val decodedName = record.properties["name"]?.lastOrNull()?.takeIf { it.isNotBlank() }
            val genericSymbol = "${record.namespace}_${record.id}"
            val preferred = decodedName ?: record.symbol.takeIf { it.isNotBlank() && it != genericSymbol } ?: genericSymbol
            record to Namer.sanitizeRSCM(preferred).ifBlank { genericSymbol }
        }
        val result = mutableListOf<ResolvedReferenceRecord>()
        candidates.groupBy { it.second }.toSortedMap().forEach { (base, group) ->
            group.sortedBy { it.first.id }.forEachIndexed { index, (record, _) ->
                result += ResolvedReferenceRecord(record, if (index == 0) base else "${base}_${record.id}")
            }
        }
        return result
    }

    private fun verifyAgainstCache(cacheDir: File, records: Map<String, List<ReferenceRecord>>) {
        require(cacheDir.isDirectory) { "Cache directory does not exist: ${cacheDir.absolutePath}" }
        CacheStore(cacheDir.toPath()).open().use { store ->
            fun verify(namespace: String, actual: Set<Int>) {
                val expected = records.getValue(namespace).map { it.id }.toSet()
                // Cache decoders intentionally omit empty/null definitions. The dump is allowed
                // to contain those placeholders, but may never omit a decoded live definition.
                check(actual.all { it in expected }) {
                    "$namespace dump is missing packed-cache IDs: ${(actual - expected).sorted().joinToString()}"
                }
            }
            verify("loc", ObjectDefinitionDecoder.decode(store).definitions.keys)
            verify("npc", NpcCacheDefinitionDecoder.decode(store).keys)
            verify("seq", AnimationDefinitionDecoder.decode(store).keys)
            verify("spotanim", SpotAnimDefinitionDecoder.decode(store).keys)
            // The server cache has an expanded varbit table beyond this pinned dump;
            // retain it as reference data without treating that known extension as a
            // dump/cache revision failure.
            val dumpVarbits = records.getValue("varbit").size
            val cacheVarbits = VarbitDefinitionDecoder.decode(store).size
            if (dumpVarbits != cacheVarbits) println("varbit reference differs from packed cache: dump=$dumpVarbits cache=$cacheVarbits")
        }
    }

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    private data class ReferenceRecord(val namespace: String, val id: Int, val symbol: String, val properties: Map<String, List<String>>) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("namespace", namespace); addProperty("id", id); addProperty("key", "")
            addProperty("symbol", symbol)
            properties["name"]?.lastOrNull()?.let { addProperty("name", it) }
            add("properties", JsonObject().also { propertyObject -> properties.forEach { (key, values) ->
                if (values.size == 1) propertyObject.addProperty(key, values.single())
                else propertyObject.add(key, JsonArray().also { values.forEach(it::add) })
            } })
        }
    }

    private data class ResolvedReferenceRecord(val record: ReferenceRecord, val key: String) {
        val id get() = record.id
        fun toJson(): JsonObject = record.toJson().also { it.addProperty("key", key) }
    }
}
