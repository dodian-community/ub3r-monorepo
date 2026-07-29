package net.dodian.uber.game.item

import com.google.gson.Gson
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DarkBowDefinitionTest {
    @Test
    fun `dark bow grants ranged accuracy but no ranged strength`() {
        val definition = Gson().fromJson(
            Files.readString(Path.of("data/def/items-json/11235.json")),
            ItemDefJson::class.java,
        )

        val bonuses = requireNotNull(definition.equipment).toBonusArray()
        assertEquals(95, bonuses[4], "Dark bow should retain its +95 ranged attack bonus")
        assertEquals(0, bonuses[11], "Dark bow has no inherent ranged-strength bonus")
    }
}
