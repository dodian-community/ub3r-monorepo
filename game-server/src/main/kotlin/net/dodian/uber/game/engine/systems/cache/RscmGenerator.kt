package net.dodian.uber.game.engine.systems.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.dodian.uber.game.rscm.Namer
import java.io.File
import java.security.MessageDigest

/**
 * Generates developer-reference mappings from the pinned revision-218 OSRS dump.
 *
 * The rich outputs intentionally preserve the dump's raw symbols and ordered
 * fields. Gameplay owns raw cache IDs and does not load these files at runtime.
 */
object RscmGenerator {
    const val PINNED_REVISION = "0a55afe6182e9c6a22955650dd02fc7d08d1648c"

    private val configNamespaces = linkedMapOf(
        "area" to "area",
        "bugtemplate" to "bugtemplate",
        "controller" to "controller",
        "dbrow" to "dbrow",
        "dbtable" to "dbtable",
        "enum" to "enum",
        "flo" to "overlay",
        "flu" to "underlay",
        "gamelogevent" to "gamelogevent",
        "headbar" to "headbar",
        "hitmark" to "hitmark",
        "hunt" to "hunt",
        "idk" to "idkit",
        "inv" to "inv",
        "itemcode" to "itemcode",
        "loc" to "loc",
        "mel" to "mapelement",
        "mesanim" to "mesanim",
        "npc" to "npc",
        "obj" to "obj",
        "param" to "param",
        "seq" to "seq",
        "spot" to "spotanim",
        "stringvector" to "stringvector",
        "struct" to "struct",
        "texture" to "material",
        "varbit" to "varbit",
        "varc" to "varc",
        "varclan" to "varclan",
        "varclansetting" to "varclansetting",
        "varcon" to "varcon",
        "varconbit" to "varconbit",
        "varg" to "varg",
        "varn" to "varn",
        "varnbit" to "varnbit",
        "varobj" to "varobj",
        "varp" to "varp",
        "vars" to "vars",
        "wma" to "wma",
    )
    private val json = Gson()
    private val prettyJson = GsonBuilder().setPrettyPrinting().create()
    private val numericComment = Regex("""^//\s*(-?\d+)\s*$""")
    private val numericSymbol = Regex("""^[a-z][a-z0-9]*_-?\d+$""")
    private val optionKey = Regex("""^(?:i?op)\d+$""")

    @JvmStatic
    fun main(args: Array<String>) {
        val options = parseArgs(args)
        val dump = File(options.getValue("dump"))
        val output = File(options["output"] ?: defaultOutput().path)
        val sourceRevision = options["source-revision"] ?: dump.name.substringAfterLast('-')
        require(dump.isDirectory) { "OSRS dump directory does not exist: ${dump.absolutePath}" }
        require(sourceRevision == PINNED_REVISION) {
            "Only pinned revision 218 ($PINNED_REVISION) is accepted; received $sourceRevision"
        }

        val configDirectory = File(dump, "config")
        val actualConfigTypes = configDirectory.listFiles { file -> file.isFile && file.name.startsWith("dump.") }
            ?.map { it.name.removePrefix("dump.") }
            ?.toSet()
            ?: emptySet()
        require(actualConfigTypes == configNamespaces.keys) {
            buildString {
                append("Pinned config dump set differs")
                val missing = configNamespaces.keys - actualConfigTypes
                val unexpected = actualConfigTypes - configNamespaces.keys
                if (missing.isNotEmpty()) append("; missing=${missing.sorted().joinToString()}")
                if (unexpected.isNotEmpty()) append("; unexpected=${unexpected.sorted().joinToString()}")
            }
        }

        val consumed = linkedSetOf<File>()
        val configRecords = linkedMapOf<String, List<ReferenceRecord>>()
        configNamespaces.forEach { (dumpType, namespace) ->
            val source = File(configDirectory, "dump.$dumpType")
            consumed += source
            configRecords[namespace] = parseConfig(source, namespace, relativePath(dump, source))
        }

        val symbolDirectory = File(dump, "symbols")
        val symbolFiles = symbolDirectory.listFiles { file -> file.isFile && file.extension == "sym" }
            ?.sortedBy { it.name }
            ?: error("Required symbol directory is missing: ${symbolDirectory.path}")
        require(symbolFiles.isNotEmpty()) { "No symbol files were found in ${symbolDirectory.path}" }
        consumed += symbolFiles
        val symbolTables = parseSymbolTables(symbolFiles)

        val interfaceSymbols = symbolTables.getValue("interface").associate { target ->
            target.id.toIntOrNull()?.let { it to target.symbol }
                ?: error("Malformed interface symbol ID ${target.id}")
        }
        val componentSymbols = symbolTables.getValue("component").associate { target -> target.id to target.symbol }
        val interfaceDirectory = File(dump, "interface")
        val interfaceFiles = interfaceDirectory.listFiles { file -> file.isFile && (file.extension == "if" || file.extension == "if3") }
            ?.sortedBy { it.name }
            ?: error("Required interface dump directory is missing: ${interfaceDirectory.path}")
        require(interfaceFiles.isNotEmpty()) { "No interface definitions were found in ${interfaceDirectory.path}" }
        consumed += interfaceFiles
        val interfaces = parseInterfaces(dump, interfaceFiles)
        val interfaceIds = interfaces.map { it.id }.toSet()
        require(interfaceSymbols.keys.all { it in interfaceIds }) {
            "Interface symbols reference missing files: ${(interfaceSymbols.keys - interfaceIds).sorted().joinToString()}"
        }
        val parsedComponentIds = interfaces.flatMap { it.components }.map { "${it.parent}:${it.child}" }.toSet()
        require(componentSymbols.keys == parsedComponentIds) {
            "Component symbols and interface definitions differ: " +
                "missing=${(componentSymbols.keys - parsedComponentIds).sorted().joinToString()} " +
                "unexpected=${(parsedComponentIds - componentSymbols.keys).sorted().joinToString()}"
        }

        val interfaceRecords = interfaces.map { definition ->
            ReferenceRecord(
                namespace = "interface",
                id = definition.id,
                symbol = interfaceSymbols[definition.id] ?: "interface_${definition.id}",
                fields = emptyList(),
                sourceFile = definition.sourceFile,
                attributes = mapOf("componentCount" to definition.components.size.toString()),
            )
        }
        val componentRecords = interfaces.flatMap { definition ->
            definition.components.map { component ->
                val packedId = (component.parent shl 16) or component.child
                ReferenceRecord(
                    namespace = "component",
                    id = packedId,
                    symbol = componentSymbols.getValue("${component.parent}:${component.child}"),
                    fields = component.fields,
                    sourceFile = definition.sourceFile,
                    attributes = mapOf(
                        "parent" to component.parent.toString(),
                        "child" to component.child.toString(),
                    ),
                )
            }
        }.sortedBy { it.id }

        val allRecords = linkedMapOf<String, List<ReferenceRecord>>().apply {
            putAll(configRecords)
            put("interface", interfaceRecords)
            put("component", componentRecords)
        }
        options["cache"]?.let { cachePath -> verifyAgainstCache(File(cachePath), allRecords) }

        val registry = buildReferenceRegistry(symbolTables, allRecords)
        val rendered = render(
            input = allRecords,
            configNamespaces = configRecords.keys,
            interfaces = interfaces,
            sourceRevision = sourceRevision,
            dump = dump,
            consumed = consumed.toList(),
            registry = registry,
        )
        if (options.containsKey("check")) {
            checkCurrent(output, rendered)
            println("Cache reference mappings are current under ${output.absolutePath}")
            return
        }
        synchronizeOutput(output, rendered)
        println("Generated ${allRecords.values.sumOf { it.size }} reference records under ${output.absolutePath}")
    }

    private fun defaultOutput(): File =
        net.dodian.uber.game.engine.config.ServerPaths.revision218Reference().toFile()

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
        require("dump" in options) {
            "Usage: --dump <path> [--output <path>] [--cache <path>] [--source-revision <id>] [--check]"
        }
        return options
    }

    private fun parseConfig(file: File, namespace: String, sourceFile: String): List<ReferenceRecord> {
        val records = mutableListOf<ReferenceRecord>()
        var pendingCommentId: Int? = null
        var currentId: Int? = null
        var currentSymbol: String? = null
        var fields = mutableListOf<ReferenceField>()

        fun flush() {
            val id = currentId ?: return
            val symbol = currentSymbol ?: error("Missing symbol for $id in ${file.name}")
            records += ReferenceRecord(namespace, id, symbol, fields.toList(), sourceFile)
            fields = mutableListOf()
        }

        file.forEachLine { raw ->
            val line = raw.trim()
            val commentId = numericComment.matchEntire(line)?.groupValues?.get(1)?.toInt()
            when {
                commentId != null -> pendingCommentId = commentId
                line.startsWith("[") && line.endsWith("]") -> {
                    flush()
                    val symbol = line.removeSurrounding("[", "]")
                    val suffixId = symbol.substringAfterLast('_').toIntOrNull()
                    // Numeric suffixes can be part of a semantic gameval
                    // (for example world-map ID 37 is [tutorial_2]). The
                    // dump's numeric comment is authoritative when present.
                    currentId = pendingCommentId ?: suffixId
                        ?: error("Could not read ID for [$symbol] in ${file.name}")
                    currentSymbol = symbol
                    pendingCommentId = null
                }
                line.isBlank() || line.startsWith("//") -> Unit
                currentId == null -> error("Property outside a section in ${file.name}: $line")
                else -> {
                    val separator = line.indexOf('=')
                    require(separator > 0) { "Malformed property in ${file.name}: $line" }
                    fields += ReferenceField(line.substring(0, separator), line.substring(separator + 1))
                }
            }
        }
        flush()
        require(records.map { it.id }.distinct().size == records.size) { "Duplicate IDs in ${file.name}" }
        require(records.map { it.symbol }.distinct().size == records.size) { "Duplicate symbols in ${file.name}" }
        return records.sortedBy { it.id }
    }

    private fun parseSymbolTables(files: List<File>): Map<String, List<SymbolTarget>> =
        files.associate { file ->
            val namespace = file.nameWithoutExtension
            namespace to file.readLines().filter { it.isNotBlank() }.map { line ->
                val parts = line.split('\t', limit = 2)
                require(parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    "Malformed symbol line in ${file.name}: $line"
                }
                SymbolTarget(namespace, parts[0], parts[1])
            }
        }

    private fun parseInterfaces(dump: File, files: List<File>): List<InterfaceDefinition> =
        files.map { file ->
            val parentId = file.name.removePrefix("interface_").substringBefore('.').toIntOrNull()
                ?: error("Could not read interface ID from ${file.name}")
            val components = mutableListOf<InterfaceComponent>()
            var pendingCoordinates: Pair<Int, Int>? = null
            var currentChild: Int? = null
            var fields = mutableListOf<ReferenceField>()

            fun flush() {
                val child = currentChild ?: return
                components += InterfaceComponent(parentId, child, fields.toList())
                fields = mutableListOf()
            }

            file.forEachLine { raw ->
                val line = raw.trim()
                val coordinates = Regex("""^//\s*(\d+):(\d+)\s*$""").matchEntire(line)
                when {
                    coordinates != null -> pendingCoordinates =
                        coordinates.groupValues[1].toInt() to coordinates.groupValues[2].toInt()
                    line.startsWith("[com_") && line.endsWith("]") -> {
                        flush()
                        val child = line.removePrefix("[com_").removeSuffix("]").toIntOrNull()
                            ?: error("Could not read component ID from $line in ${file.name}")
                        pendingCoordinates?.let { (commentParent, commentChild) ->
                            require(commentParent == parentId && commentChild == child) {
                                "Component comment $commentParent:$commentChild does not match $parentId:$child in ${file.name}"
                            }
                        }
                        currentChild = child
                        pendingCoordinates = null
                    }
                    line.isBlank() || line.startsWith("//") -> Unit
                    currentChild == null -> error("Property outside a component in ${file.name}: $line")
                    else -> {
                        val separator = line.indexOf('=')
                        require(separator > 0) { "Malformed component property in ${file.name}: $line" }
                        fields += ReferenceField(line.substring(0, separator), line.substring(separator + 1))
                    }
                }
            }
            flush()
            require(components.map { it.child }.distinct().size == components.size) {
                "Duplicate components in ${file.name}"
            }
            InterfaceDefinition(
                id = parentId,
                sourceFile = relativePath(dump, file),
                outputFile = file.name,
                content = file.readText().normalizeNewline(),
                components = components,
            )
        }.also { definitions ->
            require(definitions.map { it.id }.distinct().size == definitions.size) { "Duplicate interface IDs" }
        }.sortedBy { it.id }

    private fun buildReferenceRegistry(
        symbolTables: Map<String, List<SymbolTarget>>,
        records: Map<String, List<ReferenceRecord>>,
    ): Map<String, SymbolTarget> {
        val targets = symbolTables.values.flatten().toMutableList()
        records.forEach { (namespace, namespaceRecords) ->
            namespaceRecords.forEach { record ->
                targets += SymbolTarget(namespace, record.id.toString(), record.symbol)
            }
        }
        return targets.groupBy { it.symbol }
            .filterValues { candidates -> candidates.map { it.namespace to it.id }.distinct().size == 1 }
            .mapValues { (_, candidates) -> candidates.first() }
    }

    private fun render(
        input: Map<String, List<ReferenceRecord>>,
        configNamespaces: Set<String>,
        interfaces: List<InterfaceDefinition>,
        sourceRevision: String,
        dump: File,
        consumed: List<File>,
        registry: Map<String, SymbolTarget>,
    ): Map<String, String> {
        val rendered = linkedMapOf<String, String>()
        val resolved = input.mapValues { (namespace, records) -> resolveKeys(namespace, records) }
        resolved.forEach { (namespace, records) ->
            rendered["index/$namespace.rscm"] = records.sortedBy { it.key }
                .joinToString("\n", postfix = "\n") { "${it.key}=${it.id}" }
            rendered["metadata/$namespace.jsonl"] = records.sortedBy { it.id }
                .joinToString("\n", postfix = "\n") { record ->
                    json.toJson(record.toJson(registry))
                }
            if (namespace in configNamespaces) {
                rendered["$namespace.rscm"] = records.sortedBy { it.id }
                    .joinToString("\n\n", postfix = "\n") { it.toRichBlock() }
            }
        }
        interfaces.forEach { definition ->
            rendered["interfaces/${definition.outputFile}"] = definition.content
        }

        val unresolvedReferences = resolved.values.flatten().sumOf { record ->
            record.record.fields.sumOf { field -> unresolvedSymbolCandidates(field, registry).size }
        }
        val artifacts = (rendered.keys + "metadata/manifest.json").sorted()
        val manifest = JsonObject().apply {
            addProperty("sourceRevision", sourceRevision)
            addProperty("format", "cache-reference-v4")
            addProperty("configDumpCount", configNamespaces.size)
            addProperty("interfaceFileCount", interfaces.size)
            addProperty("componentCount", input.getValue("component").size)
            addProperty("unresolvedReferences", unresolvedReferences)
            add("sources", JsonArray().also { array ->
                consumed.distinct().sortedBy { relativePath(dump, it) }.forEach { file ->
                    array.add(JsonObject().apply {
                        addProperty("path", relativePath(dump, file))
                        addProperty("sha256", sha256(file))
                    })
                }
            })
            add("namespaces", JsonObject().also { namespaces ->
                resolved.toSortedMap().forEach { (name, records) -> namespaces.addProperty(name, records.size) }
            })
            add("nameSources", JsonObject().also { sources ->
                resolved.values.flatten()
                    .groupingBy { it.nameSource.wireName }
                    .eachCount()
                    .toSortedMap()
                    .forEach(sources::addProperty)
            })
            add("artifacts", JsonArray().also { array ->
                artifacts.forEach { path ->
                    array.add(JsonObject().apply {
                        addProperty("path", path)
                        if (path != "metadata/manifest.json") addProperty("sha256", sha256(rendered.getValue(path)))
                    })
                }
            })
        }
        rendered["metadata/manifest.json"] = prettyJson.toJson(manifest) + "\n"
        return rendered
    }

    private fun resolveKeys(namespace: String, records: List<ReferenceRecord>): List<ResolvedReferenceRecord> {
        val recordsById = records.associateBy { it.id }
        val candidates = records.map { record ->
            record to resolveName(record, recordsById, linkedSetOf())
        }
        val result = mutableListOf<ResolvedReferenceRecord>()
        candidates.groupBy { (record, name) ->
            Namer.sanitizeRSCM(name.displayName).ifBlank { record.symbol.ifBlank { "${namespace}_${record.id}" } }
        }.toSortedMap().forEach { (base, group) ->
            group.sortedBy { it.first.id }.forEachIndexed { index, (record, name) ->
                result += ResolvedReferenceRecord(record, if (index == 0) base else "${base}_${record.id}", name)
            }
        }
        return result
    }

    private fun resolveName(
        record: ReferenceRecord,
        recordsById: Map<Int, ReferenceRecord>,
        resolving: MutableSet<Int>,
    ): ResolvedName {
        val decodedName = record.values("name").lastOrNull()?.takeIf { it.isNotBlank() }
        if (decodedName != null) return ResolvedName(decodedName, NameSource.DECODED)

        if (!record.symbol.matches(Regex("""^[a-z][a-z0-9]*_${record.id}$"""))) {
            return ResolvedName(record.symbol, NameSource.SYMBOL)
        }
        if (record.namespace != "obj") return ResolvedName(record.symbol, NameSource.FALLBACK)
        check(resolving.add(record.id)) {
            "Cyclic item naming link: ${(resolving + record.id).joinToString(" -> ")}"
        }
        try {
            val link = when {
                record.has("certtemplate") -> Link("certlink", " (noted)", NameSource.LINKED_CERT)
                record.has("placeholdertemplate") ->
                    Link("placeholderlink", " (placeholder)", NameSource.LINKED_PLACEHOLDER)
                else -> null
            } ?: return ResolvedName(record.symbol, NameSource.FALLBACK)
            val linkedId = record.values(link.property).lastOrNull()?.substringAfter("obj_")?.toIntOrNull()
                ?: return ResolvedName(record.symbol, NameSource.FALLBACK)
            val linked = recordsById[linkedId] ?: return ResolvedName(record.symbol, NameSource.FALLBACK)
            val linkedName = resolveName(linked, recordsById, resolving)
            if (linkedName.nameSource == NameSource.FALLBACK) {
                return ResolvedName(record.symbol, NameSource.FALLBACK, linkedId)
            }
            return ResolvedName(linkedName.displayName + link.suffix, link.nameSource, linkedId)
        } finally {
            resolving.remove(record.id)
        }
    }

    private fun findReferences(field: ReferenceField, registry: Map<String, SymbolTarget>): List<ResolvedReference> {
        if (field.key == "name" || field.key == "desc" || field.key == "text" || optionKey.matches(field.key)) {
            return emptyList()
        }
        return field.value.split(',').map(String::trim).distinct()
            .filter { candidate -> candidate.any(Char::isLetter) }
            .mapNotNull { candidate ->
            registry[candidate]?.let { target ->
                ResolvedReference(field.key, field.value, candidate, target.namespace, target.id)
            }
        }
    }

    private fun unresolvedSymbolCandidates(field: ReferenceField, registry: Map<String, SymbolTarget>): List<String> {
        if (field.key == "name" || field.key == "desc" || field.key == "text" || optionKey.matches(field.key)) {
            return emptyList()
        }
        return field.value.split(',').map(String::trim).distinct()
            .filter { numericSymbol.matches(it) && it !in registry }
    }

    private fun verifyAgainstCache(cacheDir: File, records: Map<String, List<ReferenceRecord>>) {
        require(cacheDir.isDirectory) { "Cache directory does not exist: ${cacheDir.absolutePath}" }
        CacheStore(cacheDir.toPath()).open().use { store ->
            fun verify(namespace: String, actual: Set<Int>) {
                val expected = records.getValue(namespace).map { it.id }.toSet()
                check(actual.all { it in expected }) {
                    "$namespace dump is missing packed-cache IDs: ${(actual - expected).sorted().joinToString()}"
                }
            }
            verify("loc", ObjectDefinitionDecoder.decode(store).definitions.keys)
            verify("npc", NpcCacheDefinitionDecoder.decode(store).keys)
            verify("seq", AnimationDefinitionDecoder.decode(store).keys)
            verify("spotanim", SpotAnimDefinitionDecoder.decode(store).keys)
            val dumpVarbits = records.getValue("varbit").size
            val cacheVarbits = VarbitDefinitionDecoder.decode(store).size
            if (dumpVarbits != cacheVarbits) {
                println("varbit reference differs from packed cache: dump=$dumpVarbits cache=$cacheVarbits")
            }
        }
    }

    private fun checkCurrent(output: File, rendered: Map<String, String>) {
        val existing = managedFiles(output)
        val expected = rendered.keys
        val missingOrChanged = expected.filter { relative ->
            val target = File(output, relative)
            !target.isFile || target.readText() != rendered.getValue(relative)
        }
        val extra = existing - expected
        check(missingOrChanged.isEmpty() && extra.isEmpty()) {
            buildString {
                append("Generated reference mappings differ")
                if (missingOrChanged.isNotEmpty()) append("; missing/changed=${missingOrChanged.sorted().joinToString()}")
                if (extra.isNotEmpty()) append("; extra=${extra.sorted().joinToString()}")
            }
        }
    }

    private fun synchronizeOutput(output: File, rendered: Map<String, String>) {
        output.mkdirs()
        val stale = managedFiles(output) - rendered.keys
        stale.forEach { relative -> File(output, relative).delete() }
        rendered.forEach { (relative, content) ->
            val target = File(output, relative)
            target.parentFile.mkdirs()
            target.writeText(content)
        }
        listOf("index", "interfaces", "metadata").forEach { relative ->
            File(output, relative).walkBottomUp().filter { it.isDirectory && it.listFiles().isNullOrEmpty() }.forEach(File::delete)
        }
    }

    private fun managedFiles(output: File): Set<String> {
        if (!output.isDirectory) return emptySet()
        val rootMappings = output.listFiles { file -> file.isFile && file.extension == "rscm" }.orEmpty().toList()
        val generatedDirectories = listOf("index", "interfaces", "metadata").flatMap { relative ->
            val directory = File(output, relative)
            if (directory.isDirectory) directory.walkTopDown().filter(File::isFile).toList() else emptyList()
        }
        return (rootMappings + generatedDirectories).map { relativePath(output, it) }.toSet()
    }

    private fun relativePath(root: File, file: File): String =
        root.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize())
            .toString().replace(File.separatorChar, '/')

    private fun String.normalizeNewline(): String = replace("\r\n", "\n").removeSuffix("\n") + "\n"

    private fun sha256(file: File): String = sha256(file.readBytes())
    private fun sha256(content: String): String = sha256(content.toByteArray())
    private fun sha256(content: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }

    private data class ReferenceField(val key: String, val value: String)

    private data class ReferenceRecord(
        val namespace: String,
        val id: Int,
        val symbol: String,
        val fields: List<ReferenceField>,
        val sourceFile: String,
        val attributes: Map<String, String> = emptyMap(),
    ) {
        fun values(key: String): List<String> = fields.filter { it.key == key }.map { it.value }
        fun has(key: String): Boolean = fields.any { it.key == key }
    }

    private data class InterfaceComponent(
        val parent: Int,
        val child: Int,
        val fields: List<ReferenceField>,
    )

    private data class InterfaceDefinition(
        val id: Int,
        val sourceFile: String,
        val outputFile: String,
        val content: String,
        val components: List<InterfaceComponent>,
    )

    private data class SymbolTarget(val namespace: String, val id: String, val symbol: String)

    private enum class NameSource(val wireName: String) {
        DECODED("decoded"),
        SYMBOL("symbol"),
        LINKED_CERT("linked_cert"),
        LINKED_PLACEHOLDER("linked_placeholder"),
        FALLBACK("fallback"),
    }

    private data class Link(val property: String, val suffix: String, val nameSource: NameSource)

    private data class ResolvedName(
        val displayName: String,
        val nameSource: NameSource,
        val linkedSourceId: Int? = null,
    )

    private data class ResolvedReference(
        val field: String,
        val value: String,
        val symbol: String,
        val namespace: String,
        val id: String,
    )

    private data class ResolvedReferenceRecord(
        val record: ReferenceRecord,
        val key: String,
        val name: ResolvedName,
    ) {
        val id get() = record.id
        val nameSource get() = name.nameSource

        fun toRichBlock(): String = buildString {
            append("// ").append(record.id).append('\n')
            append("// alias=").append(key).append('\n')
            append('[').append(record.symbol).append(']')
            record.fields.forEach { field ->
                append('\n').append(field.key).append('=').append(field.value)
            }
        }

        fun toJson(registry: Map<String, SymbolTarget>): JsonObject = JsonObject().apply {
            addProperty("namespace", record.namespace)
            addProperty("id", record.id)
            addProperty("key", key)
            addProperty("alias", key)
            addProperty("symbol", record.symbol)
            addProperty("displayName", name.displayName)
            addProperty("nameSource", name.nameSource.wireName)
            addProperty("sourceFile", record.sourceFile)
            name.linkedSourceId?.let { addProperty("linkedSourceId", it) }
            record.fields.lastOrNull { it.key == "name" }?.let { addProperty("name", it.value) }
            record.attributes.forEach { (attribute, value) ->
                value.toIntOrNull()?.let { addProperty(attribute, it) } ?: addProperty(attribute, value)
            }
            add("fields", JsonArray().also { array ->
                record.fields.forEach { field ->
                    array.add(JsonObject().apply {
                        addProperty("key", field.key)
                        addProperty("value", field.value)
                    })
                }
            })
            add("properties", JsonObject().also { propertyObject ->
                record.fields.groupBy { it.key }.forEach { (property, fields) ->
                    val values = fields.map { it.value }
                    if (values.size == 1) propertyObject.addProperty(property, values.single())
                    else propertyObject.add(property, JsonArray().also { values.forEach(it::add) })
                }
            })
            val options = record.fields.filter { optionKey.matches(it.key) }.groupBy { it.key }
            if (options.isNotEmpty()) {
                add("options", JsonObject().also { optionObject ->
                    options.toSortedMap(
                        compareBy<String> { it.removePrefix("iop").removePrefix("op").toInt() }.thenBy { it },
                    ).forEach { (option, fields) ->
                        val values = fields.map { it.value }
                        if (values.size == 1) optionObject.addProperty(option, values.single())
                        else optionObject.add(option, JsonArray().also { values.forEach(it::add) })
                    }
                })
            }
            val references = record.fields.flatMap { findReferences(it, registry) }
            if (references.isNotEmpty()) {
                add("references", JsonArray().also { array ->
                    references.forEach { reference ->
                        array.add(JsonObject().apply {
                            addProperty("field", reference.field)
                            addProperty("value", reference.value)
                            addProperty("symbol", reference.symbol)
                            addProperty("namespace", reference.namespace)
                            reference.id.toIntOrNull()?.let { addProperty("id", it) }
                                ?: addProperty("id", reference.id)
                        })
                    }
                })
            }
        }
    }
}
