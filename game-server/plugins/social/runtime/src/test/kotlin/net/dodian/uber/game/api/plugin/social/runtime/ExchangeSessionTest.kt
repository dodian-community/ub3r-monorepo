package net.dodian.uber.game.api.plugin.social.runtime

import java.util.UUID
import net.dodian.uber.game.api.plugin.social.ExchangeCommandResult
import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.api.plugin.social.ExchangeParticipantId
import net.dodian.uber.game.api.plugin.social.ExchangeStage
import net.dodian.uber.game.api.plugin.social.ExchangeReservation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ExchangeSessionTest {
    private val first = ExchangeParticipantId(1, 1, 10)
    private val second = ExchangeParticipantId(2, 2, 20)

    @Test
    fun `offer changes invalidate acceptance and settlement is idempotent`() {
        val session = ExchangeSession(UUID.randomUUID(), ExchangeKind.TRADE, first, second)
        session.replaceReservations(first, listOf(ExchangeReservation(0, 995, 10)))
        val acceptedRevision = session.revision
        session.accept(first, acceptedRevision)
        session.replaceReservations(second, listOf(ExchangeReservation(1, 4151, 1)))
        session.accept(second, session.revision)
        assertEquals(ExchangeStage.OFFER, session.stage)
        session.accept(first, session.revision)
        assertEquals(ExchangeStage.CONFIRM, session.stage)
        session.accept(first, session.revision)
        session.accept(second, session.revision)
        val token = UUID.randomUUID()
        assertEquals(ExchangeCommandResult.Applied, session.beginSettlement(token))
        assertEquals(ExchangeCommandResult.Applied, session.complete(token))
        assertEquals(ExchangeCommandResult.AlreadyApplied, session.complete(token))
    }

    @Test
    fun `registry owns exactly one session per participant`() {
        val registry = ExchangeSessionRegistry()
        val session = registry.create(ExchangeKind.DUEL, first, second)
        assertSame(session, registry.session(first))
        assertSame(session, registry.session(second))
        registry.remove(session)
        assertEquals(null, registry.session(first))
    }

    @Test
    fun `withdraw one remains valid when an offer contains more than one`() {
        assertEquals(1, ExchangeAmount.resolve(requested = 1, available = 5))
        assertEquals(5, ExchangeAmount.resolve(requested = 10, available = 5))
        assertEquals(null, ExchangeAmount.resolve(requested = 0, available = 5))
        assertEquals(null, ExchangeAmount.resolve(requested = -1, available = 5))
    }
}
