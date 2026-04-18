package net.dodian.uber.game.engine.state

enum class StateDomain {
    SESSION_PACKET_QUEUE,
    INTERACTION_SESSION,
    TASK_ACTION_STATE,
}

object StateOwnershipPolicy {
    @JvmField
    val ownershipByDomain: Map<StateDomain, String> =
        linkedMapOf(
            StateDomain.SESSION_PACKET_QUEUE to "Client + InboundPacketMailbox",
            StateDomain.INTERACTION_SESSION to "TradeDuelSessionService + TradeDuelStateMachine",
            StateDomain.TASK_ACTION_STATE to "GameTaskRuntime + GameTaskSet + GameTask",
        )
}
