package net.dodian.uber.game.content.dialogue

import net.dodian.uber.game.model.entity.player.Client

/**
 * Queue/chain dialogue authoring API inspired by Tarnish, implemented in Kotlin.
 *
 * This is a pure builder that emits [DialogueStep]s. Runtime state is managed by [DialogueService].
 */
class DialogueFactory {
    internal val steps: MutableList<DialogueStep> = mutableListOf()

    fun npcChat(npcId: Int, emote: Int, text: String) {
        steps += DialogueStep.NpcChat(npcId = npcId, emote = emote, text = text)
    }

    fun npcChat(npcId: Int, emote: Int, vararg lines: String) {
        npcChat(npcId, emote, lines.joinToString("\n"))
    }

    fun playerChat(emote: Int, text: String) {
        steps += DialogueStep.PlayerChat(emote = emote, text = text)
    }

    fun playerChat(emote: Int, vararg lines: String) {
        playerChat(emote, lines.joinToString("\n"))
    }

    fun options(
        title: String = "Select an Option",
        vararg options: DialogueOption,
    ) {
        require(options.size in 2..5) { "Options must be 2..5 (got ${options.size})" }
        val built = options.map { opt ->
            DialogueStep.Option(text = opt.text, steps = buildSteps(opt.build))
        }
        steps += DialogueStep.Options(title = title, options = built)
    }

    fun action(action: (Client) -> Unit) {
        steps += DialogueStep.Action(action)
    }

    fun finish(closeInterfaces: Boolean = true, action: (Client) -> Unit = {}) {
        steps += DialogueStep.Finish(closeInterfaces = closeInterfaces, action = action)
    }

    /**
     * Clears remaining queued steps at runtime and replaces them with [builder]'s steps.
     */
    fun restart(builder: DialogueFactory.() -> Unit) {
        steps += DialogueStep.Restart(steps = buildSteps(builder))
    }

    private fun buildSteps(builder: DialogueFactory.() -> Unit): List<DialogueStep> {
        val nested = DialogueFactory().apply(builder)
        return nested.steps.toList()
    }
}

data class DialogueOption(
    val text: String,
    val build: DialogueFactory.() -> Unit,
)

