package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import java.util.Collections
import java.util.WeakHashMap

enum class NpcOptionSlot {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    ATTACK,
}

data class NpcOptionBinding(
    val slot: NpcOptionSlot,
    val label: String,
    val handler: NpcClickHandler,
)

data class NpcPluginDefinition(
    val name: String,
    val npcIds: IntArray,
    val ownsSpawnDefinitions: Boolean = false,
    val entries: List<NpcSpawnDef> = emptyList(),
    val optionBindings: List<NpcOptionBinding> = emptyList(),
    val stateNamespace: String = name,
)

data class NpcPluginContext(
    val client: Client,
    val stateNamespace: String,
) {
    fun <T : Any> state(key: String): T? = NpcPluginStateStore.get(client, stateNamespace, key)

    fun setState(key: String, value: Any?) {
        NpcPluginStateStore.set(client, stateNamespace, key, value)
    }

    fun clearState(key: String) {
        NpcPluginStateStore.remove(client, stateNamespace, key)
    }
}

object NpcPluginStateStore {
    private val states = Collections.synchronizedMap(WeakHashMap<Client, MutableMap<String, Any?>>())

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(client: Client, namespace: String, key: String): T? {
        val namespaceState = states[client] ?: return null
        return namespaceState["$namespace:$key"] as? T
    }

    fun set(client: Client, namespace: String, key: String, value: Any?) {
        val namespaceState = states.getOrPut(client) { HashMap() }
        namespaceState["$namespace:$key"] = value
    }

    fun remove(client: Client, namespace: String, key: String) {
        states[client]?.remove("$namespace:$key")
    }
}

fun NpcPluginDefinition.toContentDefinition(
    explicitName: String,
    ownsSpawnDefinitionsFlag: Boolean,
): NpcContentDefinition {
    val handlersBySlot = optionBindings.groupBy { it.slot }

    fun combined(slot: NpcOptionSlot): NpcClickHandler {
        val handlers = handlersBySlot[slot].orEmpty().map { it.handler }
        if (handlers.isEmpty()) return NO_CLICK_HANDLER
        return fun(client: Client, npc: Npc): Boolean {
            for (handler in handlers) {
                if (handler(client, npc)) {
                    return true
                }
            }
            return false
        }
    }

    return NpcContentDefinition(
        name = explicitName.ifBlank { name },
        npcIds = npcIds,
        ownsSpawnDefinitions = ownsSpawnDefinitionsFlag || ownsSpawnDefinitions,
        entries = entries,
        optionLabels = optionBindings.associate { it.slot.toOptionId() to it.label },
        interactionSource = NpcInteractionSource.DSL,
        onFirstClick = combined(NpcOptionSlot.FIRST),
        onSecondClick = combined(NpcOptionSlot.SECOND),
        onThirdClick = combined(NpcOptionSlot.THIRD),
        onFourthClick = combined(NpcOptionSlot.FOURTH),
        onAttack = combined(NpcOptionSlot.ATTACK),
    )
}

private fun NpcOptionSlot.toOptionId(): Int =
    when (this) {
        NpcOptionSlot.FIRST -> 1
        NpcOptionSlot.SECOND -> 2
        NpcOptionSlot.THIRD -> 3
        NpcOptionSlot.FOURTH -> 4
        NpcOptionSlot.ATTACK -> 5
    }
