package net.dodian.uber.game.engine.systems.skills

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.api.content.ContentEconomy
import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.api.content.ContentAttributes
import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.api.content.ContentFeatures
import net.dodian.uber.game.api.content.ContentSocial
import net.dodian.uber.game.api.plugin.skills.SkillActions
import net.dodian.uber.game.api.plugin.skills.SkillActionHandle
import net.dodian.uber.game.api.plugin.skills.SkillInventory
import net.dodian.uber.game.api.plugin.skills.SkillInventoryTransaction
import net.dodian.uber.game.api.plugin.skills.SkillLevels
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillProduction
import net.dodian.uber.game.api.plugin.skills.PendingSkillMulti
import net.dodian.uber.game.api.plugin.skills.SkillUi
import net.dodian.uber.game.api.plugin.skills.SkillWorld
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
import net.dodian.uber.game.engine.config.FeatureStateService
import net.dodian.uber.game.engine.event.GameEventScheduler
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.entity.player.MovementLockState
import net.dodian.uber.game.engine.systems.inventory.inventoryTransaction
import net.dodian.uber.game.engine.systems.world.item.Ground
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GroundItem
import net.dodian.uber.game.model.objects.GlobalObject
import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SetSmithing
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.skill.runtime.action.SkillStateCoordinator
import net.dodian.uber.game.skill.runtime.action.ActionSpec
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.GatheringTask
import net.dodian.uber.game.skill.runtime.action.SkillingRandomEventService
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiLayout
import net.dodian.uber.skills.api.SkillMultiSelection
import net.dodian.uber.skills.runtime.SkillRecipePlanner
import net.dodian.uber.game.engine.util.Misc
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.engine.systems.skills.skillguide.SkillGuide
import net.dodian.uber.game.engine.systems.skills.farming.markFarmingDirty

/** Engine-only protocol adapter. Public content receives [SkillPlayer], never [Client]. */
internal class ClientSkillPlayerAdapter(internal val client: Client) : SkillPlayer {
    override val amountPrompt = object : SkillAmountPrompt {
        override fun request(title: String, maxDigits: Int, onEntered: (Int) -> Boolean): Boolean {
            require(maxDigits in 1..10) { "Amount prompt maxDigits must be between 1 and 10" }
            client.contentRuntimeState.putPluginAttribute(AMOUNT_PROMPT_KEY, PendingSkillAmountPrompt(onEntered))
            client.enterAmountId = ENTER_AMOUNT_SKILL_MODULE
            client.send(SendFrame27(title, maxDigits))
            return true
        }

        override fun cancel() {
            client.contentRuntimeState.removePluginAttribute(AMOUNT_PROMPT_KEY)
            if (client.enterAmountId == ENTER_AMOUNT_SKILL_MODULE) client.enterAmountId = 0
        }
    }
    override val moduleState = object : SkillModuleState {
        private fun id(moduleId: String, key: String): String = "$moduleId:$key"
        override fun get(moduleId: String, key: String): String? = client.contentRuntimeState.getPluginAttribute(id(moduleId, key))
        override fun put(moduleId: String, key: String, value: String) { client.contentRuntimeState.putPluginAttribute(id(moduleId, key), value) }
        override fun remove(moduleId: String, key: String) { client.contentRuntimeState.removePluginAttribute(id(moduleId, key)) }
    }
    override val skills = object : SkillLevels {
        override fun current(skill: Skill) = client.getLevel(skill)
        override fun base(skill: Skill) = client.getSkillLevel(skill)
        override fun experience(skill: Skill) = client.getExperience(skill)
        override fun gainXp(amount: Int, skill: Skill) = ProgressionService.addXp(client, amount, skill)
    }
    override val inventory = object : SkillInventory {
        override fun contains(itemId: Int, amount: Int) = client.getInvAmt(itemId) >= amount
        override fun amount(itemId: Int) = client.getInvAmt(itemId)
        override fun freeSlots() = client.freeSlots()
        override fun slotAmount(slot: Int, itemId: Int): Int =
            if (slot in client.playerItems.indices && client.playerItems[slot] == itemId + 1) client.playerItemsN[slot] else 0
        override fun add(itemId: Int, amount: Int) = client.addItem(itemId, amount)
        override fun remove(itemId: Int, amount: Int): Boolean {
            if (client.getInvAmt(itemId) < amount) return false
            client.deleteItem(itemId, amount)
            return true
        }
        override fun transaction(block: SkillInventoryTransaction.() -> Unit) = client.inventoryTransaction {
            val staged = this
            block(object : SkillInventoryTransaction {
                override fun require(itemId: Int, amount: Int) = staged.require(itemId, amount)
                override fun remove(itemId: Int, amount: Int) = staged.remove(itemId, amount)
                override fun removeAt(slot: Int, itemId: Int, amount: Int) = staged.removeAt(slot, itemId, amount)
                override fun add(itemId: Int, amount: Int) = staged.add(itemId, amount)
            })
        }
        override fun itemName(itemId: Int): String = client.getItemName(itemId) ?: "item"
        override fun notedItemId(itemId: Int): Int = client.getNotedItem(itemId)
        override fun refresh() = client.checkItemUpdate()
    }
    override val actions = object : SkillActions {
        override val busy: Boolean get() = client.isBusy
        override fun animate(id: Int, delay: Int) = client.performAnimation(id, delay)
        override fun resetAnimation() = client.rerequestAnim()
        override fun stop() = client.resetAction()
        override fun lockMovement(locked: Boolean) = client.setMovementLocked(locked)
        override fun beginSession(key: String) = SkillStateCoordinator.beginSession(client, key)
        override fun endSession(key: String) = SkillStateCoordinator.endSession(client, key)
        override fun activeSessionKey(): String? = client.activeSkillSessionKey
        override fun triggerRandomEvent(experience: Int) = SkillingRandomEventService.trigger(client, experience)
        override fun logGathering(itemId: Int, amount: Int, reason: String) =
            ItemLog.playerGathering(client, itemId, amount, client.position.copy(), reason)
        override fun yell(message: String) = client.yell(message)
        override fun tryThrottle(key: String, cooldownMs: Long): Boolean =
            ContentInteraction.tryAcquireMs(client, key, cooldownMs)
        override fun markMagicCastCycle() { client.lastMagicCycle = GameCycleClock.currentCycle() }
        override fun queue(spec: ActionSpec, beforeStart: () -> Unit): SkillActionHandle? {
            val player = this@ClientSkillPlayerAdapter
            val task = object : GatheringTask(
                actionName = spec.name,
                client = client,
                player = player,
                delayCalculator = spec.delayCalculator,
                requirements = spec.requirements,
                priority = spec.priority,
            ) {
                override fun onStart() { spec.onStart?.invoke(player) }
                override fun onTick(): Boolean {
                    val signal = spec.onCycle?.invoke(player) ?: CycleSignal.success()
                    if (signal.succeeded) {
                        spec.onSuccess?.invoke(player)
                        succeedCycle()
                    }
                    if (!signal.keepRunning) {
                        signal.stopReason?.let { cancel(it) }
                        return false
                    }
                    return true
                }
                override fun onStop(reason: ActionStopReason) { spec.onStop?.invoke(player, reason) }
            }
            if (!task.start(beforeStart)) return null
            return object : SkillActionHandle {
                override fun cancel(reason: ActionStopReason) = task.cancel(reason)
            }
        }
    }
    override val ui = object : SkillUi {
        override fun message(text: String) = client.sendMessage(text)
        override fun string(text: String, componentId: Int) = client.sendString(text, componentId)
        override fun open(interfaceId: Int) = client.openInterface(interfaceId)
        override fun close() = client.send(RemoveInterfaces())
        override fun chatbox(interfaceId: Int) = client.sendChatboxInterface(interfaceId)
        override fun itemModel(componentId: Int, zoom: Int, itemId: Int) = client.sendInterfaceModel(componentId, zoom, itemId)
        override fun itemGrid(componentId: Int, entries: List<net.dodian.uber.game.api.plugin.skills.SkillItemGridEntry>) =
            client.send(SetSmithing(componentId, entries.map { intArrayOf(it.itemId, it.amount) }.toTypedArray()))
        override fun componentVisible(componentId: Int, visible: Boolean) = client.changeInterfaceStatus(componentId, visible)
        override fun menuGrid(entries: List<net.dodian.uber.game.api.plugin.skills.SkillItemGridEntry>) =
            client.setMenuItems(entries.map { it.itemId }.toIntArray(), entries.map { it.amount }.toIntArray())
        override fun npcDialogue(dialogueId: Int, npcId: Int) = client.startNpcDialogue(dialogueId, npcId)
        override fun openDialogue(dialogueId: Int) {
            net.dodian.uber.game.engine.systems.dialogue.DialogueService.setDialogueId(client, dialogueId)
            net.dodian.uber.game.engine.systems.dialogue.DialogueService.setDialogueSent(client, false)
        }
        override fun itemListMenu(items: List<Int>, onSelect: (Int) -> Unit) = SkillItemListMenuService.open(client, items, onSelect)
        override fun varbit(id: Int, value: Int) = client.varbit(id, value)
        override fun skillGuide(skillId: Int, tab: Int) = SkillGuide.open(client, skillId, tab)
        override fun guideBook() = SkillGuide.openBook(client)
        override fun currentSkillGuideSkillId(): Int = client.contentRuntimeState.getSelectedSkillGuideSkillId()
        override fun dialogue(builder: net.dodian.uber.game.api.plugin.skills.SkillDialogueFactory.() -> Unit) {
            val factory = net.dodian.uber.game.api.plugin.skills.SkillDialogueFactory().apply(builder)
            net.dodian.uber.game.engine.systems.dialogue.DialogueService.start(client) { emitSkillDialogue(factory.steps, client) }
        }
    }
    override val world = object : SkillWorld {
        override val position: SkillPosition get() = client.position.toSkillPosition()
        override fun distanceTo(x: Int, y: Int) = client.distanceToPoint(x, y)
        override fun teleport(destination: SkillPosition) = client.transport(destination.toPosition())
        override fun resetPosition() = client.resetPos()
        override fun anchor(position: SkillPosition) = client.setInteractionAnchor(position.x, position.y, position.z)
        override fun face(position: SkillPosition) = client.setFocus(position.x, position.y)
        override fun graphic(id: Int, height: Int) = client.stillgfx(id, client.position, height)
        override fun graphic(id: Int, position: SkillPosition, height: Int) =
            client.stillgfx(id, position.toPosition(), height)
        override fun castGraphic(id: Int, height: Int) = client.callGfxMask(id, height)
        override fun replaceObject(target: SkillObjectRef, replacementId: Int, restoreTicks: Int, type: Int, face: Int) {
            val worldObject = WorldObject(replacementId, target.position.x, target.position.y, target.position.z, type, face, target.id)
            GlobalObject.addGlobalObject(worldObject, restoreTicks * 600)
        }
        override fun withinObjectBoundary(target: SkillObjectRef): Boolean =
            ContentInteraction.withinNearestBoundaryCardinal(
                client,
                target.id,
                target.position.toPosition(),
                GameObjectData.forId(target.id),
            )
        override fun spawnTemporaryObject(objectId: Int, durationTicks: Int, onExpire: () -> Unit) {
            val pos = client.position
            val worldObject = WorldObject(objectId, pos.x, pos.y, pos.z, 10, 0, 0)
            val durationMs = durationTicks * 600
            GlobalObject.addGlobalObject(worldObject, durationMs)
            GameEventScheduler.runLaterMs(durationMs) {
                if (!client.disconnected) onExpire()
            }
        }
        override fun dropItem(itemId: Int, amount: Int) {
            Ground.addItem(GroundItem(client.position, itemId, amount, client.slot, -1))
        }
        override fun dropItemAt(position: SkillPosition, itemId: Int, amount: Int) {
            val pos = net.dodian.uber.game.model.Position(position.x, position.y, position.z)
            Ground.addItem(GroundItem(pos, itemId, amount, client.slot, -1))
        }
        override fun traverse(
            deltaX: Int,
            deltaY: Int,
            durationMs: Int,
            movementAnimationId: Int?,
            startAnimationId: Int?,
            arrivalAnimationId: Int?,
            onComplete: () -> Unit,
        ) {
            client.setMovementLockState(MovementLockState("agility:traverse", GameCycleClock.currentCycle()))
            net.dodian.uber.game.engine.systems.skills.agility.runtime.AgilityPassageOverlayService.grantForDelta(
                client, deltaX, deltaY, durationMs.toLong(),
            )
            startAnimationId?.let { client.performAnimation(it, 0) }
            movementAnimationId?.let { client.walkAnim = it }
            client.AddToWalkCords(deltaX, deltaY, durationMs.toLong())
            GameEventScheduler.runLaterMs(durationMs) {
                if (client.disconnected) {
                    client.clearMovementLockState()
                    return@runLaterMs
                }
                client.requestWeaponAnims()
                arrivalAnimationId?.let { client.performAnimation(it, 0) }
                client.clearMovementLockState()
                onComplete()
            }
        }
        override fun climb(destination: SkillPosition, animationId: Int, delayMs: Int, onComplete: () -> Unit) {
            client.setMovementLockState(MovementLockState("agility:climb", GameCycleClock.currentCycle()))
            client.performAnimation(animationId, 0)
            GameEventScheduler.runLaterMs(delayMs) {
                if (client.disconnected) {
                    client.clearMovementLockState()
                    return@runLaterMs
                }
                client.transport(destination.toPosition())
                client.clearMovementLockState()
                onComplete()
            }
        }
    }
    override val production = object : SkillProduction {
        override fun open(config: SkillMultiConfig, onSelected: (SkillMultiSelection) -> Unit): Boolean {
            val available = SkillRecipePlanner.available(config) { inventory.amount(it) }
            if (available.isEmpty()) {
                val message = config.entries.firstNotNullOfOrNull { it.recipe.missingMaterialsMessage }
                message?.let(ui::message)
                clear()
                return false
            }
            val layout = when (config.layout) {
                SkillMultiLayout.AUTO -> when (config.entries.size) {
                    1 -> SkillMultiLayout.SINGLE
                    2 -> SkillMultiLayout.TWO
                    else -> SkillMultiLayout.THREE
                }
                else -> config.layout
            }
            require(layout == SkillMultiLayout.SPECIALIZED || config.entries.size in 1..3)
            client.resetAction()
            client.contentRuntimeState.setPendingSkillMulti(PendingSkillMulti(config, onSelected))
            if (layout == SkillMultiLayout.SPECIALIZED && !SpecializedSkillMultiRegistry.render(client, config)) {
                clear()
                client.sendMessage("This production interface is unavailable.")
                return false
            }
            if (layout != SkillMultiLayout.SPECIALIZED) renderSkillMulti(client, config, layout)
            return true
        }

        override fun select(selection: SkillMultiSelection): Boolean {
            val pending = client.contentRuntimeState.getPendingSkillMulti() ?: return false
            val resolved = SkillRecipePlanner.resolve(pending.config, selection) { inventory.amount(it) } ?: run {
                clear()
                return false
            }
            clear()
            ui.close()
            pending.onSelected(selection.copy(amount = resolved.second))
            return true
        }

        override fun pending(): SkillMultiConfig? = client.contentRuntimeState.getPendingSkillMulti()?.config
        override fun clear() { client.contentRuntimeState.clearPendingSkillMulti() }
    }
    override val equipment = object : SkillEquipment {
        override fun item(slot: Int) = client.equipment.getOrElse(slot) { -1 }
        override fun amount(slot: Int) = client.equipmentN.getOrElse(slot) { 0 }
        override fun remove(slot: Int, itemId: Int, amount: Int): Boolean {
            if (amount != 1 || client.equipment.getOrElse(slot) { -1 } != itemId) return false
            client.deleteequiment(itemId, slot)
            return true
        }
        override fun refresh() = client.refreshEquipmentState()
    }
    val economy = object : ContentEconomy {
        override fun bankAmount(itemId: Int) = client.getBankAmt(itemId)
        override fun openBank() = client.openUpBankRouted()
        override fun openShop(shopId: Int) = client.openUpShopRouted(shopId)
    }
    val social = ContentSocial { encodedName -> client.hasFriend(encodedName) }
    val features = object : ContentFeatures {
        override val bankingEnabled get() = FeatureStateService.banking.get()
        override val shoppingEnabled get() = FeatureStateService.shopping.get()
        override val tradingEnabled get() = FeatureStateService.trading.get()
        override val duelingEnabled get() = FeatureStateService.dueling.get()
    }
    override val profile = object : SkillProfile {
        override val name: String get() = client.playerName
        override val premium: Boolean get() = client.isPremium
        override val combatLevel: Int get() = client.determineCombatLevel()
    }
    override val random = object : SkillRandom {
        override fun between(minInclusive: Int, maxInclusive: Int): Int {
            require(maxInclusive >= minInclusive)
            return minInclusive + Misc.random(maxInclusive - minInclusive)
        }
        override fun chance(numerator: Int, denominator: Int): Boolean {
            require(denominator > 0 && numerator in 0..denominator)
            return numerator > 0 && Misc.random(denominator - 1) < numerator
        }
    }
    override val clock = SkillClock { System.currentTimeMillis() }
    override val vitals = object : SkillVitals {
        override val currentPrayer: Int get() = client.currentPrayer
        override val maximumPrayer: Int get() = client.maxPrayer
        override val prayerBonus: Int get() = client.playerBonus[13]
        override val inDuel: Boolean get() = client.duelFight
        override val isDead: Boolean get() = client.deathStage > 0
        override fun isPrayerActive(prayer: SkillPrayer): Boolean =
            client.prayerManager.isPrayerOn(
                net.dodian.uber.game.engine.systems.skills.prayer.PrayerManager.Prayer.valueOf(prayer.name),
            )
        override fun togglePrayer(prayer: SkillPrayer) {
            client.prayerManager.togglePrayer(
                net.dodian.uber.game.engine.systems.skills.prayer.PrayerManager.Prayer.valueOf(prayer.name),
            )
        }
        override fun deactivatePrayer(prayer: SkillPrayer) {
            val legacy = net.dodian.uber.game.engine.systems.skills.prayer.PrayerManager.Prayer.valueOf(prayer.name)
            if (client.prayerManager.isPrayerOn(legacy)) client.prayerManager.togglePrayer(legacy)
        }
        override fun setPrayer(amount: Int) { client.currentPrayer = amount.coerceIn(0, client.maxPrayer) }
        override fun damage(amount: Int) { if (amount > 0) client.dealDamage(null, amount, Entity.hitType.STANDARD) }
        override fun restorePrayer(amount: Int) { client.currentPrayer = (client.currentPrayer + amount).coerceAtMost(client.maxPrayer) }
        override fun stun(ticks: Int) { client.stunTimer = ticks.coerceAtLeast(0) }
    }
    override val runePouches = object : SkillRunePouches {
        override fun amount(slot: Int): Int = client.runePouchState.amount(slot)
        override fun addAmount(slot: Int, amount: Int) = client.runePouchState.addAmount(slot, amount)
        override fun levelRequirement(slot: Int): Int = client.runePouchState.levelRequirement(slot)
        override fun capacity(slot: Int): Int = client.runePouchState.capacity(slot)
    }
    override val farmingState = object : net.dodian.uber.game.api.plugin.skills.SkillFarmingState {
        override fun patchSlots(): List<net.dodian.uber.game.api.plugin.skills.SkillPatchSlot> =
            net.dodian.uber.game.engine.systems.skills.farming.runtime.FarmingPersistenceCodec.snapshot(client).patchSlots.map {
                net.dodian.uber.game.api.plugin.skills.SkillPatchSlot(
                    it.patch.name, it.slot, it.itemId, it.state, it.compost, it.stageOrLife, it.progress, it.plantedBy,
                )
            }

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
            val patch = net.dodian.uber.game.engine.systems.skills.farming.FarmingData.patches.valueOf(patchName)
            net.dodian.uber.game.engine.systems.skills.farming.runtime.FarmingPersistenceCodec.writePatchSlot(
                client.farmingJson,
                patch,
                slot,
                net.dodian.uber.game.engine.systems.skills.farming.runtime.PatchSlotSnapshot(
                    patch, slot, itemId, state, compost, stageOrLife, progress, plantedBy,
                ),
            )
        }

        override fun compostBins(): List<net.dodian.uber.game.api.plugin.skills.SkillCompostBinState> =
            net.dodian.uber.game.engine.systems.skills.farming.runtime.FarmingPersistenceCodec.snapshot(client).compostBins.map {
                net.dodian.uber.game.api.plugin.skills.SkillCompostBinState(it.bin.name, it.compost, it.state, it.amount, it.progress)
            }

        override fun writeCompostBin(binName: String, compost: String, state: String, amount: Int, progress: Int) {
            val bin = net.dodian.uber.game.engine.systems.skills.farming.FarmingData.compostBin.valueOf(binName)
            net.dodian.uber.game.engine.systems.skills.farming.runtime.FarmingPersistenceCodec.writeCompostBin(
                client.farmingJson,
                bin,
                net.dodian.uber.game.engine.systems.skills.farming.runtime.CompostBinSnapshot(bin, compost, state, amount, progress),
            )
        }

        override fun markDirty() = with(client) { markFarmingDirty() }
        override fun refreshVisuals() = client.farming.run { client.updateFarmPatch(); client.updateCompost() }
        override fun notifyInteraction() = net.dodian.uber.game.api.content.ContentRuntimeApi.onFarmingPatchInteraction(client)
    }
    override val slayerTask = object : net.dodian.uber.game.api.plugin.skills.SkillSlayerTask {
        override val masterNpcId: Int get() = client.slayerTaskState.masterNpcId
        override val ordinal: Int get() = client.slayerTaskState.taskOrdinal
        override val assignmentAmount: Int get() = client.slayerTaskState.assignmentAmount
        override val taskName: String get() =
            net.dodian.uber.skills.slayer.SlayerTaskRegistry.forOrdinal(client.slayerTaskState.taskOrdinal)?.name ?: ""
        override val remainingAmount: Int get() = client.slayerTaskState.remainingAmount
        override val streak: Int get() = client.slayerTaskState.streak

        // Tracks kills per monster name in client.monsterName/monsterCount rather than a
        // single counter on the task state itself (legacy behavior, preserved as-is).
        private fun currentTask() =
            net.dodian.uber.skills.slayer.SlayerTaskRegistry.forOrdinal(client.slayerTaskState.taskOrdinal)

        override fun killCount(): Int {
            val task = currentTask() ?: return -1
            var total = 0
            for (npcId in task.npcIds) {
                val npcName = net.dodian.uber.game.Server.npcManager.getName(npcId)
                for (slot in 0 until client.monsterName.size) {
                    if (client.monsterName[slot].equals(npcName, ignoreCase = true)) {
                        total += client.monsterCount[slot]
                    }
                }
            }
            return total
        }

        override fun resetKillCount(): Boolean {
            val task = currentTask() ?: return false
            for (npcId in task.npcIds) {
                val npcName = net.dodian.uber.game.Server.npcManager.getName(npcId)
                for (slot in 0 until client.monsterName.size) {
                    if (client.monsterName[slot].equals(npcName, ignoreCase = true)) {
                        client.monsterCount[slot] = 0
                    }
                }
            }
            return true
        }

        override fun assign(masterNpcId: Int, ordinal: Int, amount: Int) {
            client.slayerTaskState = client.slayerTaskState.withAssignment(masterNpcId, ordinal, amount, amount)
        }

        override fun cancel() {
            client.slayerTaskState = client.slayerTaskState.withRemainingAmount(0)
        }
    }
    override val attributes = object : ContentAttributes {
        override fun <T : Any> get(key: ContentAttributeKey<T>): T? =
            client.contentRuntimeState.getPluginAttribute(key.id)

        override fun <T : Any> put(key: ContentAttributeKey<T>, value: T) {
            client.contentRuntimeState.putPluginAttribute(key.id, value)
        }

        override fun remove(key: ContentAttributeKey<*>) {
            client.contentRuntimeState.removePluginAttribute(key.id)
        }
    }
}

private const val AMOUNT_PROMPT_KEY = "skill-module:pending-amount-prompt"
/** Reserved legacy syntax-input marker for protocol-free skill module prompts. */
internal const val ENTER_AMOUNT_SKILL_MODULE = 4
private class PendingSkillAmountPrompt(val onEntered: (Int) -> Boolean)

/** Bridges the existing syntax packet to a module-owned amount prompt. */
internal object SkillAmountPromptService {
    fun tryComplete(client: Client, amount: Int): Boolean {
        if (client.enterAmountId != ENTER_AMOUNT_SKILL_MODULE) return false
        client.enterAmountId = 0
        val prompt = client.contentRuntimeState.getPluginAttribute<PendingSkillAmountPrompt>(AMOUNT_PROMPT_KEY)
            ?: return false
        client.contentRuntimeState.removePluginAttribute(AMOUNT_PROMPT_KEY)
        prompt.onEntered(amount)
        return true
    }
}

private const val ITEM_LIST_MENU_KEY = "skill-module:pending-item-list-menu"
private class PendingItemListMenu(val items: List<Int>, var page: Int, val onSelect: (Int) -> Unit)

/**
 * Generic, plugin-owned paginated item picker (3-per-page, Close/Next/Previous) - the engine
 * side of [SkillUi.itemListMenu]. Generalizes the pagination math legacy Herblore's
 * `HerbloreDialogueService.showOptions`/`DialogueInterface.handleHerbMenu` hand-rolled around a
 * hardcoded `Client.herbloreDialogueState` field; this replaces that field entirely with the
 * same `pluginAttributes` bridge [SkillAmountPromptService] already established. [DialogueInterface]
 * calls [isActive]/[handleOption] the same way it already special-cases moderation/refund-menu/
 * account-services dialogues, ahead of the generic [net.dodian.uber.game.engine.systems.dialogue.DialogueService.onOption] fallback.
 */
internal object SkillItemListMenuService {
    fun open(client: Client, items: List<Int>, onSelect: (Int) -> Unit) {
        client.contentRuntimeState.putPluginAttribute(ITEM_LIST_MENU_KEY, PendingItemListMenu(items, 0, onSelect))
        render(client)
    }

    fun isActive(client: Client): Boolean =
        client.contentRuntimeState.getPluginAttribute<PendingItemListMenu>(ITEM_LIST_MENU_KEY) != null

    /** Silently drops any pending menu state with no packet sent - mirrors legacy
     * `HerbloreDialogueState.clear()`'s use at interface-close/walk-interrupt/logout points. */
    fun clear(client: Client) {
        client.contentRuntimeState.removePluginAttribute(ITEM_LIST_MENU_KEY)
    }

    fun handleOption(client: Client, optionIndex: Int) {
        val state = client.contentRuntimeState.getPluginAttribute<PendingItemListMenu>(ITEM_LIST_MENU_KEY) ?: return
        val size = state.items.size
        val position = size - state.page
        when {
            state.page == 0 && ((size > 3 && optionIndex == 5) || (size == 3 && optionIndex == 4) || (size == 1 && optionIndex == 2) || (size == 2 && optionIndex == 3)) -> {
                client.contentRuntimeState.removePluginAttribute(ITEM_LIST_MENU_KEY)
                client.send(RemoveInterfaces())
            }
            position > 3 && optionIndex == 4 -> {
                state.page += 3
                render(client)
            }
            state.page != 0 && ((position <= 3 && optionIndex == position + 1) || (position > 3 && optionIndex == 5)) -> {
                state.page -= 3
                render(client)
            }
            state.page + optionIndex <= size -> {
                val itemId = state.items[state.page + optionIndex - 1]
                client.contentRuntimeState.removePluginAttribute(ITEM_LIST_MENU_KEY)
                client.send(RemoveInterfaces())
                state.onSelect(itemId)
            }
        }
    }

    private fun render(client: Client) {
        val state = client.contentRuntimeState.getPluginAttribute<PendingItemListMenu>(ITEM_LIST_MENU_KEY) ?: return
        val size = state.items.size
        if (size == 0) {
            client.contentRuntimeState.removePluginAttribute(ITEM_LIST_MENU_KEY)
            return
        }
        val page = state.page.coerceAtLeast(0)
        val textSize = if (size < 4) size + 2 else if (size - page <= 3) size - page + 2 else 6
        val text = Array(textSize) { "" }
        text[0] = "What do you wish me to do?"
        val shown = minOf(3, size - page)
        repeat(shown) { index -> text[index + 1] = client.getItemName(state.items[page + index]) }
        text[shown + 1] = if (textSize < 6 && page == 0) "Close" else if (textSize == 6) "Next" else "Previous"
        if (textSize == 6) text[shown + 2] = if (page == 0) "Close" else "Previous"
        net.dodian.uber.game.ui.dialogue.DialogueUi.showPlayerOption(client, text)
    }
}

/**
 * Recursively translates a [net.dodian.uber.game.api.plugin.skills.SkillDialogueStep] tree
 * (built by a plugin via [net.dodian.uber.game.api.plugin.skills.SkillDialogueFactory], which
 * is [SkillPlayer]-coupled and can't reach the engine's own [DialogueFactory]/[Client]-coupled
 * dialogue builder directly) into real calls against [DialogueFactory] - the same builder
 * [DialogueService.start] already accepts for every game-server-authored dialogue tree.
 */
private fun net.dodian.uber.game.api.content.dialogue.DialogueFactory.emitSkillDialogue(
    steps: List<net.dodian.uber.game.api.plugin.skills.SkillDialogueStep>,
    client: Client,
) {
    val skillPlayer = client.asSkillPlayer()
    steps.forEach { step ->
        when (step) {
            is net.dodian.uber.game.api.plugin.skills.SkillDialogueStep.NpcChat ->
                npcChat(step.npcId, step.emote, step.text)
            is net.dodian.uber.game.api.plugin.skills.SkillDialogueStep.PlayerChat ->
                playerChat(step.emote, step.text)
            is net.dodian.uber.game.api.plugin.skills.SkillDialogueStep.Statement ->
                statement(*step.lines.toTypedArray())
            is net.dodian.uber.game.api.plugin.skills.SkillDialogueStep.Options ->
                options(
                    title = step.title,
                    *step.options.map { opt ->
                        net.dodian.uber.game.api.content.dialogue.DialogueOption(opt.text) { emitSkillDialogue(opt.steps, client) }
                    }.toTypedArray(),
                )
            is net.dodian.uber.game.api.plugin.skills.SkillDialogueStep.Action ->
                action { step.action(skillPlayer) }
            is net.dodian.uber.game.api.plugin.skills.SkillDialogueStep.Finish ->
                finish(step.closeInterfaces) { step.action(skillPlayer) }
            is net.dodian.uber.game.api.plugin.skills.SkillDialogueStep.FinishThen ->
                finishThen(step.closeInterfaces) { step.action(skillPlayer) }
        }
    }
}

internal fun Client.asSkillPlayer(): SkillPlayer = ClientSkillPlayerAdapter(this)

/** Java-callable accessor for legacy (non-Kotlin) call sites migrating onto the SkillPlayer API. */
object SkillPlayerBridge {
    @JvmStatic
    fun of(client: Client): SkillPlayer = client.asSkillPlayer()
}

internal fun Position.toSkillPosition() = SkillPosition(x, y, z)
internal fun SkillPosition.toPosition() = Position(x, y, z)

private fun renderSkillMulti(client: Client, config: SkillMultiConfig, layout: SkillMultiLayout) {
    val entries = config.entries
    when (layout) {
        SkillMultiLayout.SINGLE -> {
            val entry = entries.single()
            client.sendInterfaceModel(1746, 190, entry.recipe.outputItemId)
            client.sendString("\\n\\n\\n\\n\\n${entry.label ?: client.getItemName(entry.recipe.outputItemId)}", 2799)
            client.sendChatboxInterface(4429)
        }
        SkillMultiLayout.TWO -> {
            client.sendString(config.title, 8879)
            val first = entries[0]
            val second = entries[1]
            client.sendInterfaceModel(8869, 250, first.recipe.outputItemId)
            client.sendInterfaceModel(8870, 250, second.recipe.outputItemId)
            client.sendString(first.label ?: client.getItemName(first.recipe.outputItemId), 8871)
            client.sendString(first.label ?: client.getItemName(first.recipe.outputItemId), 8874)
            client.sendString(second.label ?: client.getItemName(second.recipe.outputItemId), 8878)
            client.sendString(second.label ?: client.getItemName(second.recipe.outputItemId), 8875)
            client.sendChatboxInterface(8866)
        }
        SkillMultiLayout.THREE -> {
            client.sendString(config.title, 8898)
            val labelComponents = intArrayOf(8889, 8893, 8897)
            val modelComponents = intArrayOf(8883, 8884, 8885)
            entries.forEachIndexed { index, entry ->
                client.sendString(entry.label ?: client.getItemName(entry.recipe.outputItemId), labelComponents[index])
                client.sendInterfaceModel(modelComponents[index], 250, entry.recipe.outputItemId)
            }
            client.sendChatboxInterface(8880)
        }
        SkillMultiLayout.SPECIALIZED -> Unit
        SkillMultiLayout.AUTO -> error("AUTO layout must be resolved before rendering")
    }
}

/**
 * Temporary engine-side bridge for legacy skill implementations that have not
 * yet moved into their Gradle module. Public interaction contexts do not expose
 * this operation and new module code is forbidden from using it.
 */
internal object SkillEngineAccess {
    fun client(player: SkillPlayer): Client =
        (player as? ClientSkillPlayerAdapter)?.client
            ?: error("This legacy skill route requires a live engine-backed player")
}
