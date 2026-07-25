package net.dodian.uber.game.model.entity.player

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class QuestProgressSnapshot(
    val stages: Map<String, Int> = emptyMap(),
    val attributes: Map<String, String> = emptyMap(),
)

/**
 * Persisted per-player quest progress: a stage counter and named sub-objective
 * attributes, both keyed by quest key. Unlike [PlayerContentRuntimeState] (which
 * is explicitly wiped on logout), this is the durable store quest stage
 * tracking needs - see [net.dodian.uber.game.persistence.player.PlayerSaveSnapshot]
 * and [net.dodian.uber.game.persistence.account.login.AccountLoginMapper] for
 * where it's serialized to/from the `quest_data` column.
 */
class PlayerQuestProgressState {
    private val stages = mutableMapOf<String, Int>()
    private val attributes = mutableMapOf<String, String>()

    fun stage(questKey: String): Int = stages[questKey] ?: 0
    fun setStage(questKey: String, stage: Int) { stages[questKey] = stage }

    fun attribute(questKey: String, name: String): String? = attributes[attributeKey(questKey, name)]
    fun setAttribute(questKey: String, name: String, value: String?) {
        val key = attributeKey(questKey, name)
        if (value == null) attributes.remove(key) else attributes[key] = value
    }

    fun loadFromJson(json: String) {
        stages.clear()
        attributes.clear()
        if (json.isBlank()) return
        val snapshot = runCatching { MAPPER.readValue(json, QuestProgressSnapshot::class.java) }.getOrNull() ?: return
        stages.putAll(snapshot.stages)
        attributes.putAll(snapshot.attributes)
    }

    fun saveAsJson(): String = MAPPER.writeValueAsString(QuestProgressSnapshot(stages.toMap(), attributes.toMap()))

    private fun attributeKey(questKey: String, name: String) = "$questKey.$name"

    companion object {
        private val MAPPER = jacksonObjectMapper()
    }
}
