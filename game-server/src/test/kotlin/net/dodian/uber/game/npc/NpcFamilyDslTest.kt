package net.dodian.uber.game.npc

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcFamilyDslTest {
    @Test
    fun `primary spawn uses family primary id`() {
        val family = npcFamily("Test Rat", 100) {
            spawns {
                spawn(3200, 3201)
            }
        }

        assertEquals(100, family.spawns.single().npcId)
    }

    @Test
    fun `alternate spawn keeps explicit id`() {
        val family = npcFamily("Test Rat", 100) {
            ids(101)
            spawns {
                spawnId(101, 3200, 3201)
            }
        }

        assertEquals(101, family.spawns.single().npcId)
    }

    @Test
    fun `per spawn override block survives into spawn definition`() {
        val family = npcFamily("Test Rat", 100) {
            spawns {
                at(3200, 3201) {
                    movement(walkRadius = 3, attackRange = 2, alwaysActive = true)
                    profile(net.dodian.uber.game.npc.profile("super-rat"))
                    overrides {
                        attack = 4
                        defence = 5
                        strength = 6
                        hitpoints = 20
                        ranged = 7
                        magic = 8
                        respawnTicks = 45
                        attackAnimation = 111
                        deathAnimation = 222
                    }
                }
            }
        }

        val spawn = family.spawns.single()
        assertEquals(3, spawn.walkRadius)
        assertEquals(4, spawn.attack)
        assertEquals(5, spawn.defence)
        assertEquals(6, spawn.strength)
        assertEquals(20, spawn.hitpoints)
        assertEquals(7, spawn.ranged)
        assertEquals(8, spawn.magic)
        assertEquals(45, spawn.respawnTicks)
        assertEquals(111, spawn.attackAnimation)
        assertEquals(222, spawn.deathAnimation)
        assertEquals(2, spawn.attackRange)
        assertEquals(true, spawn.alwaysActive)
        assertEquals("super-rat", spawn.profile)
    }

    @Test
    fun `spawn display block records real npc appearance effects`() {
        val family = npcFamily("Test Rat", 100) {
            spawns {
                at(3200, 3201) {
                    display(headIcon = 3, transformTo = 101)
                }
            }
        }

        val spawn = family.spawns.single()
        assertEquals(3, spawn.headIcon)
        assertEquals(101, spawn.transformTo)
    }

    @Test
    fun `definition override builder strips invalid values`() {
        val override = NpcDefinitionOverrideBuilder(100).apply {
            name = "Rat"
            examine = "no name"
        }.build()

        assertEquals("Rat", override.name)
        assertNull(override.examine)
    }

    @Test
    fun `server definition builder keeps server-applied values separate from cache overrides`() {
        val definition = NpcServerDefinitionBuilder(100).apply {
            hitpoints = 20
            defence = 0
            attack = 4
            attackAnimation = 111
            respawnTicks = 45
            display(headIcon = 2, transformTo = 101)
        }.buildDefinition()

        assertEquals(20, definition.hitpoints)
        assertEquals(0, definition.defence)
        assertEquals(4, definition.attack)
        assertEquals(111, definition.attackAnimation)
        assertEquals(45, definition.respawnTicks)
        assertEquals(2, definition.headIcon)
        assertEquals(101, definition.transformTo)
    }

    @Test
    fun `future monster json fields are documented but not live server builder properties`() {
        val source = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/npc/NpcDefinitionOverride.kt"))

        assertTrue(source.contains("maxHit"))
        assertTrue(source.contains("attackSpeed"))
        assertTrue(source.contains("slayer metadata"))
        assertFalse(Regex("""var\s+maxHit""").containsMatchIn(source))
        assertTrue(Regex("""var\s+attackSpeed""").containsMatchIn(source))
        assertTrue(Regex("""var\s+aggressive""").containsMatchIn(source))
    }

    @Test
    fun `cow event reward binds to cache visible attack option`() {
        assertEquals("attack", Cow.definition.optionLabels[5])
        assertTrue(Cow.definition.onAttack !== NO_CLICK_HANDLER)
        assertTrue(Cow.definition.onFirstClick !== NO_CLICK_HANDLER)
    }

    @Test
    fun `restored shop and dialogue npcs expose click handlers`() {
        assertEquals("talk-to", Jatix.definition.optionLabels[1])
        assertEquals("trade", Jatix.definition.optionLabels[3])
        assertTrue(Jatix.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(Jatix.definition.onThirdClick !== NO_CLICK_HANDLER)

        assertEquals("trade", Gerrant.definition.optionLabels[3])
        assertTrue(Gerrant.definition.onThirdClick !== NO_CLICK_HANDLER)

        assertEquals("trade", BowAndArrowSalesm.definition.optionLabels[3])
        assertTrue(BowAndArrowSalesm.definition.onThirdClick !== NO_CLICK_HANDLER)
    }

    @Test
    fun `restored service npcs expose their legacy actions`() {
        assertEquals("assignment", Mazchna.definition.optionLabels[3])
        assertEquals("assignment", Vannaka.definition.optionLabels[3])
        assertEquals("assignment", Duradel.definition.optionLabels[3])
        assertTrue(Mazchna.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(Vannaka.definition.onThirdClick !== NO_CLICK_HANDLER)
        assertTrue(Duradel.definition.onThirdClick !== NO_CLICK_HANDLER)

        assertTrue(Zahur.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(Zahur.definition.onThirdClick !== NO_CLICK_HANDLER)
        assertEquals("decant", Zahur.definition.optionLabels[3])
        assertEquals("clean", Zahur.definition.optionLabels[4])

        assertTrue(DukeHoracio.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(LegendsGuard.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertEquals(listOf(2727 to 3349, 2730 to 3349), LegendsGuard.spawns.map { it.x to it.y })
        assertEquals("talk-to", Tanner.definition.optionLabels[1])
        assertEquals("trade", Tanner.definition.optionLabels[3])
        assertTrue(Tanner.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(Tanner.definition.onThirdClick !== NO_CLICK_HANDLER)
        assertEquals("talk-to", Horvik.definition.optionLabels[1])
        assertEquals("trade", Horvik.definition.optionLabels[3])
        assertTrue(Horvik.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(Horvik.definition.onThirdClick !== NO_CLICK_HANDLER)
        assertEquals("talk-to", Sedridor.definition.optionLabels[1])
        assertEquals("teleport", Sedridor.definition.optionLabels[3])
        assertTrue(Sedridor.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(Sedridor.definition.onThirdClick !== NO_CLICK_HANDLER)
        assertTrue(RugMerchant.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(RugMerchant2.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(RugMerchant3.definition.onFirstClick !== NO_CLICK_HANDLER)
        assertTrue(RugMerchant4.definition.onFirstClick !== NO_CLICK_HANDLER)
    }

    @Test
    fun `social interface exposes tarnish add friend button`() {
        val binding = net.dodian.uber.game.ui.SocialInterface.bindings.single { it.componentKey == "social.friend.add" }

        assertTrue(5068 in binding.rawButtonIds)
    }

    @Test
    fun `teleport interface close button is mapped`() {
        val binding = net.dodian.uber.game.ui.UiInterface.bindings.single { it.componentKey == "ui.close_interface" }

        assertTrue(58002 in binding.rawButtonIds)
    }

    @Test
    fun `tarnish pickaxe first style gives attack xp`() {
        val primary = net.dodian.uber.game.ui.CombatInterface.bindings.single { it.componentKey == "combat.style.primary" }
        val secondary = net.dodian.uber.game.ui.CombatInterface.bindings.single { it.componentKey == "combat.style.secondary" }

        assertTrue(5567 in primary.rawButtonIds)
        assertTrue(5579 in secondary.rawButtonIds)
    }
}
