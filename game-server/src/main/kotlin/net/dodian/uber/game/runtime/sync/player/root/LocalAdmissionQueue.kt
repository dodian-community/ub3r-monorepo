package net.dodian.uber.game.runtime.sync.player.root

class LocalAdmissionQueue {
    fun rebuildPending(
        state: ViewerDesiredLocalState,
        desiredDiff: DesiredLocalSetDiff,
        desiredSignature: Int,
    ): Int {
        if (state.pendingAddSignature == desiredSignature &&
            state.pendingAddCount > 0 &&
            state.pendingAddHead < state.pendingAddTail
        ) {
            return state.pendingAddCount
        }

        val additions = desiredDiff.additions
        ensureCapacity(state, additions.size)
        if (additions.isEmpty()) {
            state.pendingAddHead = 0
            state.pendingAddTail = 0
            state.pendingAddCount = 0
            state.pendingAddSignature = desiredSignature
            return 0
        }

        System.arraycopy(additions, 0, state.pendingAddSlots, 0, additions.size)
        state.pendingAddHead = 0
        state.pendingAddTail = additions.size
        state.pendingAddCount = additions.size
        state.pendingAddSignature = desiredSignature
        return additions.size
    }

    fun rebuildPending(
        desiredDiff: DesiredLocalSetDiff,
        currentLocalSlots: IntArray,
    ): IntArray {
        return desiredDiff.additions
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
