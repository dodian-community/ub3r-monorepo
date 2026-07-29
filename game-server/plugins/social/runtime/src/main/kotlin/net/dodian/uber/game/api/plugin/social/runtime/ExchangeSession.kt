package net.dodian.uber.game.api.plugin.social.runtime

import java.util.UUID
import net.dodian.uber.game.api.plugin.social.ExchangeCommandResult
import net.dodian.uber.game.api.plugin.social.ExchangeItem
import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.api.plugin.social.ExchangeParticipantId
import net.dodian.uber.game.api.plugin.social.ExchangeRejectReason
import net.dodian.uber.game.api.plugin.social.ExchangeReservation
import net.dodian.uber.game.api.plugin.social.ExchangeStage

/**
 * Game-thread-confined exchange aggregate. Containers and acceptance live here
 * rather than being mirrored on two player objects.
 */
class ExchangeSession(
    val id: UUID,
    val kind: ExchangeKind,
    val first: ExchangeParticipantId,
    val second: ExchangeParticipantId,
) {
    init {
        require(first != second) { "An exchange requires two distinct participants" }
    }

    private val reservations = linkedMapOf(
        first to mutableListOf<ExchangeReservation>(),
        second to mutableListOf(),
    )
    private val acceptedRevision = mutableMapOf<ExchangeParticipantId, Long>()
    private val rules = BooleanArray(22)
    private var terminalToken: UUID? = null

    var stage: ExchangeStage = ExchangeStage.OFFER
        private set
    var revision: Long = 0L
        private set

    fun other(participant: ExchangeParticipantId): ExchangeParticipantId? = when (participant) {
        first -> second
        second -> first
        else -> null
    }

    fun reservationSnapshot(participant: ExchangeParticipantId): List<ExchangeReservation> =
        reservations[participant]?.toList().orEmpty()

    fun offerSnapshot(participant: ExchangeParticipantId): List<ExchangeItem> {
        val result = mutableListOf<ExchangeItem>()
        reservationSnapshot(participant).forEach { reservation ->
            val existing = result.indexOfFirst { it.id == reservation.itemId }
            if (existing >= 0) {
                val item = result[existing]
                result[existing] = item.copy(amount = Math.addExact(item.amount, reservation.amount))
            } else {
                result += ExchangeItem(reservation.itemId, reservation.amount)
            }
        }
        return result
    }

    fun replaceReservations(
        participant: ExchangeParticipantId,
        items: List<ExchangeReservation>,
    ): ExchangeCommandResult {
        val target = reservations[participant]
            ?: return ExchangeCommandResult.Rejected(ExchangeRejectReason.INVALID_PARTICIPANT)
        if (stage != ExchangeStage.OFFER) {
            return ExchangeCommandResult.Rejected(ExchangeRejectReason.WRONG_STAGE)
        }
        target.clear()
        target.addAll(items)
        changed()
        return ExchangeCommandResult.Applied
    }

    fun setRule(participant: ExchangeParticipantId, index: Int, enabled: Boolean): ExchangeCommandResult {
        if (participant !in reservations) return ExchangeCommandResult.Rejected(ExchangeRejectReason.INVALID_PARTICIPANT)
        if (kind != ExchangeKind.DUEL || stage != ExchangeStage.OFFER) {
            return ExchangeCommandResult.Rejected(ExchangeRejectReason.WRONG_STAGE)
        }
        if (index !in rules.indices) return ExchangeCommandResult.Rejected(ExchangeRejectReason.INVALID_SLOT)
        if (rules[index] == enabled) return ExchangeCommandResult.AlreadyApplied
        rules[index] = enabled
        changed()
        return ExchangeCommandResult.Applied
    }

    fun rule(index: Int): Boolean = index in rules.indices && rules[index]
    fun rulesSnapshot(): List<Boolean> = rules.toList()

    fun accept(participant: ExchangeParticipantId, expectedRevision: Long): ExchangeCommandResult {
        if (participant !in reservations) return ExchangeCommandResult.Rejected(ExchangeRejectReason.INVALID_PARTICIPANT)
        if (stage != ExchangeStage.OFFER && stage != ExchangeStage.CONFIRM) {
            return ExchangeCommandResult.Rejected(ExchangeRejectReason.WRONG_STAGE)
        }
        if (expectedRevision != revision) {
            return ExchangeCommandResult.Rejected(ExchangeRejectReason.STALE_REVISION)
        }
        acceptedRevision[participant] = revision
        if (acceptedRevision[first] == revision && acceptedRevision[second] == revision) {
            stage = if (stage == ExchangeStage.OFFER) ExchangeStage.CONFIRM else ExchangeStage.SETTLING
            acceptedRevision.clear()
        }
        return ExchangeCommandResult.Applied
    }

    fun beginActive(): ExchangeCommandResult {
        if (kind != ExchangeKind.DUEL || stage != ExchangeStage.SETTLING) {
            return ExchangeCommandResult.Rejected(ExchangeRejectReason.WRONG_STAGE)
        }
        stage = ExchangeStage.ACTIVE
        return ExchangeCommandResult.Applied
    }

    fun beginSettlement(token: UUID): ExchangeCommandResult {
        if (stage == ExchangeStage.COMPLETE) {
            return if (terminalToken == token) ExchangeCommandResult.AlreadyApplied
            else ExchangeCommandResult.Rejected(ExchangeRejectReason.ALREADY_SETTLED)
        }
        if (stage != ExchangeStage.SETTLING && stage != ExchangeStage.ACTIVE) {
            return ExchangeCommandResult.Rejected(ExchangeRejectReason.WRONG_STAGE)
        }
        if (terminalToken != null) {
            return if (terminalToken == token) ExchangeCommandResult.AlreadyApplied
            else ExchangeCommandResult.Rejected(ExchangeRejectReason.ALREADY_SETTLED)
        }
        terminalToken = token
        return ExchangeCommandResult.Applied
    }

    fun complete(token: UUID): ExchangeCommandResult {
        if (terminalToken != token) return ExchangeCommandResult.Rejected(ExchangeRejectReason.ALREADY_SETTLED)
        if (stage == ExchangeStage.COMPLETE) return ExchangeCommandResult.AlreadyApplied
        stage = ExchangeStage.COMPLETE
        return ExchangeCommandResult.Applied
    }

    fun abortSettlement(token: UUID): ExchangeCommandResult {
        if (terminalToken != token || stage == ExchangeStage.COMPLETE) {
            return ExchangeCommandResult.Rejected(ExchangeRejectReason.ALREADY_SETTLED)
        }
        terminalToken = null
        stage = if (kind == ExchangeKind.DUEL) ExchangeStage.ACTIVE else ExchangeStage.CONFIRM
        acceptedRevision.clear()
        return ExchangeCommandResult.Applied
    }

    fun cancel(): ExchangeCommandResult {
        if (stage == ExchangeStage.COMPLETE || terminalToken != null) {
            return ExchangeCommandResult.Rejected(ExchangeRejectReason.ALREADY_SETTLED)
        }
        if (stage == ExchangeStage.CANCELLED) return ExchangeCommandResult.AlreadyApplied
        stage = ExchangeStage.CANCELLED
        acceptedRevision.clear()
        return ExchangeCommandResult.Applied
    }

    private fun changed() {
        revision++
        acceptedRevision.clear()
    }
}
