package net.dodian.uber.game.npc

import net.dodian.uber.game.activity.partyroom.PartyRoomBalloons
import net.dodian.uber.game.api.content.dialogue.DialogueEmote
import net.dodian.uber.game.api.content.dialogue.DialogueFactory
import net.dodian.uber.game.api.content.dialogue.DialogueOption
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.interaction.npcs.NpcInteractionActionService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.shop.ShopId

enum class NpcDialogueFinishMode {
    FINISH,
    FINISH_THEN,
}

typealias NpcRuntimeAction = (Client, Npc) -> Unit

class NpcActionChainBuilder {
    private val actions = ArrayList<NpcRuntimeAction>()

    fun openShop(shopId: Int) {
        actions += { client, _ -> NpcInteractionActionService.openShop(client, shopId) }
    }

    fun openShop(shopId: ShopId) {
        openShop(shopId.id)
    }

    fun openBank() {
        actions += { client, _ -> NpcInteractionActionService.openBank(client) }
    }

    fun teleport(
        x: Int,
        y: Int,
        z: Int = 0,
        message: String? = null,
        randomXRange: Int = 0,
        randomYRange: Int = 0,
    ) {
        actions += { client, _ ->
            val finalX = x + randomOffset(randomXRange)
            val finalY = y + randomOffset(randomYRange)
            NpcInteractionActionService.teleport(client, finalX, finalY, z, message)
        }
    }

    fun sendMessage(message: String) {
        actions += { client, _ -> NpcInteractionActionService.sendMessage(client, message) }
    }

    fun clientMessage(message: String) {
        actions += { client, _ ->
            if (message.isNotBlank()) {
                client.sendMessage(message)
            }
        }
    }

    fun custom(action: NpcRuntimeAction) {
        actions += action
    }

    fun logic(block: NpcConditionalActionBuilder.() -> Unit) {
        val conditional = NpcConditionalActionBuilder().apply(block).build()
        actions += conditional
    }

    internal fun build(): List<NpcRuntimeAction> = actions.toList()

    private fun randomOffset(range: Int): Int {
        if (range <= 0) return 0
        val width = range * 2 + 1
        return (Math.random() * width).toInt() - range
    }
}

class NpcConditionalActionBuilder {
    private data class Branch(
        val predicate: (Client, Npc) -> Boolean,
        val actions: List<NpcRuntimeAction>,
    )

    private val branches = ArrayList<Branch>()
    private var defaultActions: List<NpcRuntimeAction> = emptyList()

    fun ifCase(
        predicate: (Client, Npc) -> Boolean,
        block: NpcActionChainBuilder.() -> Unit,
    ) {
        branches += Branch(predicate = predicate, actions = NpcActionChainBuilder().apply(block).build())
    }

    fun elseCase(block: NpcActionChainBuilder.() -> Unit) {
        defaultActions = NpcActionChainBuilder().apply(block).build()
    }

    internal fun build(): NpcRuntimeAction = { client, npc ->
        val branch = branches.firstOrNull { it.predicate(client, npc) }
        val actions = branch?.actions ?: defaultActions
        for (action in actions) {
            action(client, npc)
        }
    }
}

class NpcDialogueFlowBuilder {
    private val labels = LinkedHashSet<String>()

    fun npc(
        label: String,
        text: String,
    ) {
        require(label.isNotBlank()) { "Dialogue labels must be non-blank." }
        require(labels.add(label)) { "Duplicate dialogue label '$label'." }
    }

    internal fun build(): NpcDialogueFlow = NpcDialogueFlow()
}

class NpcDialogueFlow internal constructor()

fun npcDialogue(init: NpcDialogueFlowBuilder.() -> Unit): NpcDialogueFlow =
    NpcDialogueFlowBuilder().apply(init).build()

internal sealed interface NpcSequentialStep

internal data class SeqNpcChat(
    val text: String,
    val emote: Int,
) : NpcSequentialStep

internal data class SeqPlayerChat(
    val text: String,
    val emote: Int,
) : NpcSequentialStep

internal data class SeqChoice(
    val title: String,
    val options: List<SeqChoiceOption>,
) : NpcSequentialStep

internal data class SeqChoiceOption(
    val text: String,
    val steps: List<NpcSequentialStep>,
)

internal data class SeqActions(
    val actions: List<NpcRuntimeAction>,
) : NpcSequentialStep

internal data class SeqCondition(
    val predicate: (Client, Npc) -> Boolean,
    val thenSteps: List<NpcSequentialStep>,
    val elseSteps: List<NpcSequentialStep>,
) : NpcSequentialStep

internal data class SeqFinish(
    val mode: NpcDialogueFinishMode,
    val closeInterfaces: Boolean,
    val actions: List<NpcRuntimeAction>,
) : NpcSequentialStep

class NpcConditionContext internal constructor(
    val client: Client,
    val npc: Npc,
) {
    fun balloonsEventActive(): Boolean = PartyRoomBalloons.isPartyEventActive()
}

class NpcSequentialChoiceBuilder {
    private val options = ArrayList<SeqChoiceOption>()

    operator fun String.invoke(block: NpcSequentialOptionBuilder.() -> Unit) {
        option(this, block)
    }

    fun option(text: String, block: NpcSequentialOptionBuilder.() -> Unit) {
        val nested = NpcSequentialOptionBuilder().apply(block)
        options += SeqChoiceOption(text = text, steps = nested.build())
    }

    internal fun build(): List<SeqChoiceOption> = options.toList()
}

class NpcConditionalSequenceBuilder internal constructor(
    private val parent: NpcSequentialOptionBuilder,
    private val predicate: (Client, Npc) -> Boolean,
    private val thenSteps: List<NpcSequentialStep>,
) {
    infix fun otherwise(block: NpcSequentialOptionBuilder.() -> Unit): NpcSequentialOptionBuilder {
        val elseSteps = NpcSequentialOptionBuilder().apply(block).build()
        parent.addCondition(predicate, thenSteps, elseSteps)
        return parent
    }
}

class NpcSequentialOptionBuilder {
    private val steps = ArrayList<NpcSequentialStep>()

    fun npc(
        text: String,
        emote: DialogueEmote = DialogueEmote.DEFAULT,
    ) {
        steps += SeqNpcChat(text = text, emote = emote.id)
    }

    fun player(
        text: String,
        emote: DialogueEmote = DialogueEmote.DEFAULT,
    ) {
        steps += SeqPlayerChat(text = text, emote = emote.id)
    }

    fun choice(
        title: String = "Select an Option",
        init: NpcSequentialChoiceBuilder.() -> Unit,
    ) {
        val built = NpcSequentialChoiceBuilder().apply(init).build()
        require(built.size in 2..5) { "Choice '$title' must have 2..5 options." }
        steps += SeqChoice(title = title, options = built)
    }

    fun openShop(shopId: Int) {
        action { openShop(shopId) }
    }

    fun openShop(shopId: ShopId) {
        action { openShop(shopId) }
    }

    fun openBank() {
        action { openBank() }
    }

    fun teleport(
        x: Int,
        y: Int,
        z: Int = 0,
        random: Int = 0,
        message: String? = null,
    ) {
        action {
            teleport(
                x = x,
                y = y,
                z = z,
                randomXRange = random,
                randomYRange = random,
                message = message,
            )
        }
    }

    fun sendMessage(message: String) {
        action { sendMessage(message) }
    }

    fun action(block: NpcActionChainBuilder.() -> Unit) {
        steps += SeqActions(actions = NpcActionChainBuilder().apply(block).build())
    }

    fun finish(
        closeInterfaces: Boolean = true,
        actions: NpcActionChainBuilder.() -> Unit = {},
    ) {
        steps +=
            SeqFinish(
                mode = NpcDialogueFinishMode.FINISH,
                closeInterfaces = closeInterfaces,
                actions = NpcActionChainBuilder().apply(actions).build(),
            )
    }

    fun finishThen(
        closeInterfaces: Boolean = true,
        actions: NpcActionChainBuilder.() -> Unit = {},
    ) {
        steps +=
            SeqFinish(
                mode = NpcDialogueFinishMode.FINISH_THEN,
                closeInterfaces = closeInterfaces,
                actions = NpcActionChainBuilder().apply(actions).build(),
            )
    }

    fun whenCondition(
        predicate: NpcConditionContext.() -> Boolean,
        thenBlock: NpcSequentialOptionBuilder.() -> Unit,
    ): NpcConditionalSequenceBuilder {
        val thenSteps = NpcSequentialOptionBuilder().apply(thenBlock).build()
        val compiled: (Client, Npc) -> Boolean = { client, npc -> NpcConditionContext(client, npc).predicate() }
        return NpcConditionalSequenceBuilder(parent = this, predicate = compiled, thenSteps = thenSteps)
    }

    internal fun addCondition(
        predicate: (Client, Npc) -> Boolean,
        thenSteps: List<NpcSequentialStep>,
        elseSteps: List<NpcSequentialStep>,
    ) {
        steps += SeqCondition(predicate = predicate, thenSteps = thenSteps, elseSteps = elseSteps)
    }

    internal fun build(): List<NpcSequentialStep> = steps.toList()
}

private fun containsDialogueSteps(steps: List<NpcSequentialStep>): Boolean {
    for (step in steps) {
        when (step) {
            is SeqNpcChat, is SeqPlayerChat, is SeqChoice -> return true
            is SeqCondition -> {
                if (containsDialogueSteps(step.thenSteps) || containsDialogueSteps(step.elseSteps)) return true
            }
            is SeqActions, is SeqFinish -> Unit
        }
    }
    return false
}

private fun hasExplicitFinish(steps: List<NpcSequentialStep>): Boolean {
    for (step in steps) {
        when (step) {
            is SeqFinish -> return true
            is SeqCondition -> {
                if (hasExplicitFinish(step.thenSteps) || hasExplicitFinish(step.elseSteps)) return true
            }
            is SeqNpcChat, is SeqPlayerChat, is SeqChoice, is SeqActions -> Unit
        }
    }
    return false
}

private fun executeImmediate(
    client: Client,
    npc: Npc,
    steps: List<NpcSequentialStep>,
) {
    for (step in resolveConditions(steps, client, npc)) {
        when (step) {
            is SeqActions -> step.actions.forEach { it(client, npc) }
            is SeqFinish -> {
                step.actions.forEach { it(client, npc) }
                return
            }
            is SeqNpcChat, is SeqPlayerChat, is SeqChoice, is SeqCondition -> Unit
        }
    }
}

private fun resolveConditions(
    steps: List<NpcSequentialStep>,
    client: Client,
    npc: Npc,
): List<NpcSequentialStep> {
    val resolved = ArrayList<NpcSequentialStep>()
    for (step in steps) {
        when (step) {
            is SeqCondition -> {
                val selected = if (step.predicate(client, npc)) step.thenSteps else step.elseSteps
                resolved += resolveConditions(selected, client, npc)
            }
            else -> resolved += step
        }
    }
    return resolved
}

private fun DialogueFactory.emitSequential(
    npc: Npc,
    steps: List<NpcSequentialStep>,
) {
    for (step in steps) {
        when (step) {
            is SeqNpcChat -> npcChat(npc.id, step.emote, step.text)
            is SeqPlayerChat -> playerChat(step.emote, step.text)
            is SeqActions -> {
                action { client -> step.actions.forEach { it(client, npc) } }
            }
            is SeqChoice -> {
                options(
                    title = step.title,
                    *step.options.map { option ->
                        DialogueOption(option.text) {
                            emitSequential(npc, option.steps)
                        }
                    }.toTypedArray(),
                )
            }
            is SeqFinish -> {
                when (step.mode) {
                    NpcDialogueFinishMode.FINISH ->
                        finish(closeInterfaces = step.closeInterfaces) { client ->
                            step.actions.forEach { it(client, npc) }
                        }
                    NpcDialogueFinishMode.FINISH_THEN ->
                        finishThen(closeInterfaces = step.closeInterfaces) { client ->
                            step.actions.forEach { it(client, npc) }
                        }
                }
            }
            is SeqCondition -> Unit
        }
    }
}

internal fun buildSequentialHandler(
    init: NpcSequentialOptionBuilder.() -> Unit,
): NpcClickHandler {
    val steps = NpcSequentialOptionBuilder().apply(init).build()
    return { client, npc ->
        val resolved = resolveConditions(steps, client, npc)
        val usesDialogue = containsDialogueSteps(resolved)
        val hasFinish = hasExplicitFinish(resolved)
        if (!usesDialogue) {
            executeImmediate(client, npc, resolved)
            true
        } else {
            DialogueService.start(client) {
                emitSequential(npc, resolved)
                if (!hasFinish) {
                    finish()
                }
            }
            true
        }
    }
}
