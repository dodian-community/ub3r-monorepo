package net.dodian.uber.game.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

typealias NpcClickHandlerWithState = NpcPluginContext.(Client, Npc) -> Boolean

class NpcOptionsBuilder internal constructor(
    private val stateNamespace: String,
) {
    private val bindings = ArrayList<NpcOptionBinding>()

    fun first(label: String, handler: (Client, Npc) -> Boolean) = bind(NpcOptionSlot.FIRST, label, handler)

    fun second(label: String, handler: (Client, Npc) -> Boolean) = bind(NpcOptionSlot.SECOND, label, handler)

    fun third(label: String, handler: (Client, Npc) -> Boolean) = bind(NpcOptionSlot.THIRD, label, handler)

    fun fourth(label: String, handler: (Client, Npc) -> Boolean) = bind(NpcOptionSlot.FOURTH, label, handler)

    fun attack(label: String, handler: (Client, Npc) -> Boolean) = bind(NpcOptionSlot.ATTACK, label, handler)

    fun first(
        label: String,
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = bind(NpcOptionSlot.FIRST, label, buildSequentialHandler(init))

    fun second(
        label: String,
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = bind(NpcOptionSlot.SECOND, label, buildSequentialHandler(init))

    fun third(
        label: String,
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = bind(NpcOptionSlot.THIRD, label, buildSequentialHandler(init))

    fun fourth(
        label: String,
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = bind(NpcOptionSlot.FOURTH, label, buildSequentialHandler(init))

    fun attack(
        label: String,
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = bind(NpcOptionSlot.ATTACK, label, buildSequentialHandler(init))

    fun talkTo(
        label: String = "talk-to",
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = first(label, init)

    fun trade(
        label: String = "trade",
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = second(label, init)

    fun teleportOption(
        label: String = "teleport",
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = third(label, init)

    fun option4(
        label: String = "option-4",
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = fourth(label, init)

    fun attackOption(
        label: String = "attack",
        init: NpcSequentialOptionBuilder.() -> Unit,
    ) = attack(label, init)

    fun firstState(label: String, handler: NpcClickHandlerWithState) = bindWithState(NpcOptionSlot.FIRST, label, handler)

    fun secondState(label: String, handler: NpcClickHandlerWithState) = bindWithState(NpcOptionSlot.SECOND, label, handler)

    fun thirdState(label: String, handler: NpcClickHandlerWithState) = bindWithState(NpcOptionSlot.THIRD, label, handler)

    fun fourthState(label: String, handler: NpcClickHandlerWithState) = bindWithState(NpcOptionSlot.FOURTH, label, handler)

    fun attackState(label: String, handler: NpcClickHandlerWithState) = bindWithState(NpcOptionSlot.ATTACK, label, handler)

    internal fun build(): List<NpcOptionBinding> = bindings.toList()

    private fun bindWithState(slot: NpcOptionSlot, label: String, handler: NpcClickHandlerWithState) {
        require(bindings.none { it.slot == slot }) { "Duplicate option binding for slot=$slot label='$label'." }
        bindings += NpcOptionBinding(slot, label) { client, npc ->
            val context = NpcPluginContext(client = client, stateNamespace = stateNamespace)
            handler(context, client, npc)
        }
    }

    private fun bind(slot: NpcOptionSlot, label: String, handler: (Client, Npc) -> Boolean) {
        require(bindings.none { it.slot == slot }) { "Duplicate option binding for slot=$slot label='$label'." }
        bindings += NpcOptionBinding(slot, label, handler)
    }
}

class NpcSpawnsBuilder internal constructor() {
    private val entries = ArrayList<NpcSpawnDef>()

    fun spawn(npcId: Int, x: Int, y: Int, z: Int = 0, face: Int = 0) {
        entries += NpcSpawnDef(npcId = npcId, x = x, y = y, z = z, face = face)
    }

    fun addAll(spawns: List<NpcSpawnDef>) {
        entries += spawns
    }

    internal fun build(): List<NpcSpawnDef> = entries.toList()
}

class NpcPluginBuilder internal constructor(
    private val name: String,
) {
    private val npcIds = LinkedHashSet<Int>()
    private var ownsSpawnDefinitions: Boolean = false
    private var entries: List<NpcSpawnDef> = emptyList()
    private var optionBindings: List<NpcOptionBinding> = emptyList()
    private var stateNamespace: String = name

    fun ids(vararg ids: Int) {
        ids.forEach { npcIds += it }
    }

    fun ownsSpawns(value: Boolean = true) {
        ownsSpawnDefinitions = value
    }

    fun spawns(entries: List<NpcSpawnDef>) {
        this.entries = entries
        entries.forEach { npcIds += it.npcId }
    }

    fun spawns(block: NpcSpawnsBuilder.() -> Unit) {
        val builder = NpcSpawnsBuilder()
        builder.block()
        spawns(builder.build())
    }

    fun options(block: NpcOptionsBuilder.() -> Unit) {
        val builder = NpcOptionsBuilder(stateNamespace = stateNamespace)
        builder.block()
        optionBindings = builder.build()
    }

    fun state(namespace: String) {
        stateNamespace = namespace
    }

    fun build(): NpcPluginDefinition {
        return NpcPluginDefinition(
            name = name,
            npcIds = npcIds.toIntArray(),
            ownsSpawnDefinitions = ownsSpawnDefinitions,
            entries = entries,
            optionBindings = optionBindings,
            stateNamespace = stateNamespace,
        )
    }
}

fun npcPlugin(name: String, init: NpcPluginBuilder.() -> Unit): NpcPluginDefinition =
    NpcPluginBuilder(name).apply(init).build()
