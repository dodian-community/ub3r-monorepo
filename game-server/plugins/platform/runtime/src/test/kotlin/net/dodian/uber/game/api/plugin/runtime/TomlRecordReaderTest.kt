package net.dodian.uber.game.api.plugin.runtime

import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TomlRecordReaderTest {
    @Test
    fun `missing resources fail with the resource path`() {
        val error = assertThrows(IllegalStateException::class.java) {
            TomlRecordReader.readRecords("missing/skill.toml", "record")
        }
        assertEquals(true, error.message!!.contains("missing/skill.toml"))
    }

    @Test
    fun `malformed toml fails instead of silently dropping records`() {
        val loader = object : ClassLoader() {
            override fun getResourceAsStream(name: String) =
                if (name == "broken.toml") ByteArrayInputStream("[[record]\\nvalue = [".toByteArray()) else null
        }
        assertThrows(IllegalStateException::class.java) {
            TomlRecordReader.readRecords("broken.toml", "record", loader)
        }
    }

    @Test
    fun `missing or unsupported schema version fails with an actionable message`() {
        val loader = object : ClassLoader() {
            override fun getResourceAsStream(name: String) =
                if (name == "old.toml") ByteArrayInputStream("[[record]]\nvalue = 1".toByteArray()) else null
        }
        val error = assertThrows(IllegalArgumentException::class.java) {
            TomlRecordReader.readRecords("old.toml", "record", loader)
        }
        assertEquals(true, error.message!!.contains("schema_version"))
    }
}
