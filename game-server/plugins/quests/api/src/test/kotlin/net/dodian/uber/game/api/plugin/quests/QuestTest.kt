package net.dodian.uber.game.api.plugin.quests

import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.api.content.ContentAttributes
import net.dodian.uber.game.api.plugin.skills.*
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiSelection
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Minimal in-memory [QuestPlayer] double, local to this module (quests/testkit depends on quests/api, not the reverse). */
private class MinimalQuestPlayer : QuestPlayer {
    private val stages = mutableMapOf<String, Int>()
    private val grantedXp = mutableMapOf<Skill, Int>()
    private val items = mutableMapOf<Int, Int>()

    override val questProgress = object : QuestProgress {
        override fun stage(questKey: String) = stages[questKey] ?: 0
        override fun setStage(questKey: String, stage: Int) { stages[questKey] = stage }
        override fun advance(questKey: String, amount: Int): Int {
            val next = stage(questKey) + amount
            setStage(questKey, next)
            return next
        }
        override fun <T> attribute(questKey: String, name: String, default: T): QuestAttributeHandle<T> {
            var current = default
            return object : QuestAttributeHandle<T> {
                override fun get(): T = current
                override fun set(value: T) { current = value }
                override fun reset(): T { current = default; return current }
                override fun clear() { current = default }
            }
        }
    }

    override val skills = object : SkillLevels {
        override fun current(skill: Skill) = 1
        override fun base(skill: Skill) = 1
        override fun experience(skill: Skill) = grantedXp[skill] ?: 0
        override fun gainXp(amount: Int, skill: Skill): Boolean {
            grantedXp[skill] = (grantedXp[skill] ?: 0) + amount
            return true
        }
    }
    override val inventory = object : SkillInventory {
        override fun contains(itemId: Int, amount: Int) = amount(itemId) >= amount
        override fun amount(itemId: Int) = items[itemId] ?: 0
        override fun slotAmount(slot: Int, itemId: Int) = 0
        override fun freeSlots() = 28
        override fun add(itemId: Int, amount: Int): Boolean { items[itemId] = (items[itemId] ?: 0) + amount; return true }
        override fun remove(itemId: Int, amount: Int) = false
        override fun transaction(block: SkillInventoryTransaction.() -> Unit) = false
        override fun itemName(itemId: Int) = "item $itemId"
        override fun notedItemId(itemId: Int) = itemId
        override fun refresh() {}
    }
    override val equipment = object : SkillEquipment {
        override fun item(slot: Int) = -1
        override fun amount(slot: Int) = 0
        override fun remove(slot: Int, itemId: Int, amount: Int) = false
        override fun refresh() {}
    }
    override val actions = object : SkillActions {
        override fun animate(id: Int, delay: Int) {}
        override fun queue(spec: SkillActionSpec, beforeStart: () -> Unit) = null
        override fun stop() {}
        override fun lockMovement(locked: Boolean) {}
        override fun beginSession(key: String) = true
        override fun endSession(key: String) {}
        override fun activeSessionKey(): String? = null
        override fun triggerRandomEvent(experience: Int) {}
        override fun logGathering(itemId: Int, amount: Int, reason: String) {}
        override fun yell(message: String) {}
        override fun tryThrottle(key: String, cooldownMs: Long) = true
        override fun markMagicCastCycle() {}
    }
    override val ui = object : SkillUi {
        override fun message(text: String) {}
        override fun string(text: String, componentId: Int) {}
        override fun open(interfaceId: Int) {}
        override fun close() {}
        override fun chatbox(interfaceId: Int) {}
        override fun itemModel(componentId: Int, zoom: Int, itemId: Int) {}
        override fun itemGrid(componentId: Int, entries: List<SkillItemGridEntry>) {}
        override fun componentVisible(componentId: Int, visible: Boolean) {}
        override fun menuGrid(entries: List<SkillItemGridEntry>) {}
        override fun npcDialogue(dialogueId: Int, npcId: Int) {}
        override fun openDialogue(dialogueId: Int) {}
        override fun varbit(id: Int, value: Int) {}
        override fun itemListMenu(items: List<Int>, onSelect: (Int) -> Unit) {}
        override fun skillGuide(skillId: Int, tab: Int) {}
        override fun guideBook() {}
        override fun currentSkillGuideSkillId() = -1
        override fun dialogue(builder: SkillDialogueFactory.() -> Unit) {}
    }
    override val world = object : SkillWorld {
        override val position = SkillPosition(3200, 3200, 0)
        override fun distanceTo(x: Int, y: Int) = 0
        override fun teleport(destination: SkillPosition) {}
        override fun resetPosition() {}
        override fun anchor(position: SkillPosition) {}
        override fun face(position: SkillPosition) {}
        override fun graphic(id: Int, height: Int) {}
        override fun graphic(id: Int, position: SkillPosition, height: Int) {}
        override fun castGraphic(id: Int, height: Int) {}
        override fun replaceObject(
            target: SkillObjectRef,
            replacementId: Int,
            restoreTicks: Int,
            type: Int,
            face: Int,
        ) {}
        override fun withinObjectBoundary(target: SkillObjectRef) = true
        override fun spawnTemporaryObject(objectId: Int, durationTicks: Int, onExpire: () -> Unit) {}
        override fun dropItem(itemId: Int, amount: Int) {}
        override fun dropItemAt(position: SkillPosition, itemId: Int, amount: Int) {}
        override fun traverse(deltaX: Int, deltaY: Int, durationMs: Int, movementAnimationId: Int?, startAnimationId: Int?, arrivalAnimationId: Int?, onComplete: () -> Unit) {}
        override fun climb(destination: SkillPosition, animationId: Int, delayMs: Int, onComplete: () -> Unit) {}
    }
    override val production = object : SkillProduction {
        override fun open(config: SkillMultiConfig, onSelected: (SkillMultiSelection) -> Unit) = false
        override fun select(selection: SkillMultiSelection) = false
        override fun pending(): SkillMultiConfig? = null
        override fun clear() {}
    }
    override val amountPrompt = object : SkillAmountPrompt {
        override fun request(title: String, maxDigits: Int, onEntered: (Int) -> Boolean) = false
        override fun cancel() {}
    }
    override val profile = object : SkillProfile { override val name = "test"; override val premium = false; override val combatLevel = 3 }
    override val random = object : SkillRandom {
        override fun between(minInclusive: Int, maxInclusive: Int) = minInclusive
        override fun chance(numerator: Int, denominator: Int) = false
    }
    override val clock = SkillClock { 0L }
    private val activePrayers = mutableSetOf<SkillPrayer>()
    override val vitals = object : SkillVitals {
        override val currentPrayer = 0
        override val maximumPrayer = 0
        override val prayerBonus = 0
        override val inDuel = false
        override val isDead = false
        override fun isPrayerActive(prayer: SkillPrayer) = prayer in activePrayers
        override fun togglePrayer(prayer: SkillPrayer) { if (!activePrayers.add(prayer)) activePrayers.remove(prayer) }
        override fun deactivatePrayer(prayer: SkillPrayer) { activePrayers.remove(prayer) }
        override fun setPrayer(amount: Int) {}
        override fun damage(amount: Int) {}
        override fun restorePrayer(amount: Int) {}
        override fun stun(ticks: Int) {}
    }
    override val slayerTask = object : SkillSlayerTask {
        override val masterNpcId = -1
        override val ordinal = -1
        override val assignmentAmount = 0
        override val taskName = ""
        override val remainingAmount = 0
        override val streak = 0
        override fun killCount() = -1
        override fun resetKillCount() = false
        override fun assign(masterNpcId: Int, ordinal: Int, amount: Int) {}
        override fun cancel() {}
    }
    override val runePouches = object : SkillRunePouches {
        override fun amount(slot: Int) = 0
        override fun addAmount(slot: Int, amount: Int) {}
        override fun levelRequirement(slot: Int) = 1
        override fun capacity(slot: Int) = 0
    }
    override val farmingState = object : SkillFarmingState {
        override fun patchSlots() = emptyList<SkillPatchSlot>()
        override fun writePatchSlot(patchName: String, slot: Int, itemId: Int, state: String, compost: String, stageOrLife: Int, progress: Int, plantedBy: Int) {}
        override fun compostBins() = emptyList<SkillCompostBinState>()
        override fun writeCompostBin(binName: String, compost: String, state: String, amount: Int, progress: Int) {}
        override fun markDirty() {}
        override fun refreshVisuals() {}
        override fun notifyInteraction() {}
    }
    override val attributes = object : ContentAttributes {
        override fun <T : Any> get(key: ContentAttributeKey<T>): T? = null
        override fun <T : Any> put(key: ContentAttributeKey<T>, value: T) {}
        override fun remove(key: ContentAttributeKey<*>) {}
    }
    override val moduleState = object : SkillModuleState {
        override fun get(moduleId: String, key: String): String? = null
        override fun put(moduleId: String, key: String, value: String) {}
        override fun remove(moduleId: String, key: String) {}
    }
}

class QuestTest {
    @AfterEach
    fun resetRegistry() = Quest.resetForTests()

    @Test
    fun `advancing to maxSteps grants rewards once`() {
        val quest = Quest.register(1, "test_quest", "Test Quest", maxSteps = 2, rewards = rewards {
            xp(Skill.COOKING, 300)
            item(1891, 1)
        })
        val player = MinimalQuestPlayer()

        assertEquals(QuestState.NOT_STARTED, quest.questState(player))
        quest.advanceQuestStage(player)
        assertEquals(QuestState.IN_PROGRESS, quest.questState(player))
        quest.advanceQuestStage(player)
        assertEquals(QuestState.FINISHED, quest.questState(player))
        assertTrue(quest.isQuestCompleted(player))
        assertEquals(300, player.skills.experience(Skill.COOKING))
        assertTrue(player.inventory.contains(1891))

        // Advancing further must not clamp past maxSteps or re-grant rewards.
        quest.advanceQuestStage(player, 5)
        assertEquals(2, quest.getQuestStage(player))
        assertEquals(300, player.skills.experience(Skill.COOKING))
    }

    @Test
    fun `duplicate registration is rejected`() {
        Quest.register(1, "dup", "Dup", 1)
        assertThrowsIllegalArgument { Quest.register(2, "dup", "Dup Again", 1) }
    }

    @Test
    fun `requirements helper reads quest state`() {
        val quest = Quest.register(1, "req_quest", "Req Quest", 1)
        val player = MinimalQuestPlayer()
        assertFalse(QuestRequirements.hasCompleted(player, quest.key))
        quest.advanceQuestStage(player)
        assertTrue(QuestRequirements.hasCompleted(player, quest.key))
    }

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
