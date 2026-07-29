package net.dodian.uber.game.api.plugin.social.runtime

import java.util.UUID
import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.api.plugin.social.ExchangeParticipantId

class ExchangeSessionRegistry {
    private val sessions = linkedMapOf<UUID, ExchangeSession>()
    private val byParticipant = linkedMapOf<ExchangeParticipantId, UUID>()

    fun create(
        kind: ExchangeKind,
        first: ExchangeParticipantId,
        second: ExchangeParticipantId,
        id: UUID = UUID.randomUUID(),
    ): ExchangeSession {
        require(first !in byParticipant && second !in byParticipant) { "Participant already has an active exchange" }
        require(id !in sessions) { "Duplicate exchange session id $id" }
        return ExchangeSession(id, kind, first, second).also {
            sessions[id] = it
            byParticipant[first] = id
            byParticipant[second] = id
        }
    }

    fun session(participant: ExchangeParticipantId): ExchangeSession? =
        byParticipant[participant]?.let(sessions::get)

    fun remove(session: ExchangeSession): Boolean {
        if (sessions.remove(session.id) !== session) return false
        byParticipant.remove(session.first, session.id)
        byParticipant.remove(session.second, session.id)
        return true
    }

    fun snapshot(): List<ExchangeSession> = sessions.values.toList()
}
