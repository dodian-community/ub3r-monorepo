package net.dodian.uber.quests.testkit

import net.dodian.uber.game.api.plugin.quests.QuestAttributeHandle
import net.dodian.uber.game.api.plugin.quests.QuestPlayer
import net.dodian.uber.game.api.plugin.quests.QuestProgress
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.skills.testkit.FakeSkillPlayer

/**
 * Protocol-free deterministic [QuestPlayer] double for quest module tests.
 * Reuses [FakeSkillPlayer] via Kotlin interface delegation for every capability
 * quests share with skills (inventory/ui/world/etc.), and adds a simple
 * in-memory (non-persisted) [questProgress] store on top.
 */
class FakeQuestPlayer(
    initialItems: Map<Int, Int> = emptyMap(),
    /** The delegate backing every [SkillPlayer] capability - exposed so tests can assert on messages, inventory, etc. */
    val fake: FakeSkillPlayer = FakeSkillPlayer(initialItems),
) : QuestPlayer, SkillPlayer by fake {
    private val stages = mutableMapOf<String, Int>()
    private val attributeValues = mutableMapOf<Pair<String, String>, Any?>()

    override val questProgress = object : QuestProgress {
        override fun stage(questKey: String): Int = stages[questKey] ?: 0
        override fun setStage(questKey: String, stage: Int) { stages[questKey] = stage }
        override fun advance(questKey: String, amount: Int): Int {
            val next = stage(questKey) + amount
            setStage(questKey, next)
            return next
        }

        override fun <T> attribute(questKey: String, name: String, default: T): QuestAttributeHandle<T> {
            val key = questKey to name
            return object : QuestAttributeHandle<T> {
                @Suppress("UNCHECKED_CAST")
                override fun get(): T = attributeValues.getOrPut(key) { default } as T
                override fun set(value: T) { attributeValues[key] = value }
                override fun reset(): T { attributeValues[key] = default; return default }
                override fun clear() { attributeValues.remove(key) }
            }
        }
    }
}
