package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillPluginLayoutBoundaryTest {
    @Test
    fun `prayer skill module uses data actions and explicit plugin entrypoint contract`() {
        val prayerData = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/skill/prayer/PrayerData.kt"))
        val prayerActions = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/skill/prayer/PrayerActions.kt"))
        val prayerSkill = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/skill/prayer/Prayer.kt"))

        assertTrue(prayerData.contains("object PrayerRouteIds"))
        assertTrue(prayerData.contains("object PrayerActionIds"))
        assertTrue(prayerActions.contains("PrayerActionIds.ALTAR_BONES"))
        assertTrue(prayerSkill.contains("object PrayerSkillPlugin : SkillPlugin"))
        assertTrue(prayerSkill.contains("PrayerRouteIds.ALTAR_OBJECT_IDS"))
        assertTrue(prayerSkill.contains("PrayerRouteIds.BONE_ITEM_IDS"))
    }
}
