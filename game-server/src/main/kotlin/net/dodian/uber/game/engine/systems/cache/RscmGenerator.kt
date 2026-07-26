package net.dodian.uber.game.engine.systems.cache

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.dodian.uber.game.rscm.Namer
import java.io.File
import java.io.FileReader
import java.nio.file.Path

object RscmGenerator {

    @JvmStatic
    fun main(args: Array<String>) {
        val locOnly = args.contains("--loc-only")
        var outputDir = File("content/mappings")
        if (File("../settings.gradle.kts").exists()) {
            outputDir = File("../game-server/content/mappings")
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val gson = Gson()
        val cachePath = "data/cache"
        val cacheDir = File(cachePath).let { if (it.exists()) it else File("game-server/$cachePath") }
        if (!cacheDir.isDirectory) {
            println("Cache directory not found: ${cacheDir.absolutePath}")
            return
        }

        println("Opening cache at ${cacheDir.absolutePath}...")
        val store = CacheStore(cacheDir.toPath()).open()

        writeRscm(outputDir, "obj", buildItemEntries(gson))

        println("Decoding NPC definitions...")
        val npcDefs = NpcCacheDefinitionDecoder.decode(store)
        writeRscm(outputDir, "npc", buildNpcLines(npcDefs.values
            .sortedWith(compareByDescending<CacheNpcDefinition> { it.actions.filterNotNull().size }
                .thenByDescending { it.id })
            .toList()))

        println("Decoding Object definitions...")
        val objDefs = ObjectDefinitionDecoder.decode(store).definitions
        val alterObjectNames = loadAlterObjectNames(System.getProperty("alter.objs.path")?.let(::File))
        writeRscm(outputDir, "loc", buildLocLines(objDefs, outputDir, alterObjectNames))
        if (locOnly) {
            println("Successfully generated loc.rscm under ${outputDir.absolutePath}!")
            store.close()
            return
        }

        println("Decoding SpotAnim definitions...")
        val spotAnimDefs = SpotAnimDefinitionDecoder.decode(store)
        val spotAnimSymFile = File("/Users/tylercovalt/Desktop/rsmod-main/.data/symbols/spotanim.sym")
        val spotAnimNames = parseSymFile(spotAnimSymFile)
        println("Loaded ${spotAnimNames.size} spotanim names from ${spotAnimSymFile.path}")
        writeRscm(outputDir, "spotanim", buildLines(
            spotAnimDefs.keys.map { id ->
                val name = spotAnimNames[id] ?: "spotanim_$id"
                name to id
            }
        ))

        println("Reading Projectile definitions...")
        val projSymFile = File("/Users/tylercovalt/Desktop/rsmod-main/.data/symbols/projanim.sym")
        val projNames = parseSymFile(projSymFile)
        println("Loaded ${projNames.size} projanim names from ${projSymFile.path}")
        writeRscm(outputDir, "projanim", buildLines(projNames.map { (id, name) -> name to id }))

        println("Decoding Sequence definitions...")
        val seqDefs = AnimationDefinitionDecoder.decode(store)
        val seqSymFile = File("/Users/tylercovalt/Desktop/rsmod-main/.data/symbols/seq.sym")
        val seqNames = parseSymFile(seqSymFile)
        println("Loaded ${seqNames.size} seq names from ${seqSymFile.path}")
        val maxSeqId = 10677
        writeRscm(outputDir, "seq", buildLines(
            seqDefs.keys.filter { it <= maxSeqId }.map { id ->
                val name = seqNames[id] ?: "seq_$id"
                name to id
            }
        ))

        val voidPsDir = File("/Users/tylercovalt/Desktop/RSPS/game-server-main/data")
        val varps = mutableMapOf<String, Int>()
        if (voidPsDir.isDirectory) {
            println("Scanning VoidPS Varp TOML files...")
            voidPsDir.walkTopDown().filter { it.isFile && it.name.endsWith(".varps.toml") }.forEach { file ->
                parseSectionIdToml(file).forEach { (name, id) -> varps[name] = id }
            }
            writeRscm(outputDir, "varp", buildLines(varps.map { it.key to it.value }))
        }

        val varbits = mutableMapOf<String, Int>()
        if (voidPsDir.isDirectory) {
            println("Scanning VoidPS Varbit TOML files...")
            voidPsDir.walkTopDown().filter { it.isFile && it.name.endsWith(".varbits.toml") }.forEach { file ->
                parseSectionIdToml(file).forEach { (name, id) -> varbits[name] = id }
            }
            writeRscm(outputDir, "varbit", buildLines(varbits.map { it.key to it.value }))
        }

        val ifaceNamesFile = File("/Users/tylercovalt/Desktop/RSPS/377.txt")
        if (ifaceNamesFile.exists()) {
            println("Generating Interface mappings from cache and 377 names...")
            val (ifaceLines, compLines) = buildInterfaceLines(store, ifaceNamesFile)
            writeRscm(outputDir, "interface", ifaceLines)
            writeRscm(outputDir, "component", compLines)
        } else {
            println("Warning: Interface names file not found at ${ifaceNamesFile.path}")
        }

        println("Successfully generated RSCM mapping files under ${outputDir.absolutePath}!")
    }

    private fun buildNpcLines(defs: List<CacheNpcDefinition>): List<String> {
        val existingKeys = mutableSetOf<String>()
        val lines = mutableListOf<String>()
        for (def in defs) {
            val key = generateKey(def.name, def.id, existingKeys)
            val actions = def.actions.filterNotNull()
            if (actions.isNotEmpty()) {
                lines.add("# ${actions.size} actions: ${actions.joinToString(", ", "\"", "\"")}")
            } else {
                lines.add("# 0 actions")
            }
            lines.add("$key=${def.id}")
        }
        return lines
    }

    private fun buildLines(rawEntries: List<Pair<String, Int>>): List<String> {
        val existingKeys = mutableSetOf<String>()
        return rawEntries.map { (rawName, id) ->
            val key = generateKey(rawName, id, existingKeys)
            "$key=$id"
        }
    }

    /**
     * Produces a complete cache-backed loc mapping while allowing a compatible
     * Alter object table to improve names for IDs it knows about. The override
     * source is intentionally optional: cache IDs and existing current-only
     * keys always survive generation.
     */
    private fun buildLocLines(
        definitions: Map<Int, net.dodian.cache.objects.GameObjectData>,
        outputDir: File,
        alterNames: Map<Int, String>,
    ): List<String> {
        val existingNames = loadMappingsById(File(outputDir, "loc.rscm"))
        data class Candidate(val id: Int, val key: String, val fromAlter: Boolean)

        val candidates = definitions.values
            .sortedBy { it.id }
            .map { definition ->
                val alterName = alterNames[definition.id]
                Candidate(
                    id = definition.id,
                    key = alterName ?: existingNames[definition.id] ?: normalizedKey(definition.name),
                    fromAlter = alterName != null,
                )
            }

        val resolved = LinkedHashMap<Int, String>(candidates.size)
        val used = mutableSetOf<String>()
        candidates.groupBy { it.key }.toSortedMap().forEach { (key, group) ->
            val preferred = group.firstOrNull { it.fromAlter } ?: group.minBy { it.id }
            resolved[preferred.id] = key
            used += key
            group.filterNot { it === preferred }.sortedBy { it.id }.forEach { candidate ->
                var resolvedKey = "${key}_${candidate.id}"
                var suffix = 2
                while (!used.add(resolvedKey)) {
                    resolvedKey = "${key}_${candidate.id}_$suffix"
                    suffix++
                }
                resolved[candidate.id] = resolvedKey
            }
        }
        return resolved.entries.sortedBy { it.key }.map { (id, key) -> "$key=$id" }
    }

    private fun loadAlterObjectNames(file: File?): Map<Int, String> {
        if (file == null || !file.isFile) {
            println("No Alter Objs.kt supplied; retaining cache loc names.")
            return emptyMap()
        }
        val names = LinkedHashMap<Int, String>()
        file.forEachLine { line ->
            val declaration = line.trim()
            if (!declaration.startsWith("const val ")) return@forEachLine
            val parts = declaration.removePrefix("const val ").split(" = ", limit = 2)
            val id = parts.getOrNull(1)?.toIntOrNull() ?: return@forEachLine
            val name = parts[0].takeIf { it.isNotBlank() } ?: return@forEachLine
            names[id] = name.lowercase()
        }
        println("Loaded ${names.size} Alter object names from ${file.path}")
        return names
    }

    private fun loadMappingsById(file: File): Map<Int, String> {
        if (!file.isFile) return emptyMap()
        val result = HashMap<Int, String>()
        file.forEachLine { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) return@forEachLine
            val id = line.substring(separator + 1).trim().toIntOrNull() ?: return@forEachLine
            result[id] = line.substring(0, separator).trim()
        }
        return result
    }

    private fun normalizedKey(name: String): String =
        Namer.sanitizeRSCM(name).ifEmpty { "unnamed" }

    private fun generateKey(name: String, id: Int, existing: MutableSet<String>): String {
        val base = Namer.sanitizeRSCM(name)
        val key = if (base.isEmpty()) "unnamed" else base
        if (existing.add(key)) {
            return key
        }
        val fallbackKey = "${key}_$id"
        existing.add(fallbackKey)
        return fallbackKey
    }

    private fun writeRscm(dir: File, name: String, lines: List<String>) {
        val file = File(dir, "$name.rscm")
        file.printWriter().use { writer ->
            lines.forEach { writer.println(it) }
        }
        val entryCount = lines.count { !it.startsWith("#") }
        println("Generated $entryCount mappings in ${file.name}")
    }

    private fun buildItemEntries(gson: Gson): List<String> {
        var sourceFile = File("content/items/item_definitions.json")
        if (!sourceFile.exists()) {
            sourceFile = File("game-server/content/items/item_definitions.json")
        }
        if (!sourceFile.exists()) {
            println("Warning: Item definitions not found")
            return emptyList()
        }

        val existingKeys = mutableSetOf<String>()
        val lines = mutableListOf<String>()

        FileReader(sourceFile).use { reader ->
            val jsonArray = gson.fromJson(reader, JsonArray::class.java)
            for (element in jsonArray) {
                if (element is JsonObject) {
                    val id = element.get("id")?.asInt ?: continue
                    val name = element.get("name")?.asString ?: "unnamed"
                    val key = generateKey(name, id, existingKeys)
                    lines.add("$key=$id")
                }
            }
        }

        return lines
    }

    private fun buildInterfaceLines(store: CacheStore, namesFile: File): Pair<List<String>, List<String>> {
        val interfaceMap = mutableMapOf<String, Int>()
        val componentMap = mutableMapOf<String, Int>()
        val interfaceExistingKeys = mutableSetOf<String>()
        val componentExistingKeys = mutableSetOf<String>()

        val interfaces = InterfaceDefinitionDecoder.decode(store)
        if (interfaces.isEmpty()) {
            println("Warning: No interface definitions decoded from cache.")
            return emptyList<String>() to emptyList()
        }

        val cleanInterfaceNames = parse377InterfaceNames(namesFile)
        println("Loaded ${cleanInterfaceNames.size} interface names from ${namesFile.path}")

        fun getRootInterfaceId(id: Int): Int {
            var current = interfaces[id] ?: return id
            var safety = 0
            while (current.parentId != -1 && safety < 100) {
                val parent = interfaces[current.parentId] ?: break
                current = parent
                safety++
            }
            return current.id
        }

        val rootIdToName = mutableMapOf<Int, String>()

        for ((id, rawName) in cleanInterfaceNames.entries.sortedBy { it.key }) {
            val key = generateKey(rawName, id, interfaceExistingKeys)
            interfaceMap[key] = id
            rootIdToName[id] = key
        }

        val cacheRootIds = interfaces.values.filter { it.parentId == -1 }.map { it.id }.sorted()
        for (rootId in cacheRootIds) {
            if (rootIdToName.containsKey(rootId)) continue

            val rootDef = interfaces[rootId]
            var nameToUse = rootDef?.disabledMessage?.trim() ?: ""
            if (nameToUse.isEmpty()) {
                nameToUse = interfaces.values
                    .filter { getRootInterfaceId(it.id) == rootId && it.type == 4 && it.disabledMessage.trim().isNotEmpty() }
                    .minByOrNull { it.id }
                    ?.disabledMessage?.trim() ?: ""
            }

            val key = if (nameToUse.isNotEmpty()) {
                generateKey(nameToUse, rootId, interfaceExistingKeys)
            } else {
                "interface_$rootId"
            }
            interfaceMap[key] = rootId
            rootIdToName[rootId] = key
        }

        val childDefs = interfaces.values.filter { it.parentId != -1 }.sortedBy { it.id }
        for (def in childDefs) {
            val rootId = getRootInterfaceId(def.id)
            val parentName = rootIdToName[rootId] ?: "interface_$rootId"

            var cleanText = when {
                def.tooltip.isNotEmpty() -> def.tooltip
                def.spellName.isNotEmpty() -> def.spellName
                def.type == 4 && def.disabledMessage.isNotEmpty() -> def.disabledMessage
                else -> ""
            }.trim()

            if (cleanText.isNotEmpty()) {
                cleanText = Namer.removeTags(cleanText)
                    .replace("\n", " ").replace("\r", " ").replace("\t", " ").trim()

                if (cleanText.isNotEmpty()) {
                    val compKey = generateKey(cleanText, def.id, componentExistingKeys)
                    componentMap["$parentName:$compKey"] = def.id
                }
            }
        }

        val ifaceLines = interfaceMap.entries
            .sortedBy { it.value }
            .map { "${it.key}=${it.value}" }
        val compLines = componentMap.entries
            .sortedBy { it.value }
            .map { "${it.key}=${it.value}" }

        return ifaceLines to compLines
    }

    private fun parse377InterfaceNames(file: File): Map<Int, String> {
        if (!file.exists()) return emptyMap()
        val results = mutableMapOf<Int, String>()
        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
            val firstSpace = trimmed.indexOfFirst { it.isWhitespace() }
            if (firstSpace != -1) {
                val idStr = trimmed.substring(0, firstSpace)
                val nameStr = trimmed.substring(firstSpace).trim()
                val id = idStr.toIntOrNull()
                if (id != null && nameStr.isNotEmpty()) {
                    results[id] = nameStr
                }
            }
        }
        return results
    }

    private fun parseSectionIdToml(file: File): Map<String, Int> {
        val results = mutableMapOf<String, Int>()
        var currentSection: String? = null
        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length - 1).trim()
            } else if (currentSection != null && trimmed.startsWith("id")) {
                val parts = trimmed.split("=")
                if (parts.size == 2) {
                    parts[1].trim().toIntOrNull()?.let { id ->
                        results[currentSection!!] = id
                    }
                }
            }
        }
        return results
    }

    private fun parseSymFile(file: File): Map<Int, String> {
        if (!file.exists()) return emptyMap()
        val results = mutableMapOf<Int, String>()
        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
            val parts = trimmed.split("\t")
            if (parts.size == 2) {
                val id = parts[0].toIntOrNull()
                val name = parts[1].trim()
                if (id != null && name.isNotEmpty()) {
                    results[id] = name
                }
            }
        }
        return results
    }
}
