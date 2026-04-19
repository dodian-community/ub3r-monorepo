package net.dodian.uber.game.model.player

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.systems.zone.RegionSong
import net.dodian.uber.game.content.minigames.casino.SlotMachine
import net.dodian.uber.game.content.minigames.casino.Spin
import net.dodian.uber.game.content.minigames.casino.Symbol
import net.dodian.uber.game.content.skills.Skillcape
import net.dodian.uber.game.model.player.quests.QuestSend
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
        val sender = QuestSend.getSender(7332)
        assertEquals(QuestSend.PLAGUE_DOCKS, sender)

        Server.serverStartup = System.currentTimeMillis() - 120_000L
        val first = QuestSend.getCachedUptimeText()
        val second = QuestSend.getCachedUptimeText()
        assertEquals(first, second)
        assertTrue(first.contains("uptime"))
    }

    @Test
    fun `spin payouts are deterministic by symbol ids`() {
        Server.slots = SlotMachine()
        Server.slots.slotsJackpot = 240_000
        Server.slots.peteBalance = 10_000

        val jackpotSpin = Spin(arrayOf(symbol(8), symbol(8), symbol(8)))
        assertEquals(250_000, jackpotSpin.getWinnings())

        val doubleHigh = Spin(arrayOf(symbol(7), symbol(7), symbol(1)))
        assertEquals(11_000, doubleHigh.getWinnings())

        val singleHigh = Spin(arrayOf(symbol(7), symbol(2), symbol(3)))
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

    private fun symbol(id: Int): Symbol = Symbol(id, id.toString(), intArrayOf())
}
