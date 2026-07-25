package net.dodian.uber.game.engine.systems.quests

import net.dodian.uber.game.api.plugin.quests.QuestAttributeHandle
import net.dodian.uber.game.api.plugin.quests.QuestPlayer
import net.dodian.uber.game.api.plugin.quests.QuestProgress
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.model.entity.player.Client

/**
 * Engine-only protocol adapter. Public quest content receives [QuestPlayer],
 * never [Client]. Delegates every [SkillPlayer] capability to a fresh
 * [net.dodian.uber.game.engine.systems.skills.ClientSkillPlayerAdapter] instead
 * of duplicating it, and adds the persisted [questProgress] store on top,
 * backed by [Client.getQuestProgressState].
 */
internal class ClientQuestPlayerAdapter(private val client: Client) : QuestPlayer, SkillPlayer by client.asSkillPlayer() {
    override val questProgress = object : QuestProgress {
        override fun stage(questKey: String): Int = client.questProgressState.stage(questKey)
        override fun setStage(questKey: String, stage: Int) { client.questProgressState.setStage(questKey, stage) }
        override fun advance(questKey: String, amount: Int): Int {
            val next = stage(questKey) + amount
            setStage(questKey, next)
            return next
        }

        override fun <T> attribute(questKey: String, name: String, default: T): QuestAttributeHandle<T> =
            object : QuestAttributeHandle<T> {
                override fun get(): T = decodeAttribute(client.questProgressState.attribute(questKey, name), default)
                override fun set(value: T) { client.questProgressState.setAttribute(questKey, name, value.toString()) }
                override fun reset(): T { client.questProgressState.setAttribute(questKey, name, null); return default }
                override fun clear() { client.questProgressState.setAttribute(questKey, name, null) }
            }
    }
}

/** Only Boolean/Int/String attribute values round-trip through the persisted string store; anything else falls back to its default. */
@Suppress("UNCHECKED_CAST")
private fun <T> decodeAttribute(raw: String?, default: T): T {
    if (raw == null) return default
    return when (default) {
        is Boolean -> raw.toBooleanStrictOrNull() as? T ?: default
        is Int -> raw.toIntOrNull() as? T ?: default
        is String -> raw as T
        else -> default
    }
}

internal fun Client.asQuestPlayer(): QuestPlayer = ClientQuestPlayerAdapter(this)
