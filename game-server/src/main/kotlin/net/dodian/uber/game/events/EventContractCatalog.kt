package net.dodian.uber.game.events

data class EventContract(
    val kind: EventContractCatalog.Contract,
    val requiredPayloadKeys: Set<String> = emptySet(),
)

object EventContractCatalog {
    enum class Contract {
        STRICT,
        PRODUCER_ONLY,
        RESERVED_DEFINITION_ONLY,
    }

    private val strictEvents = setOf(
        "CommandEvent",
        "DialogueContinueEvent",
        "ItemDropEvent",
        "ItemExamineEvent",
        "NpcExamineEvent",
        "ObjectExamineEvent",
    )

    private val producerOnlyEvents = setOf(
        "ButtonClickEvent",
        "DialogueOptionEvent",
        "ItemClickEvent",
        "ItemOnItemEvent",
        "ItemOnNpcEvent",
        "ItemOnObjectEvent",
        "ItemOnPlayerEvent",
        "ItemPickupEvent",
        "LevelUpEvent",
        "MagicOnNpcEvent",
        "MagicOnObjectEvent",
        "MagicOnPlayerEvent",
        "NpcClickEvent",
        "NpcDeathEvent",
        "NpcDropEvent",
        "ObjectClickEvent",
        "PlayerAttackEvent",
        "PlayerDeathEvent",
        "PlayerLoginEvent",
        "PlayerLogoutEvent",
        "SkillActionCompleteEvent",
        "SkillActionInterruptEvent",
        "SkillActionStartEvent",
        "SkillProgressAppliedEvent",
        "SkillingActionCycleEvent",
        "SkillingActionStartedEvent",
        "SkillingActionStoppedEvent",
        "SkillingActionSucceededEvent",
        "TradeCancelEvent",
        "TradeCompleteEvent",
        "TradeRequestEvent",
        "ChatMessageEvent",
        "PrivateMessageEvent",
        "WalkEvent",
        "WorldTickEvent",
    )

    private val reservedDefinitionOnlyEvents = setOf(
        "PlayerTickEvent",
    )

    private val payloadRequirements = mapOf(
        "SkillActionCompleteEvent" to setOf("client", "actionName"),
        "SkillActionInterruptEvent" to setOf("client", "actionName", "reason"),
        "SkillActionStartEvent" to setOf("client", "actionName"),
        "SkillProgressAppliedEvent" to setOf("client", "skill", "mode"),
        "SkillingActionCycleEvent" to setOf("client", "actionName"),
        "SkillingActionStartedEvent" to setOf("client", "actionName"),
        "SkillingActionStoppedEvent" to setOf("client", "actionName", "reason"),
        "SkillingActionSucceededEvent" to setOf("client", "actionName"),
    )

    @JvmField
    val contractsByEventSimpleName: Map<String, EventContract> =
        buildMap {
            strictEvents.forEach { eventName ->
                put(eventName, EventContract(Contract.STRICT, payloadRequirements[eventName] ?: emptySet()))
            }
            producerOnlyEvents.forEach { eventName ->
                put(eventName, EventContract(Contract.PRODUCER_ONLY, payloadRequirements[eventName] ?: emptySet()))
            }
            reservedDefinitionOnlyEvents.forEach { eventName ->
                put(eventName, EventContract(Contract.RESERVED_DEFINITION_ONLY, payloadRequirements[eventName] ?: emptySet()))
            }
        }
}
