package net.dodian.uber.skills.testkit

import net.dodian.uber.game.api.plugin.testkit.FakeContentAttributes
import net.dodian.uber.game.api.plugin.skills.SkillActions
import net.dodian.uber.game.api.plugin.skills.SkillActionHandle
import net.dodian.uber.game.api.plugin.skills.SkillInventory
import net.dodian.uber.game.api.plugin.skills.SkillInventoryTransaction
import net.dodian.uber.game.api.plugin.skills.SkillLevels
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillUi
import net.dodian.uber.game.api.plugin.skills.SkillWorld
import net.dodian.uber.game.api.plugin.skills.SkillProduction
import net.dodian.uber.game.api.plugin.skills.SkillEquipment
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillProfile
import net.dodian.uber.game.api.plugin.skills.SkillRandom
import net.dodian.uber.game.api.plugin.skills.SkillClock
import net.dodian.uber.game.api.plugin.skills.SkillModuleState
import net.dodian.uber.game.api.plugin.skills.SkillAmountPrompt
import net.dodian.uber.game.api.plugin.skills.SkillVitals
import net.dodian.uber.game.api.plugin.skills.SkillPrayer
import net.dodian.uber.game.api.plugin.skills.SkillRunePouches
import net.dodian.uber.game.api.plugin.skills.SkillSlayerTask
import net.dodian.uber.game.api.plugin.skills.SkillDialogueFactory
import net.dodian.uber.game.api.plugin.skills.SkillDialogueStep
import net.dodian.uber.game.api.plugin.skills.SkillItemGridEntry
import net.dodian.uber.game.api.plugin.skills.SkillValidationResult
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.ActionSpec
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.requirements.ValidationResult
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiSelection
import net.dodian.uber.skills.runtime.SkillRecipePlanner

/** Protocol-free deterministic player double for modular skill tests. */
class FakeSkillPlayer(initialItems: Map<Int, Int> = emptyMap()) : SkillPlayer {
    val activePrayers = mutableSetOf<SkillPrayer>()
    private val runePouchAmounts = IntArray(4)
    val messages = mutableListOf<String>()
    val animations = mutableListOf<Pair<Int, Int>>()
    val strings = mutableMapOf<Int, String>()
    var refreshCount = 0
        private set
    var randomEventChecks = 0
        private set
    val gatheringLogs = mutableListOf<Triple<Int, Int, String>>()
    val yells = mutableListOf<String>()
    private val throttleAcquiredAtMillis = mutableMapOf<String, Long>()
    val graphicsPlayed = mutableListOf<Pair<Int, SkillPosition>>()
    val castGraphicsPlayed = mutableListOf<Pair<Int, Int>>()
    val openedDialogueIds = mutableListOf<Int>()
    var magicCastCycleMarked = false
    var positionValue = SkillPosition(3200, 3200, 0)
    var resetPositionCalls = 0
    val openedInterfaces = mutableListOf<Int>()
    val chatboxInterfaces = mutableListOf<Int>()
    val itemModels = mutableListOf<Triple<Int, Int, Int>>()
    val itemGrids = mutableMapOf<Int, List<SkillItemGridEntry>>()
    val componentVisibility = mutableMapOf<Int, Boolean>()
    var menuGrid: List<SkillItemGridEntry> = emptyList()
    val varbits = mutableMapOf<Int, Int>()
    val replacedObjects = mutableListOf<Pair<SkillObjectRef, Int>>()
    val spawnedObjects = mutableListOf<Pair<Int, Int>>()
    val groundItems = mutableListOf<Pair<Int, Int>>()
    val traversals = mutableListOf<Triple<Int, Int, Int>>()
    /** No real cache/object-shape data in tests; tests toggle this to simulate moving out of range. */
    var withinBoundaryOverride = true
    var anchor: SkillPosition? = null
    var prayerRestored = 0
    var currentPrayerValue = 1
    var maximumPrayerValue = 99
    var damageTaken = 0
    var stunTicks = 0
    var busy = false
    var nowMillis = 1L
    private val itemAmounts = initialItems.filterValues { it > 0 }.toMutableMap()
    private val xp = mutableMapOf<Skill, Int>()
    private val levels = mutableMapOf<Skill, Int>()
    private val equipmentItems = mutableMapOf<Int, Pair<Int, Int>>()
    private var sessionKey: String? = null
    private var activeAction: FakeAction? = null
    private var pendingProduction: Pair<SkillMultiConfig, (SkillMultiSelection) -> Unit>? = null
    var amountPromptTitle: String? = null
        private set
    private var pendingAmountPrompt: ((Int) -> Boolean)? = null
    private var pendingItemListMenu: Pair<List<Int>, (Int) -> Unit>? = null
    private var dialogueQueue: ArrayDeque<SkillDialogueStep>? = null
    private var pendingDialogueOptions: SkillDialogueStep.Options? = null
    /** Every npcChat line seen so far across all `ui.dialogue { ... }` calls, in order. */
    val dialogueNpcChatLines = mutableListOf<String>()
    private val moduleStateValues = mutableMapOf<String, String>()
    private class PendingExpiry(var ticksRemaining: Int, val onExpire: () -> Unit)
    private val pendingExpiries = mutableListOf<PendingExpiry>()

    private inner class FakeAction(val spec: ActionSpec) : SkillActionHandle {
        var ticksUntilCycle = 0
        var stopped = false
        override fun cancel(reason: ActionStopReason) {
            if (stopped) return
            stopped = true
            spec.onStop?.invoke(this@FakeSkillPlayer, reason)
            if (activeAction === this) activeAction = null
        }
    }

    override val skills = object : SkillLevels {
        override fun current(skill: Skill) = levels[skill] ?: 1
        override fun base(skill: Skill) = levels[skill] ?: 1
        override fun experience(skill: Skill) = xp[skill] ?: 0
        override fun gainXp(amount: Int, skill: Skill): Boolean {
            if (amount <= 0) return false
            xp[skill] = experience(skill) + amount
            return true
        }
    }
    override val moduleState = object : SkillModuleState {
        private fun id(moduleId: String, key: String) = "$moduleId:$key"
        override fun get(moduleId: String, key: String): String? = moduleStateValues[id(moduleId, key)]
        override fun put(moduleId: String, key: String, value: String) { moduleStateValues[id(moduleId, key)] = value }
        override fun remove(moduleId: String, key: String) { moduleStateValues.remove(id(moduleId, key)) }
    }
    override val inventory = object : SkillInventory {
        override fun contains(itemId: Int, amount: Int) = this@FakeSkillPlayer.amount(itemId) >= amount
        override fun amount(itemId: Int) = this@FakeSkillPlayer.amount(itemId)
        override fun freeSlots() = (28 - itemAmounts.size).coerceAtLeast(0)
        override fun slotAmount(slot: Int, itemId: Int) = if (slot >= 0) amount(itemId) else 0
        override fun add(itemId: Int, amount: Int): Boolean {
            if (itemId < 0 || amount <= 0) return false
            itemAmounts[itemId] = (itemAmounts[itemId] ?: 0) + amount
            return true
        }
        override fun remove(itemId: Int, amount: Int): Boolean {
            if (!contains(itemId, amount)) return false
            setAmount(itemId, this@FakeSkillPlayer.amount(itemId) - amount)
            return true
        }
        override fun transaction(block: SkillInventoryTransaction.() -> Unit): Boolean {
            val staged = itemAmounts.toMutableMap()
            var failed = false
            val transaction = object : SkillInventoryTransaction {
                override fun require(itemId: Int, amount: Int): Boolean {
                    val present = itemId >= 0 && amount > 0 && staged[itemId].orZero() >= amount
                    if (!present) failed = true
                    return present
                }
                override fun remove(itemId: Int, amount: Int): Boolean {
                    if (itemId < 0 || amount <= 0 || staged[itemId].orZero() < amount) return false.also { failed = true }
                    staged[itemId] = staged[itemId].orZero() - amount
                    if (staged[itemId] == 0) staged.remove(itemId)
                    return true
                }
                override fun removeAt(slot: Int, itemId: Int, amount: Int) = remove(itemId, amount)
                override fun add(itemId: Int, amount: Int): Boolean {
                    if (itemId < 0 || amount <= 0) return false.also { failed = true }
                    staged[itemId] = staged[itemId].orZero() + amount
                    return true
                }
            }
            transaction.block()
            if (failed) return false
            itemAmounts.clear(); itemAmounts.putAll(staged); refresh()
            return true
        }
        override fun itemName(itemId: Int) = "item $itemId"
        override fun notedItemId(itemId: Int) = itemId
        override fun refresh() { refreshCount++ }
    }
    override val actions = object : SkillActions {
        override val busy: Boolean get() = this@FakeSkillPlayer.busy
        override fun animate(id: Int, delay: Int) { animations += id to delay }
        override fun resetAnimation() { animations += -1 to 0 }
        override fun stop() { sessionKey = null }
        override fun lockMovement(locked: Boolean) = Unit
        override fun beginSession(key: String): Boolean = if (sessionKey == null || sessionKey == key) { sessionKey = key; true } else false
        override fun endSession(key: String) { if (sessionKey == key) sessionKey = null }
        override fun activeSessionKey(): String? = sessionKey
        override fun triggerRandomEvent(experience: Int) { if (experience > 0) randomEventChecks++ }
        override fun logGathering(itemId: Int, amount: Int, reason: String) { gatheringLogs += Triple(itemId, amount, reason) }
        override fun yell(message: String) { yells += message }
        override fun markMagicCastCycle() { magicCastCycleMarked = true }
        override fun tryThrottle(key: String, cooldownMs: Long): Boolean {
            val lastAcquired = throttleAcquiredAtMillis[key]
            if (lastAcquired != null && nowMillis - lastAcquired < cooldownMs) return false
            throttleAcquiredAtMillis[key] = nowMillis
            return true
        }
        override fun queue(spec: ActionSpec, beforeStart: () -> Unit): SkillActionHandle? {
            val failure = spec.requirements.asSequence()
                .map { it.validate(this@FakeSkillPlayer) }
                .filterIsInstance<SkillValidationResult.Failed>()
                .firstOrNull()
            if (failure != null) {
                messages += failure.message
                return null
            }
            activeAction?.cancel(ActionStopReason.USER_INTERRUPT)
            beforeStart()
            val action = FakeAction(spec)
            activeAction = action
            spec.onStart?.invoke(this@FakeSkillPlayer)
            return action
        }
    }
    override val ui = object : SkillUi {
        override fun message(text: String) { messages += text }
        override fun string(text: String, componentId: Int) { strings[componentId] = text }
        override fun open(interfaceId: Int) { openedInterfaces += interfaceId }
        override fun close() = Unit
        override fun chatbox(interfaceId: Int) { chatboxInterfaces += interfaceId }
        override fun itemModel(componentId: Int, zoom: Int, itemId: Int) { itemModels += Triple(componentId, zoom, itemId) }
        override fun itemGrid(componentId: Int, entries: List<SkillItemGridEntry>) { itemGrids[componentId] = entries }
        override fun componentVisible(componentId: Int, visible: Boolean) { componentVisibility[componentId] = visible }
        override fun menuGrid(entries: List<SkillItemGridEntry>) { this@FakeSkillPlayer.menuGrid = entries }
        override fun npcDialogue(dialogueId: Int, npcId: Int) = Unit
        override fun openDialogue(dialogueId: Int) { openedDialogueIds += dialogueId }
        override fun itemListMenu(items: List<Int>, onSelect: (Int) -> Unit) { pendingItemListMenu = items to onSelect }
        override fun varbit(id: Int, value: Int) { varbits[id] = value }
        override fun skillGuide(skillId: Int, tab: Int) {
            openedInterfaces += 8714
            skillGuideCalls += skillId to tab
            currentSkillGuideSkillIdValue = skillId
        }
        override fun guideBook() { openedInterfaces += 8134 }
        override fun currentSkillGuideSkillId(): Int = currentSkillGuideSkillIdValue
        override fun dialogue(builder: SkillDialogueFactory.() -> Unit) {
            val factory = SkillDialogueFactory().apply(builder)
            dialogueQueue = ArrayDeque(factory.steps)
            advanceDialogue()
        }
    }
    val skillGuideCalls = mutableListOf<Pair<Int, Int>>()
    var currentSkillGuideSkillIdValue = -1
    override val world = object : SkillWorld {
        override val position: SkillPosition get() = positionValue
        override fun distanceTo(x: Int, y: Int) = maxOf(kotlin.math.abs(position.x - x), kotlin.math.abs(position.y - y))
        override fun teleport(destination: SkillPosition) { positionValue = destination }
        override fun resetPosition() { resetPositionCalls++ }
        override fun anchor(position: SkillPosition) { anchor = position }
        override fun face(position: SkillPosition) = Unit
        override fun graphic(id: Int, height: Int) { graphicsPlayed += id to positionValue }
        override fun graphic(id: Int, position: SkillPosition, height: Int) { graphicsPlayed += id to position }
        override fun castGraphic(id: Int, height: Int) { castGraphicsPlayed += id to height }
        override fun replaceObject(target: SkillObjectRef, replacementId: Int, restoreTicks: Int, type: Int, face: Int) {
            replacedObjects += target to replacementId
        }
        override fun withinObjectBoundary(target: SkillObjectRef): Boolean = withinBoundaryOverride
        override fun spawnTemporaryObject(objectId: Int, durationTicks: Int, onExpire: () -> Unit) {
            spawnedObjects += objectId to durationTicks
            pendingExpiries += PendingExpiry(durationTicks, onExpire)
        }
        override fun dropItem(itemId: Int, amount: Int) { groundItems += itemId to amount }
        override fun dropItemAt(position: SkillPosition, itemId: Int, amount: Int) { groundItems += itemId to amount }
        override fun traverse(
            deltaX: Int,
            deltaY: Int,
            durationMs: Int,
            movementAnimationId: Int?,
            startAnimationId: Int?,
            arrivalAnimationId: Int?,
            onComplete: () -> Unit,
        ) {
            startAnimationId?.let { animations += it to 0 }
            movementAnimationId?.let { animations += it to 0 }
            traversals += Triple(deltaX, deltaY, durationMs)
            positionValue = SkillPosition(positionValue.x + deltaX, positionValue.y + deltaY, positionValue.z)
            pendingExpiries += PendingExpiry((durationMs / 600).coerceAtLeast(1)) {
                arrivalAnimationId?.let { animations += it to 0 }
                onComplete()
            }
        }
        override fun climb(destination: SkillPosition, animationId: Int, delayMs: Int, onComplete: () -> Unit) {
            animations += animationId to 0
            pendingExpiries += PendingExpiry((delayMs / 600).coerceAtLeast(1)) {
                positionValue = destination
                onComplete()
            }
        }
    }
    override val production = object : SkillProduction {
        override fun open(config: SkillMultiConfig, onSelected: (SkillMultiSelection) -> Unit): Boolean {
            if (SkillRecipePlanner.available(config) { amount(it) }.isEmpty()) return false
            pendingProduction = config to onSelected
            return true
        }
        override fun select(selection: SkillMultiSelection): Boolean {
            val pending = pendingProduction ?: return false
            val resolved = SkillRecipePlanner.resolve(pending.first, selection) { amount(it) } ?: run {
                clear(); return false
            }
            clear()
            pending.second(selection.copy(amount = resolved.second))
            return true
        }
        override fun pending(): SkillMultiConfig? = pendingProduction?.first
        override fun clear() { pendingProduction = null }
    }
    override val amountPrompt = object : SkillAmountPrompt {
        override fun request(title: String, maxDigits: Int, onEntered: (Int) -> Boolean): Boolean {
            require(maxDigits in 1..10)
            amountPromptTitle = title
            pendingAmountPrompt = onEntered
            return true
        }

        override fun cancel() {
            amountPromptTitle = null
            pendingAmountPrompt = null
        }
    }
    override val equipment = object : SkillEquipment {
        override fun item(slot: Int) = equipmentItems[slot]?.first ?: -1
        override fun amount(slot: Int) = equipmentItems[slot]?.second ?: 0
        override fun remove(slot: Int, itemId: Int, amount: Int): Boolean {
            val current = equipmentItems[slot] ?: return false
            if (current.first != itemId || amount <= 0 || current.second < amount) return false
            val remaining = current.second - amount
            if (remaining == 0) equipmentItems.remove(slot) else equipmentItems[slot] = itemId to remaining
            return true
        }
        override fun refresh() = Unit
    }
    var combatLevelValue = 3
    override val profile = object : SkillProfile { override val name = "fixture"; override val premium = false; override val combatLevel get() = combatLevelValue }
    override val random = object : SkillRandom {
        override fun between(minInclusive: Int, maxInclusive: Int) = minInclusive
        override fun chance(numerator: Int, denominator: Int) = numerator > 0
    }
    override val clock = SkillClock { nowMillis }
    var prayerBonusValue = 0
    var inDuelValue = false
    var isDeadValue = false
    override val vitals = object : SkillVitals {
        override val currentPrayer: Int get() = currentPrayerValue
        override val maximumPrayer: Int get() = maximumPrayerValue
        override val prayerBonus: Int get() = prayerBonusValue
        override val inDuel: Boolean get() = inDuelValue
        override val isDead: Boolean get() = isDeadValue
        override fun isPrayerActive(prayer: SkillPrayer): Boolean = prayer in activePrayers
        override fun togglePrayer(prayer: SkillPrayer) {
            if (!activePrayers.add(prayer)) activePrayers.remove(prayer)
        }
        override fun deactivatePrayer(prayer: SkillPrayer) { activePrayers.remove(prayer) }
        override fun setPrayer(amount: Int) { currentPrayerValue = amount.coerceIn(0, maximumPrayerValue) }
        override fun damage(amount: Int) { damageTaken += amount.coerceAtLeast(0) }
        override fun restorePrayer(amount: Int) {
            val restored = amount.coerceAtLeast(0).coerceAtMost(maximumPrayerValue - currentPrayerValue)
            currentPrayerValue += restored
            prayerRestored += restored
        }
        override fun stun(ticks: Int) { stunTicks = ticks.coerceAtLeast(0) }
    }
    override val runePouches = object : SkillRunePouches {
        override fun amount(slot: Int): Int = runePouchAmounts[slot]
        override fun addAmount(slot: Int, amount: Int) { runePouchAmounts[slot] += amount }
        override fun levelRequirement(slot: Int): Int = intArrayOf(1, 20, 40, 60)[slot]
        override fun capacity(slot: Int): Int = intArrayOf(4, 7, 10, 13)[slot]
    }
    var slayerMasterNpcId = -1
    var slayerTaskOrdinal = -1
    var slayerAssignmentAmount = 0
    var slayerTaskName = ""
    var slayerRemainingAmount = 0
    var slayerStreak = 0
    var slayerKillCountValue = -1
    override val slayerTask = object : SkillSlayerTask {
        override val masterNpcId: Int get() = slayerMasterNpcId
        override val ordinal: Int get() = slayerTaskOrdinal
        override val assignmentAmount: Int get() = slayerAssignmentAmount
        override val taskName: String get() = slayerTaskName
        override val remainingAmount: Int get() = slayerRemainingAmount
        override val streak: Int get() = slayerStreak
        override fun killCount(): Int = slayerKillCountValue
        override fun resetKillCount(): Boolean {
            if (slayerTaskName.isEmpty()) return false
            slayerKillCountValue = 0
            return true
        }
        override fun assign(masterNpcId: Int, ordinal: Int, amount: Int) {
            slayerMasterNpcId = masterNpcId
            slayerTaskOrdinal = ordinal
            slayerAssignmentAmount = amount
            slayerRemainingAmount = amount
        }
        override fun cancel() { slayerRemainingAmount = 0 }
    }
    private val patchSlotState = mutableMapOf<Pair<String, Int>, net.dodian.uber.game.api.plugin.skills.SkillPatchSlot>()
    private val compostBinFakeState = mutableMapOf<String, net.dodian.uber.game.api.plugin.skills.SkillCompostBinState>()
    var farmingStateDirty = false
        private set
    override val farmingState = object : net.dodian.uber.game.api.plugin.skills.SkillFarmingState {
        override fun patchSlots(): List<net.dodian.uber.game.api.plugin.skills.SkillPatchSlot> = patchSlotState.values.toList()
        override fun writePatchSlot(
            patchName: String,
            slot: Int,
            itemId: Int,
            state: String,
            compost: String,
            stageOrLife: Int,
            progress: Int,
            plantedBy: Int,
        ) {
            patchSlotState[patchName to slot] =
                net.dodian.uber.game.api.plugin.skills.SkillPatchSlot(patchName, slot, itemId, state, compost, stageOrLife, progress, plantedBy)
        }
        override fun compostBins(): List<net.dodian.uber.game.api.plugin.skills.SkillCompostBinState> = compostBinFakeState.values.toList()
        override fun writeCompostBin(binName: String, compost: String, state: String, amount: Int, progress: Int) {
            compostBinFakeState[binName] = net.dodian.uber.game.api.plugin.skills.SkillCompostBinState(binName, compost, state, amount, progress)
        }
        override fun markDirty() { farmingStateDirty = true }
        override fun refreshVisuals() { farmingRefreshVisualsCalls++ }
        override fun notifyInteraction() { farmingNotifyInteractionCalls++ }
    }
    var farmingRefreshVisualsCalls = 0
        private set
    var farmingNotifyInteractionCalls = 0
        private set

    /** Seeds a patch slot's starting state for a test - defaults match a fresh, unplanted patch. */
    fun seedPatchSlot(
        patchName: String,
        slot: Int,
        itemId: Int = -1,
        state: String = "WEED",
        compost: String = "NONE",
        stageOrLife: Int = 0,
        progress: Int = 0,
        plantedBy: Int = -1,
    ) {
        patchSlotState[patchName to slot] =
            net.dodian.uber.game.api.plugin.skills.SkillPatchSlot(patchName, slot, itemId, state, compost, stageOrLife, progress, plantedBy)
    }

    fun patchSlot(patchName: String, slot: Int): net.dodian.uber.game.api.plugin.skills.SkillPatchSlot? = patchSlotState[patchName to slot]

    /** Seeds a compost bin's starting state for a test - defaults match a fresh, empty bin. */
    fun seedCompostBin(binName: String, compost: String = "NONE", state: String = "EMPTY", amount: Int = 0, progress: Int = 0) {
        compostBinFakeState[binName] = net.dodian.uber.game.api.plugin.skills.SkillCompostBinState(binName, compost, state, amount, progress)
    }

    fun compostBin(binName: String): net.dodian.uber.game.api.plugin.skills.SkillCompostBinState? = compostBinFakeState[binName]

    override val attributes = FakeContentAttributes()

    fun amount(itemId: Int): Int = itemAmounts[itemId].orZero()
    fun submitAmount(amount: Int): Boolean {
        val prompt = pendingAmountPrompt ?: return false
        amountPromptTitle = null
        pendingAmountPrompt = null
        return prompt(amount)
    }

    fun pendingItemListMenuItems(): List<Int>? = pendingItemListMenu?.first
    fun selectItemListOption(itemId: Int): Boolean {
        val (items, onSelect) = pendingItemListMenu ?: return false
        if (itemId !in items) return false
        pendingItemListMenu = null
        onSelect(itemId)
        return true
    }

    /**
     * Drains [dialogueQueue] through non-interactive steps (npcChat/playerChat/statement/
     * action) immediately, exactly as the real engine does synchronously between clicks -
     * this fake has no continue-click pacing concept, matching how [SkillUi.message] already
     * fires immediately with no pacing. Stops at the first [SkillDialogueStep.Options] (see
     * [pendingDialogueOptionTexts]/[selectDialogueOption]) or drains to a Finish/FinishThen.
     */
    private fun advanceDialogue() {
        val queue = dialogueQueue ?: return
        while (true) {
            val step = queue.removeFirstOrNull() ?: run { dialogueQueue = null; return }
            when (step) {
                is SkillDialogueStep.NpcChat -> dialogueNpcChatLines += step.text.split("\n")
                is SkillDialogueStep.PlayerChat -> Unit
                is SkillDialogueStep.Statement -> Unit
                is SkillDialogueStep.Action -> step.action(this)
                is SkillDialogueStep.Options -> { pendingDialogueOptions = step; return }
                is SkillDialogueStep.Finish -> { step.action(this); dialogueQueue = null; return }
                is SkillDialogueStep.FinishThen -> { dialogueQueue = null; step.action(this); return }
            }
        }
    }

    fun pendingDialogueOptionTexts(): List<String>? = pendingDialogueOptions?.options?.map { it.text }

    /** Selects the pending dialogue option with exact text [text] and resumes the tree. */
    fun selectDialogueOption(text: String): Boolean {
        val options = pendingDialogueOptions ?: return false
        val chosen = options.options.firstOrNull { it.text == text } ?: return false
        pendingDialogueOptions = null
        dialogueQueue = ArrayDeque(chosen.steps)
        advanceDialogue()
        return true
    }

    fun setLevel(skill: Skill, level: Int) { levels[skill] = level.coerceAtLeast(1) }
    fun equip(slot: Int, itemId: Int, amount: Int = 1) { equipmentItems[slot] = itemId to amount }
    fun activeActionName(): String? = activeAction?.spec?.name
    fun advanceTicks(ticks: Int = 1) {
        repeat(ticks.coerceAtLeast(0)) {
            tickPendingExpiries()
            val action = activeAction ?: return@repeat
            if (action.stopped) return@repeat
            if (action.ticksUntilCycle > 0) {
                action.ticksUntilCycle--
                return@repeat
            }
            val failure = action.spec.requirements.asSequence()
                .map { it.validate(this) }
                .filterIsInstance<SkillValidationResult.Failed>()
                .firstOrNull()
            if (failure != null) {
                messages += failure.message
                action.cancel(ActionStopReason.REQUIREMENT_FAILED)
                return@repeat
            }
            val signal = action.spec.onCycle?.invoke(this) ?: CycleSignal.success()
            if (signal.succeeded) action.spec.onSuccess?.invoke(this)
            if (!signal.keepRunning) {
                action.cancel(signal.stopReason ?: ActionStopReason.COMPLETED)
                return@repeat
            }
            action.ticksUntilCycle = (action.spec.delayCalculator(this) - 1).coerceAtLeast(0)
        }
    }
    private fun tickPendingExpiries() {
        val iterator = pendingExpiries.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            pending.ticksRemaining--
            if (pending.ticksRemaining <= 0) {
                iterator.remove()
                pending.onExpire()
            }
        }
    }
    private fun setAmount(itemId: Int, amount: Int) { if (amount <= 0) itemAmounts.remove(itemId) else itemAmounts[itemId] = amount }
    private fun Int?.orZero(): Int = this ?: 0
}
