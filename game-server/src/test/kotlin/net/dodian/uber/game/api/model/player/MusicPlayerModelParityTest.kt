package net.dodian.uber.game.model.player

import net.dodian.uber.game.Server
import net.dodian.uber.game.activity.casino.SlotSpin
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.engine.systems.zone.RegionSong
import net.dodian.uber.game.activity.casino.SlotMachine
import net.dodian.uber.game.activity.casino.SlotSymbol
import net.dodian.uber.game.skill.Skillcape
import net.dodian.uber.game.ui.QuestTabEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MusicPlayerModelParityTest {
    @Test
    fun `region song resolves area and fallback`() {
        val song = RegionSong.getRegionSong(Position(2705, 3345, 0))
        assertEquals(RegionSong.VICTORIOUS_DAYS, song)

        val fallback = RegionSong.getRegionSong(Position(1, 1, 0))
        assertEquals(RegionSong.THE_LONG_JOURNEY_HOME, fallback)
    }

    @Test
    fun `skillcape lookup and trimmed checks remain stable`() {
        val untrimmed = Skillcape.getSkillCape(9747)
        val trimmed = Skillcape.getSkillCape(9748)

        assertEquals(Skillcape.ATTACK_CAPE, untrimmed)
        assertEquals(Skillcape.ATTACK_CAPE, trimmed)
        assertTrue(Skillcape.isTrimmed(9748))
    }

    @Test
    fun `quest sender lookup and uptime text cache`() {
        val sender = QuestTabEntry.getSender(7332)
        assertEquals(QuestTabEntry.PLAGUE_DOCKS, sender)

        Server.serverStartup = System.currentTimeMillis() - 120_000L
        val first = QuestTabEntry.getCachedUptimeText()
        val second = QuestTabEntry.getCachedUptimeText()
        assertEquals(first, second)
        assertTrue(first.contains("uptime"))
    }

    @Test
    fun `spin payouts are deterministic by symbol ids`() {
        Server.slots = SlotMachine()
        Server.slots.jackpotPool = 240_000
        Server.slots.houseBalance = 10_000

        val jackpotSpin = SlotSpin(arrayOf(symbol(8), symbol(8), symbol(8)))
        assertEquals(250_000, jackpotSpin.getWinnings())

        val doubleHigh = SlotSpin(arrayOf(symbol(7), symbol(7), symbol(1)))
        assertEquals(11_000, doubleHigh.getWinnings())

        val singleHigh = SlotSpin(arrayOf(symbol(7), symbol(2), symbol(3)))
        assertEquals(1_500, singleHigh.getWinnings())
    }

    @Test
    fun `slot machine spins without null symbols`() {
        val machine = SlotMachine()
        repeat(20) {
            val spin = machine.spin()
            val symbols = spin.getSymbols()
            assertEquals(3, symbols.size)
            assertNotNull(symbols[0])
            assertNotNull(symbols[1])
            assertNotNull(symbols[2])
        }
    }

    private fun symbol(id: Int): SlotSymbol = SlotSymbol(id, id.toString(), intArrayOf())
}
