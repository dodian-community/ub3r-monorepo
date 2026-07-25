package net.dodian.uber.game.api.plugin.quests

import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.model.player.skills.Skill

/**
 * A [SkillPlayer] extended with persisted quest-progress access. Quest content
 * reuses [SkillPlayer]'s capabilities (inventory, ui, world, dialogue, attributes)
 * instead of duplicating them - quests need the same player-facing surface as
 * skills, plus a place to read/write durable quest state.
 */
interface QuestPlayer : SkillPlayer {
    val questProgress: QuestProgress
}

/** Per-player, per-quest persisted progress. Values survive logout, unlike [SkillPlayer.attributes]. */
interface QuestProgress {
    fun stage(questKey: String): Int
    fun setStage(questKey: String, stage: Int)
    fun advance(questKey: String, amount: Int = 1): Int
    fun <T> attribute(questKey: String, name: String, default: T): QuestAttributeHandle<T>
}

/** A single persisted, named sub-objective flag scoped to one quest and one player. */
interface QuestAttributeHandle<T> {
    fun get(): T
    fun set(value: T)
    fun reset(): T
    fun clear()
}

enum class QuestState { NOT_STARTED, IN_PROGRESS, FINISHED }

data class QuestReward(
    val xp: Map<Skill, Int> = emptyMap(),
    val items: List<Pair<Int, Int>> = emptyList(),
)

class QuestRewardBuilder internal constructor() {
    private val xp = mutableMapOf<Skill, Int>()
    private val items = mutableListOf<Pair<Int, Int>>()
    fun xp(skill: Skill, amount: Int) { xp[skill] = (xp[skill] ?: 0) + amount }
    fun item(itemId: Int, amount: Int = 1) { items += itemId to amount }
    internal fun build() = QuestReward(xp.toMap(), items.toList())
}

fun rewards(block: QuestRewardBuilder.() -> Unit): QuestReward = QuestRewardBuilder().apply(block).build()

/**
 * A quest definition, registered once by its owning [QuestPlugin]. Mirrors
 * OpenRune-Server's `Quest`/`Quest.register`, minus the OSRS cache-table lookup
 * (ub3r has no cache-derived quest metadata, so definitions are plain constructor
 * calls).
 */
data class Quest(
    val id: Int,
    val key: String,
    val displayName: String,
    val maxSteps: Int,
    val rewards: QuestReward = QuestReward(),
) {
    fun getQuestStage(player: QuestPlayer): Int = player.questProgress.stage(key)

    fun questState(player: QuestPlayer): QuestState {
        val stage = getQuestStage(player)
        return when {
            stage <= 0 -> QuestState.NOT_STARTED
            stage >= maxSteps -> QuestState.FINISHED
            else -> QuestState.IN_PROGRESS
        }
    }

    fun isQuestCompleted(player: QuestPlayer): Boolean = questState(player) == QuestState.FINISHED

    /** Advances the quest's stage counter, clamped to `[0, maxSteps]`, granting [rewards] the first time it reaches [maxSteps]. */
    fun advanceQuestStage(player: QuestPlayer, amount: Int = 1): Int {
        val current = getQuestStage(player)
        val next = (current + amount).coerceIn(0, maxSteps)
        player.questProgress.setStage(key, next)
        if (next >= maxSteps && current < maxSteps) grantRewards(player)
        return next
    }

    private fun grantRewards(player: QuestPlayer) {
        rewards.xp.forEach { (skill, amount) -> player.skills.gainXp(amount, skill) }
        rewards.items.forEach { (itemId, amount) -> player.inventory.add(itemId, amount) }
    }

    companion object {
        private val byKey = linkedMapOf<String, Quest>()
        private val byId = linkedMapOf<Int, Quest>()

        fun register(id: Int, key: String, displayName: String, maxSteps: Int, rewards: QuestReward = QuestReward()): Quest {
            require(maxSteps > 0) { "Quest '$key' must declare at least one step" }
            require(key !in byKey) { "Quest key '$key' already registered by ${byKey[key]}" }
            require(id !in byId) { "Quest id $id already registered by ${byId[id]}" }
            val quest = Quest(id, key, displayName, maxSteps, rewards)
            byKey[key] = quest
            byId[id] = quest
            return quest
        }

        fun get(key: String): Quest? = byKey[key]
        fun getById(id: Int): Quest? = byId[id]
        fun all(): Collection<Quest> = byKey.values

        /** Test-only: clears the registry so unit tests can register quests without id/key collisions across test methods. */
        fun resetForTests() {
            byKey.clear()
            byId.clear()
        }
    }
}

/** Lets other content gate on quest state without depending on quest internals - mirrors OpenRune's `QuestRequirements`. */
object QuestRequirements {
    fun hasCompleted(player: QuestPlayer, questKey: String): Boolean = Quest.get(questKey)?.isQuestCompleted(player) ?: false
    fun isOnQuest(player: QuestPlayer, questKey: String): Boolean = Quest.get(questKey)?.questState(player) == QuestState.IN_PROGRESS
    fun hasNotCompleted(player: QuestPlayer, questKey: String): Boolean = !hasCompleted(player, questKey)
}
