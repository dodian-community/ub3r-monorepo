package net.dodian.uber.game.rscm

import net.dodian.uber.game.engine.systems.cache.RscmGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class RscmGeneratorNameEnrichmentTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `derives only same revision item variant names and falls back safely`() {
        val dump = temporaryDirectory.resolve("dump")
        val config = Files.createDirectories(dump.resolve("config"))
        val symbols = Files.createDirectories(dump.resolve("symbols"))
        val output = temporaryDirectory.resolve("output")
        writeMinimalDump(config, symbols)

        RscmGenerator.main(arrayOf("--dump", dump.toString(), "--output", output.toString()))

        val entries = Files.readAllLines(output.resolve("obj.rscm"))
            .associate { line -> line.substringBefore('=') to line.substringAfter('=').toInt() }
        assertEquals(1, entries["white_dark_bow_paint"])
        assertEquals(2, entries["white_dark_bow_paint_noted"])
        assertEquals(3, entries["white_dark_bow_paint_placeholder"])
        assertEquals(11, entries["white_dark_bow_paint_noted_11"])
        assertEquals(4, entries["obj_4"])
        assertEquals(5, entries["obj_5"])
        assertEquals(6, entries["obj_6"])
        assertEquals(7, entries["obj_7"])
        assertEquals(8, entries["obj_8"])
        assertEquals(entries.size, entries.values.toSet().size)

        val metadata = Files.readAllLines(output.resolve("metadata/obj.jsonl"))
        val noted = metadata.first { it.contains("\"id\":2") }
        val placeholder = metadata.first { it.contains("\"id\":3") }
        val missing = metadata.first { it.contains("\"id\":4") }
        assertTrue(noted.contains("\"displayName\":\"White dark bow paint (noted)\""))
        assertTrue(noted.contains("\"nameSource\":\"linked_cert\""))
        assertTrue(noted.contains("\"linkedSourceId\":1"))
        assertTrue(placeholder.contains("\"nameSource\":\"linked_placeholder\""))
        assertTrue(missing.contains("\"nameSource\":\"fallback\""))
        assertTrue(Files.readString(output.resolve("metadata/manifest.json")).contains("\"format\": \"cache-reference-v2\""))
    }

    private fun writeMinimalDump(config: Path, symbols: Path) {
        val simpleConfigs = mapOf(
            "loc" to "[loc_0]\nname=Crate\n",
            "npc" to "[npc_0]\nname=Test npc\n",
            "seq" to "[seq_0]\n",
            "spot" to "[spotanim_0]\n",
            "varbit" to "[varbit_0]\n",
            "varp" to "[varp_0]\n",
        )
        simpleConfigs.forEach { (type, content) -> Files.writeString(config.resolve("dump.$type"), content) }
        Files.writeString(
            config.resolve("dump.obj"),
            """
            [obj_1]
            name=White dark bow paint

            [obj_2]
            certlink=obj_1
            certtemplate=obj_799

            [obj_3]
            placeholderlink=obj_1
            placeholdertemplate=obj_14401

            [obj_4]
            certlink=obj_404
            certtemplate=obj_799

            [obj_5]
            certlink=obj_6
            certtemplate=obj_799

            [obj_6]
            certlink=obj_5
            certtemplate=obj_799

            [obj_7]
            certlink=obj_8
            certtemplate=obj_799

            [obj_8]

            [obj_11]
            name=White dark bow paint (noted)
            """.trimIndent() + "\n",
        )
        Files.writeString(symbols.resolve("interface.sym"), "0\tinterface_0\n")
        Files.writeString(symbols.resolve("component.sym"), "0:0\tcomponent_0\n")
    }
}
