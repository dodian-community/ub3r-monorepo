package net.dodian.uber.game.api.plugin.runtime

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.toml.TomlMapper

/**
 * Strict array-of-tables TOML reader for classpath-bundled plugin data.
 *
 * Plugin modules are compiled independently of :game-server and run merged onto
 * one classpath alongside every other plugin module's resources, so data files
 * are loaded by resource path (not a filesystem-relative "content/" path like
 * :game-server's own bundled content) and callers should namespace their
 * resource path by module (e.g. "cooking/recipes.toml") to avoid collisions.
 *
 * Values are returned as raw strings; callers convert fields with their own
 * defaults, matching the per-file typed parsers already used on the
 * :game-server side (see TomlProjectileLoader, TomlAncientSpellLoader).
 */
object TomlRecordReader {
    private val mapper: TomlMapper = TomlMapper.builder()
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
        .build()

    @JvmStatic
    @JvmOverloads
    fun readRecords(
        resource: String,
        table: String,
        classLoader: ClassLoader = TomlRecordReader::class.java.classLoader,
    ): List<Map<String, String>> {
        require(resource.endsWith(".toml")) { "Plugin data resource must be a TOML file: $resource" }
        require(table.matches(Regex("[a-z][a-z0-9_]*"))) { "Invalid TOML table '$table' in $resource" }
        val root = classLoader.getResourceAsStream(resource)?.use { stream ->
            runCatching { mapper.readTree(stream) }.getOrElse { cause ->
                throw IllegalStateException("Unable to parse plugin TOML $resource: ${cause.message}", cause)
            }
        } ?: throw IllegalStateException("Missing required plugin TOML resource: $resource")

        require(root.path("schema_version").asInt(-1) == 1) {
            "Unsupported or missing schema_version in plugin TOML $resource; expected 1"
        }

        val records = root.path(table)
        require(records.isArray) { "Plugin TOML $resource must declare [[$table]] records" }
        require(records.size() > 0) { "Plugin TOML $resource has no [[$table]] records" }
        return records.mapIndexed { index, node ->
            require(node.isObject) { "Plugin TOML $resource [$table][$index] must be an object" }
            node.fields().asSequence().associate { (key, value) ->
                require(!value.isContainerNode || value.isArray) {
                    "Plugin TOML $resource [$table][$index].$key must be a scalar or array"
                }
                key to value.asRecordValue(resource, table, index, key)
            }
        }
    }

    private fun JsonNode.asRecordValue(resource: String, table: String, index: Int, key: String): String = when {
        isTextual || isNumber || isBoolean -> asText()
        isArray -> joinToString(",") { element ->
            require(element.isTextual || element.isNumber || element.isBoolean) {
                "Plugin TOML $resource [$table][$index].$key may only contain scalar array values"
            }
            element.asText()
        }
        else -> error("Plugin TOML $resource [$table][$index].$key has an unsupported value")
    }
}
