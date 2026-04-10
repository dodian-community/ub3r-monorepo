package net.dodian.uber.game.engine.sync.playerinfo.admission

import net.dodian.uber.game.engine.sync.playerinfo.state.ViewerDesiredLocalState

class LocalAdmissionQueue {
    fun rebuildPending(
        state: ViewerDesiredLocalState,
        desiredDiff: DesiredLocalSetDiff,
        queueSignature: Int,
    ): Int {
        if (state.pendingAddSignature == queueSignature &&
            state.pendingAddCount > 0 &&
            state.pendingAddHead < state.pendingAddTail
        ) {
            return state.pendingAddCount
        }

        val additions = desiredDiff.additions
        val additionsCount = desiredDiff.additionsCount
        val reinserts = desiredDiff.reinserts
        val reinsertsCount = desiredDiff.reinsertsCount
        val totalCount = reinsertsCount + additionsCount
        ensureCapacity(state, totalCount)
        if (totalCount <= 0) {
            state.pendingAddHead = 0
            state.pendingAddTail = 0
            state.pendingAddCount = 0
            state.pendingAddSignature = queueSignature
            return 0
        }

        if (reinsertsCount > 0) {
            System.arraycopy(reinserts, 0, state.pendingAddSlots, 0, reinsertsCount)
        }
        if (additionsCount > 0) {
            System.arraycopy(additions, 0, state.pendingAddSlots, reinsertsCount, additionsCount)
        }
        state.pendingAddHead = 0
        state.pendingAddTail = totalCount
        state.pendingAddCount = totalCount
        state.pendingAddSignature = queueSignature
        return totalCount
    }

    @Suppress("UNUSED_PARAMETER")
    fun rebuildPending(
        desiredDiff: DesiredLocalSetDiff,
        currentLocalSlots: IntArray,
    ): IntArray {
        val totalCount = desiredDiff.totalAdmissionsCount
        if (totalCount <= 0) {
            return IntArray(0)
        }
        val pending = IntArray(totalCount)
        if (desiredDiff.reinsertsCount > 0) {
            System.arraycopy(desiredDiff.reinserts, 0, pending, 0, desiredDiff.reinsertsCount)
        }
        if (desiredDiff.additionsCount > 0) {
            System.arraycopy(
                desiredDiff.additions,
                0,
                pending,
                desiredDiff.reinsertsCount,
                desiredDiff.additionsCount,
            )
        }
        return pending
    }

    fun drainPending(
        state: ViewerDesiredLocalState,
        maxAdds: Int,
    ): LocalAdmissionBatch {
        if (state.pendingAddCount <= 0 || state.pendingAddHead >= state.pendingAddTail) {
            return LocalAdmissionBatch(
                sentSlots = IntArray(0),
                pendingCount = 0,
                progress = AdmissionProgressState(0, 0, 0),
            )
        }

        val sentCount = maxAdds.coerceAtMost(state.pendingAddCount)
        val sent = IntArray(sentCount)
        for (index in 0 until sentCount) {
            sent[index] = state.pendingAddSlots[state.pendingAddHead + index]
        }
        state.pendingAddHead += sentCount
        state.pendingAddCount -= sentCount
        if (state.pendingAddCount == 0) {
            state.pendingAddHead = 0
            state.pendingAddTail = 0
        }

        return LocalAdmissionBatch(
            sentSlots = sent,
            pendingCount = state.pendingAddCount,
            progress = AdmissionProgressState(
                totalPending = sentCount + state.pendingAddCount,
                sentCount = sentCount,
                deferredCount = state.pendingAddCount,
            ),
        )
    }

    private fun ensureCapacity(state: ViewerDesiredLocalState, required: Int) {
        if (state.pendingAddSlots.size >= required) {
            return
        }
        var newSize = state.pendingAddSlots.size.coerceAtLeast(16)
        while (newSize < required) {
            newSize *= 2
        }
        state.pendingAddSlots = state.pendingAddSlots.copyOf(newSize)
    }
}
