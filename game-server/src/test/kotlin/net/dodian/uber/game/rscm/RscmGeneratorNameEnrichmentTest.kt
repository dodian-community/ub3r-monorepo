package net.dodian.uber.game.rscm

import com.google.gson.JsonParser
import net.dodian.uber.game.engine.systems.cache.RscmGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class RscmGeneratorNameEnrichmentTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `exports rich ordered records and same revision aliases`() {
        val dump = temporaryDirectory.resolve("osrs-dumps-${RscmGenerator.PINNED_REVISION}")
        val config = Files.createDirectories(dump.resolve("config"))
        val symbols = Files.createDirectories(dump.resolve("symbols"))
        val interfaces = Files.createDirectories(dump.resolve("interface"))
        val output = temporaryDirectory.resolve("output")
        writePinnedFixture(config, symbols, interfaces)

        RscmGenerator.main(arrayOf("--dump", dump.toString(), "--output", output.toString()))

        val entries = Files.readAllLines(output.resolve("index/obj.rscm"))
            .associate { line -> line.substringBefore('=') to line.substringAfter('=').toInt() }
        assertEquals(1, entries["white_dark_bow_paint"])
        assertEquals(2, entries["white_dark_bow_paint_noted"])
        assertEquals(3, entries["white_dark_bow_paint_placeholder"])
        assertEquals(11, entries["white_dark_bow_paint_noted_11"])
        assertEquals(4, entries["obj_4"])
        assertEquals(5, entries["obj_5"])
        assertEquals(6, entries["obj_6"])
        assertEquals(entries.size, entries.values.toSet().size)

        val richLocation = Files.readString(output.resolve("loc.rscm"))
        assertTrue(
            richLocation.contains(
                """
                // 11366
                // alias=coal_rocks_11366
                [loc_11366]
                op1=Mine
                op3=hidden
                model=model_1390
                model=model_1391
                name=Coal rocks
                """.trimIndent(),
            ),
        )
        assertFalse(richLocation.contains("coalrock1"))

        val objMetadata = Files.readAllLines(output.resolve("metadata/obj.jsonl"))
        val noted = objMetadata.first { it.contains("\"namespace\":\"obj\",\"id\":2,") }
        val placeholder = objMetadata.first { it.contains("\"namespace\":\"obj\",\"id\":3,") }
        val broken = objMetadata.first { it.contains("\"namespace\":\"obj\",\"id\":4,") }
        assertTrue(noted.contains("\"displayName\":\"White dark bow paint (noted)\""))
        assertTrue(noted.contains("\"nameSource\":\"linked_cert\""))
        assertTrue(noted.contains("\"linkedSourceId\":1"))
        assertTrue(placeholder.contains("\"nameSource\":\"linked_placeholder\""))
        assertTrue(broken.contains("\"nameSource\":\"fallback\""))

        val locationMetadata = Files.readAllLines(output.resolve("metadata/loc.jsonl"))
            .first { it.contains("\"namespace\":\"loc\",\"id\":11366,") }
        val locationJson = JsonParser().parse(locationMetadata).asJsonObject
        assertEquals(listOf("op1", "op3", "model", "model", "name"), locationJson["fields"].asJsonArray.map {
            it.asJsonObject["key"].asString
        })
        assertEquals(2, locationJson["properties"].asJsonObject["model"].asJsonArray.size())
        assertEquals("Mine", locationJson["options"].asJsonObject["op1"].asString)

        val npcMetadata = Files.readAllLines(output.resolve("metadata/npc.jsonl"))
            .first { it.contains("\"namespace\":\"npc\",\"id\":7402,") }
        assertEquals(
            "Attack",
            JsonParser().parse(npcMetadata).asJsonObject["options"].asJsonObject["op2"].asString,
        )

        val databaseMetadata = Files.readAllLines(output.resolve("metadata/dbrow.jsonl")).single()
        assertEquals(
            2,
            JsonParser().parse(databaseMetadata).asJsonObject["properties"].asJsonObject["data"].asJsonArray.size(),
        )
        assertEquals(
            Files.readString(interfaces.resolve("interface_514.if3")),
            Files.readString(output.resolve("interfaces/interface_514.if3")),
        )

        val manifest = Files.readString(output.resolve("metadata/manifest.json"))
        assertTrue(manifest.contains("\"format\": \"cache-reference-v4\""))
        assertTrue(manifest.contains("\"configDumpCount\": 39"))
        assertTrue(manifest.contains("\"interfaceFileCount\": 1"))
    }

    @Test
    fun `rejects cyclic item naming links`() {
        val dump = temporaryDirectory.resolve("cycle-${RscmGenerator.PINNED_REVISION}")
        val config = Files.createDirectories(dump.resolve("config"))
        val symbols = Files.createDirectories(dump.resolve("symbols"))
        val interfaces = Files.createDirectories(dump.resolve("interface"))
        writePinnedFixture(config, symbols, interfaces)
        Files.writeString(
            config.resolve("dump.obj"),
            """
            // 5
            [obj_5]
            certlink=obj_6
            certtemplate=obj_799

            // 6
            [obj_6]
            certlink=obj_5
            certtemplate=obj_799
            """.trimIndent() + "\n",
        )

        val failure = assertThrows(IllegalStateException::class.java) {
            RscmGenerator.main(
                arrayOf(
                    "--dump",
                    dump.toString(),
                    "--output",
                    temporaryDirectory.resolve("cycle-output").toString(),
                    "--source-revision",
                    RscmGenerator.PINNED_REVISION,
                ),
            )
        }
        assertTrue(failure.message.orEmpty().contains("Cyclic item naming link"))
    }

    @Test
    fun `rejects non pinned revisions`() {
        val dump = Files.createDirectories(temporaryDirectory.resolve("other-dump"))
        val failure = assertThrows(IllegalArgumentException::class.java) {
            RscmGenerator.main(
                arrayOf(
                    "--dump",
                    dump.toString(),
                    "--output",
                    temporaryDirectory.resolve("other-output").toString(),
                    "--source-revision",
                    "a5001c643b3a3b6fa1a90fcced2ae34ed5cbcddb",
                ),
            )
        }
        assertTrue(failure.message.orEmpty().contains("Only pinned revision 218"))
    }

    private fun writePinnedFixture(config: Path, symbols: Path, interfaces: Path) {
        CONFIG_TYPES.forEach { type ->
            val content = when (type) {
                "loc" ->
                    """
                    // 0
                    [loc_0]
                    name=Coal rocks

                    // 11366
                    [loc_11366]
                    op1=Mine
                    op3=hidden
                    model=model_1390
                    model=model_1391
                    name=Coal rocks
                    """.trimIndent() + "\n"
                "npc" ->
                    """
                    // 7402
                    [npc_7402]
                    op2=Attack
                    name=Abhorrent spectre
                    """.trimIndent() + "\n"
                "obj" ->
                    """
                    // 1
                    [obj_1]
                    name=White dark bow paint

                    // 2
                    [obj_2]
                    certlink=obj_1
                    certtemplate=obj_799

                    // 3
                    [obj_3]
                    placeholderlink=obj_1
                    placeholdertemplate=obj_14401

                    // 4
                    [obj_4]
                    certlink=obj_404
                    certtemplate=obj_799

                    // 5
                    [obj_5]
                    certlink=obj_6
                    certtemplate=obj_799

                    // 6
                    [obj_6]

                    // 11
                    [obj_11]
                    name=White dark bow paint (noted)
                    """.trimIndent() + "\n"
                "dbrow" ->
                    """
                    // 0
                    [dbrow_0]
                    data=col0,123
                    data=col1,Test row
                    """.trimIndent() + "\n"
                "texture" ->
                    """
                    [material_0]
                    sprite=door
                    """.trimIndent() + "\n"
                "wma" ->
                    """
                    // 0
                    [main]
                    name=Gielinor Surface
                    """.trimIndent() + "\n"
                else -> "// 0\n[${type}_0]\n"
            }
            Files.writeString(config.resolve("dump.$type"), content)
        }
        Files.writeString(symbols.resolve("interface.sym"), "514\tinterface_514\n")
        Files.writeString(symbols.resolve("component.sym"), "514:0\tcomponent_514_0\n")
        Files.writeString(symbols.resolve("model.sym"), "1390\tmodel_1390\n1391\tmodel_1391\n")
        Files.writeString(
            interfaces.resolve("interface_514.if3"),
            """
            // 514:0
            [com_0]
            type=model
            model=model_1390
            widthmode=minus
            heightmode=minus
            """.trimIndent() + "\n",
        )
    }

    private companion object {
        val CONFIG_TYPES = listOf(
            "area", "bugtemplate", "controller", "dbrow", "dbtable", "enum", "flo", "flu",
            "gamelogevent", "headbar", "hitmark", "hunt", "idk", "inv", "itemcode", "loc",
            "mel", "mesanim", "npc", "obj", "param", "seq", "spot", "stringvector", "struct",
            "texture", "varbit", "varc", "varclan", "varclansetting", "varcon", "varconbit",
            "varg", "varn", "varnbit", "varobj", "varp", "vars", "wma",
        )
    }
}
