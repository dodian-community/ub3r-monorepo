package net.dodian.uber.game.engine.systems.interaction

import net.dodian.uber.game.Server
import net.dodian.cache.objects.GameObjectData
import net.dodian.cache.objects.GameObjectDef
import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.engine.systems.interaction.objects.ObjectContentRegistry
import net.dodian.uber.game.engine.systems.interaction.objects.ObjectInteractionService
import net.dodian.uber.game.engine.systems.interaction.npcs.BankerApproachFallbackService
import net.dodian.uber.game.engine.systems.interaction.npcs.NpcContentDispatcher
import net.dodian.uber.game.engine.systems.interaction.items.ItemOnNpcContentService
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.item.ItemOnNpcEvent
import net.dodian.uber.game.events.magic.MagicOnNpcEvent
import net.dodian.uber.game.events.magic.MagicOnPlayerEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.objects.GlobalObject
import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.engine.systems.combat.CombatIntent
import net.dodian.uber.game.engine.systems.combat.CombatStartService
import net.dodian.uber.game.engine.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.engine.systems.interaction.scheduler.InteractionExecutionResult
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.engine.systems.skills.SkillPolicyMetrics
import net.dodian.uber.game.engine.systems.skills.SkillPolicyResult
import net.dodian.uber.game.engine.systems.skills.SkillPolicyRoute
import net.dodian.uber.game.engine.util.Misc
import net.dodian.uber.game.engine.config.runtimePhaseWarnMs
import org.slf4j.LoggerFactory
import java.util.IdentityHashMap

object InteractionProcessor {
    private val logger = LoggerFactory.getLogger(InteractionProcessor::class.java)
    private val settledSinceCycle = IdentityHashMap<InteractionIntent, Long>()

    @JvmStatic
    fun process(player: Client): InteractionExecutionResult {
        val intent = player.pendingInteraction ?: return InteractionExecutionResult.CANCELLED
        if (player.didTeleport() || player.disconnected || !player.isActive || !player.validClient) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (PlayerRegistry.cycle < player.interactionEarliestCycle) {
            return InteractionExecutionResult.WAITING
        }
        return when (intent) {
            is NpcInteractionIntent -> processNpcInteraction(player, intent)
            is ObjectClickIntent -> processObjectClick(player, intent)
            is ItemOnObjectIntent -> processItemOnObject(player, intent)
            is MagicOnObjectIntent -> processMagicOnObject(player, intent)
            is ItemOnNpcIntent -> processItemOnNpc(player, intent)
            is MagicOnNpcIntent -> processMagicOnNpc(player, intent)
            is MagicOnPlayerIntent -> processMagicOnPlayer(player, intent)
            is AttackPlayerIntent -> processAttackPlayer(player, intent)
            else -> {
                clear(player)
                InteractionExecutionResult.CANCELLED
            }
        }
    }

    private fun processNpcInteraction(player: Client, intent: NpcInteractionIntent): InteractionExecutionResult {
        val startNs = System.nanoTime()
        val npc = Server.npcManager.getNpc(intent.npcIndex)
        if (npc == null) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        if (player.randomed || player.UsingAgility) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (intent.option != NPC_ATTACK_OPTION && !npc.isAlive) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val range =
            if (intent.option == NPC_ATTACK_OPTION && player.getAttackStyle() != 0) {
                5
            } else {
                1
        }

        val legendsGuardFrontLane = isLegendsGuardFrontLaneInteraction(player, npc, intent.option)
        val routeStart = System.nanoTime()
        if (!legendsGuardFrontLane && !player.goodDistanceEntity(npc, range)) {
            if (BankerApproachFallbackService.shouldAttemptFallback(player, npc, intent.option)) {
                BankerApproachFallbackService.tryRouteCustomerSide(player, npc)
            }
            return InteractionExecutionResult.WAITING
        }
        if (npc.position.withinDistance(player.position, 0)) {
            return InteractionExecutionResult.WAITING
        }
        if (intent.option != NPC_ATTACK_OPTION && !legendsGuardFrontLane) {
            settleNpcInteractionMovement(player)
        }
        val routeNs = System.nanoTime() - routeStart

        player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
        val timing =
            when (intent.option) {
                1 -> handleNpcClick1(player, npc)
                2 -> handleNpcClick2(player, npc)
                3 -> handleNpcClick3(player, npc)
                4 -> handleNpcClick4(player, npc)
                NPC_ATTACK_OPTION -> handleNpcAttack(player, npc)
                else -> DispatchTiming(false, 0L, 0L, null)
            }
        clear(player)
        slowLogIfNeeded(
            player,
            intent,
            npc.id,
            intent.option,
            routeNs,
            timing.resolveNs,
            timing.handlerNs,
            timing.handlerName,
            startNs,
        )
        return InteractionExecutionResult.COMPLETE
    }

    private fun processObjectClick(player: Client, intent: ObjectClickIntent): InteractionExecutionResult {
        val startNs = System.nanoTime()
        if (player.disconnected || player.randomed || player.UsingAgility) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.pendingInteraction !== intent) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val targetPosition = resolveTargetPosition(intent.objectPosition, player)
        val routeSnapshot =
            resolveObjectSnapshot(
                objectId = intent.objectId,
                position = targetPosition,
                fallbackData = intent.objectData,
                fallbackDef = intent.objectDef,
            )
        val skillObjectBinding = PluginRegistry.currentSkills().objectBinding(intent.option, intent.objectId)
        val policy =
            SkillInteractionDispatcher.resolveObjectPolicy(
                option = intent.option,
                objectId = intent.objectId,
                position = targetPosition,
                obj = routeSnapshot.objectData,
            ) ?: ObjectContentRegistry.resolvePolicy(
                objectId = intent.objectId,
                position = targetPosition,
                interactionType = ObjectInteractionPolicy.InteractionType.CLICK,
                option = intent.option,
                obj = routeSnapshot.objectData,
            ) ?: ObjectInteractionPolicy.DEFAULT

        val routeStart = System.nanoTime()
        if (
            ObjectInteractionDistance.resolveDistancePosition(
                player,
                targetPosition,
                intent.objectId,
                routeSnapshot.objectData,
                routeSnapshot.objectDef,
                resolveDistanceMode(policy.distanceRule),
            ) == null
        ) {
            skillObjectBinding?.let {
                SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.OBJECT, SkillPolicyResult.POLICY_REJECT)
            }
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        if (!isSettleGateSatisfied(player, intent, policy)) {
            skillObjectBinding?.let {
                SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.OBJECT, SkillPolicyResult.SETTLE_WAIT)
            }
            return InteractionExecutionResult.WAITING
        }

        if (intent.option == 1) {
            if (!player.validClient || player.randomed) {
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
            if (player.adding) {
                // Preserve legacy debug behavior.
                player.objects.add(
                    WorldObject(
                        intent.objectId,
                        targetPosition.x,
                        targetPosition.y,
                        targetPosition.z,
                        1,
                    ),
                )
            }
            if ((player.isWalkBlocked() && !player.UsingAgility) || player.genie || player.antique) {
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
            val playerPos = player.position.copy()
            val xDiff = kotlin.math.abs(playerPos.x - targetPosition.x)
            val yDiff = kotlin.math.abs(playerPos.y - targetPosition.y)
            PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.OBJECT_INTERACTION, false, false, false, true)
            player.setFocus(targetPosition.x, targetPosition.y)
            if (xDiff > 5 || yDiff > 5) {
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
        } else if (intent.option == 2) {
            if (player.adding) {
                player.objects.add(
                    WorldObject(
                        intent.objectId,
                        targetPosition.x,
                        targetPosition.y,
                        targetPosition.z,
                        2,
                    ),
                )
            }
            if (player.isWalkBlocked()) {
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
            player.setFocus(targetPosition.x, targetPosition.y)
        } else if (intent.option == 3) {
            player.setFocus(targetPosition.x, targetPosition.y)
        }

        val dispatchSnapshot =
            resolveObjectSnapshot(
                objectId = intent.objectId,
                position = targetPosition,
                fallbackData = routeSnapshot.objectData,
                fallbackDef = routeSnapshot.objectDef,
            )
        if (intent.objectDef != null &&
            !isObjectStillPresent(
                objectId = intent.objectId,
                position = targetPosition,
                objectDef = dispatchSnapshot.objectDef,
            )
        ) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val timing =
            ObjectInteractionService.tryHandleTimed(
                ObjectInteractionContext.click(
                    client = player,
                    option = intent.option,
                    objectId = intent.objectId,
                    position = targetPosition,
                    obj = dispatchSnapshot.objectData,
                    packetOpcode = intent.opcode,
                ),
            )
        if (skillObjectBinding == null &&
            timing.handled &&
            timing.handlerName != SkillInteractionDispatcher::class.java.name
        ) {
            val preset = PluginRegistry.currentSkills().firstPresetForObjectId(intent.objectId)
            if (preset != null) {
                SkillPolicyMetrics.record(preset, SkillPolicyRoute.OBJECT, SkillPolicyResult.ROUTE_BYPASS_REJECT)
            }
        }
        clear(player)
        slowLogIfNeeded(
            player,
            intent,
            intent.objectId,
            intent.option,
            routeNs,
            timing.resolveNs,
            timing.handlerNs,
            timing.handlerName,
            startNs,
        )
        return InteractionExecutionResult.COMPLETE
    }

    private fun processItemOnObject(player: Client, intent: ItemOnObjectIntent): InteractionExecutionResult {
        val startNs = System.nanoTime()
        if (player.disconnected || player.randomed) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.pendingInteraction !== intent) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val targetPosition = resolveTargetPosition(intent.objectPosition, player)
        val routeSnapshot =
            resolveObjectSnapshot(
                objectId = intent.objectId,
                position = targetPosition,
                fallbackData = intent.objectData,
                fallbackDef = intent.objectDef,
            )
        val skillItemOnObjectBinding = PluginRegistry.currentSkills().itemOnObjectBinding(intent.objectId, intent.itemId)
        val policy =
            SkillInteractionDispatcher.resolveItemOnObjectPolicy(
                objectId = intent.objectId,
                itemId = intent.itemId,
            ) ?: ObjectContentRegistry.resolvePolicy(
                objectId = intent.objectId,
                position = targetPosition,
                interactionType = ObjectInteractionPolicy.InteractionType.ITEM_ON_OBJECT,
                obj = routeSnapshot.objectData,
                itemId = intent.itemId,
                itemSlot = intent.itemSlot,
                interfaceId = intent.interfaceId,
            ) ?: ObjectInteractionPolicy.DEFAULT

        val routeStart = System.nanoTime()
        if (
            ObjectInteractionDistance.resolveDistancePosition(
                player,
                targetPosition,
                intent.objectId,
                routeSnapshot.objectData,
                routeSnapshot.objectDef,
                resolveDistanceMode(policy.distanceRule),
            ) == null
        ) {
            skillItemOnObjectBinding?.let {
                SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.ITEM_ON_OBJECT, SkillPolicyResult.POLICY_REJECT)
            }
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        if (!isSettleGateSatisfied(player, intent, policy)) {
            skillItemOnObjectBinding?.let {
                SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.ITEM_ON_OBJECT, SkillPolicyResult.SETTLE_WAIT)
            }
            return InteractionExecutionResult.WAITING
        }

        val dispatchSnapshot =
            resolveObjectSnapshot(
                objectId = intent.objectId,
                position = targetPosition,
                fallbackData = routeSnapshot.objectData,
                fallbackDef = routeSnapshot.objectDef,
            )
        if (intent.objectDef != null &&
            !isObjectStillPresent(
                objectId = intent.objectId,
                position = targetPosition,
                objectDef = dispatchSnapshot.objectDef,
            )
        ) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        var timing = DispatchTiming(false, 0L, 0L, null)
        if (player.playerHasItem(intent.itemId)) {
            player.setFocus(targetPosition.x, targetPosition.y)
            timing =
                ObjectInteractionService.tryHandleTimed(
                    ObjectInteractionContext.useItem(
                        client = player,
                        objectId = intent.objectId,
                        position = targetPosition,
                        obj = dispatchSnapshot.objectData,
                        itemId = intent.itemId,
                        itemSlot = intent.itemSlot,
                        interfaceId = intent.interfaceId,
                        packetOpcode = intent.opcode,
                    ),
                )
        }
        if (skillItemOnObjectBinding != null &&
            timing.handled &&
            timing.handlerName != SkillInteractionDispatcher::class.java.name
        ) {
            SkillPolicyMetrics.record(
                skillItemOnObjectBinding.preset,
                SkillPolicyRoute.ITEM_ON_OBJECT,
                SkillPolicyResult.ROUTE_BYPASS_REJECT,
            )
        }
        clear(player)
        slowLogIfNeeded(
            player,
            intent,
            intent.objectId,
            -1,
            routeNs,
            timing.resolveNs,
            timing.handlerNs,
            timing.handlerName,
            startNs,
        )
        return InteractionExecutionResult.COMPLETE
    }

    private fun processMagicOnObject(player: Client, intent: MagicOnObjectIntent): InteractionExecutionResult {
        val startNs = System.nanoTime()
        if (player.disconnected || player.randomed || player.UsingAgility) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.pendingInteraction !== intent) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val targetPosition = resolveTargetPosition(intent.objectPosition, player)
        val routeSnapshot =
            resolveObjectSnapshot(
                objectId = intent.objectId,
                position = targetPosition,
                fallbackData = intent.objectData,
                fallbackDef = intent.objectDef,
            )
        val skillMagicOnObjectBinding = PluginRegistry.currentSkills().magicOnObjectBinding(intent.objectId, intent.spellId)
        val policy =
            SkillInteractionDispatcher.resolveMagicOnObjectPolicy(
                objectId = intent.objectId,
                spellId = intent.spellId,
            ) ?: ObjectContentRegistry.resolvePolicy(
                objectId = intent.objectId,
                position = targetPosition,
                interactionType = ObjectInteractionPolicy.InteractionType.MAGIC,
                obj = routeSnapshot.objectData,
                spellId = intent.spellId,
            ) ?: ObjectInteractionPolicy.DEFAULT

        val routeStart = System.nanoTime()
        if (
            ObjectInteractionDistance.resolveDistancePosition(
                player,
                targetPosition,
                intent.objectId,
                routeSnapshot.objectData,
                routeSnapshot.objectDef,
                resolveDistanceMode(policy.distanceRule),
            ) == null
        ) {
            skillMagicOnObjectBinding?.let {
                SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.MAGIC_ON_OBJECT, SkillPolicyResult.POLICY_REJECT)
            }
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        if (!isSettleGateSatisfied(player, intent, policy)) {
            skillMagicOnObjectBinding?.let {
                SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.MAGIC_ON_OBJECT, SkillPolicyResult.SETTLE_WAIT)
            }
            return InteractionExecutionResult.WAITING
        }

        val dispatchSnapshot =
            resolveObjectSnapshot(
                objectId = intent.objectId,
                position = targetPosition,
                fallbackData = routeSnapshot.objectData,
                fallbackDef = routeSnapshot.objectDef,
            )
        if (intent.objectDef != null &&
            !isObjectStillPresent(
                objectId = intent.objectId,
                position = targetPosition,
                objectDef = dispatchSnapshot.objectDef,
            )
        ) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        player.setFocus(targetPosition.x, targetPosition.y)
        val timing =
            ObjectInteractionService.tryHandleTimed(
                ObjectInteractionContext.magic(
                    client = player,
                    objectId = intent.objectId,
                    position = targetPosition,
                    obj = dispatchSnapshot.objectData,
                    spellId = intent.spellId,
                    packetOpcode = intent.opcode,
                ),
            )
        if (skillMagicOnObjectBinding != null &&
            timing.handled &&
            timing.handlerName != SkillInteractionDispatcher::class.java.name
        ) {
            SkillPolicyMetrics.record(
                skillMagicOnObjectBinding.preset,
                SkillPolicyRoute.MAGIC_ON_OBJECT,
                SkillPolicyResult.ROUTE_BYPASS_REJECT,
            )
        }
        clear(player)
        slowLogIfNeeded(
            player,
            intent,
            intent.objectId,
            -1,
            routeNs,
            timing.resolveNs,
            timing.handlerNs,
            timing.handlerName,
            startNs,
        )
        return InteractionExecutionResult.COMPLETE
    }

    private fun processItemOnNpc(player: Client, intent: ItemOnNpcIntent): InteractionExecutionResult {
        val startNs = System.nanoTime()
        val npc = Server.npcManager.getNpc(intent.npcIndex)
        if (npc == null) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.disconnected || player.randomed || player.UsingAgility) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.pendingInteraction !== intent) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (intent.itemSlot < 0 || intent.itemSlot >= player.playerItems.size) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.playerItems[intent.itemSlot] - 1 != intent.itemId) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val routeStart = System.nanoTime()
        if (!player.goodDistanceEntity(npc, 1)) {
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
        player.faceNpc(intent.npcIndex)
        val handled = GameEventBus.postWithResult(ItemOnNpcEvent(player, intent.itemId, intent.itemSlot, intent.npcIndex, npc))
        if (!handled) {
            ItemOnNpcContentService.handle(player, intent.itemId, intent.itemSlot, intent.npcIndex, npc)
        }
        clear(player)
        slowLogIfNeeded(
            player,
            intent,
            npc.id,
            -1,
            routeNs,
            0L,
            0L,
            if (handled) "GameEventBus" else ItemOnNpcContentService::class.java.name,
            startNs,
        )
        return InteractionExecutionResult.COMPLETE
    }

    private fun processMagicOnNpc(player: Client, intent: MagicOnNpcIntent): InteractionExecutionResult {
        val startNs = System.nanoTime()
        val npc = Server.npcManager.getNpc(intent.npcIndex)
        if (npc == null) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.disconnected || player.randomed || player.UsingAgility || player.deathStage >= 1) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.pendingInteraction !== intent) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val routeStart = System.nanoTime()
        if (!player.goodDistanceEntity(npc, 5)) {
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
        player.magicId = intent.spellId
        val handled = GameEventBus.postWithResult(MagicOnNpcEvent(player, intent.spellId, intent.npcIndex, npc))
        if (!handled) {
            CombatStartService.startNpcAttack(player, npc, CombatIntent.MAGIC_ON_NPC)
        }
        clear(player)
        slowLogIfNeeded(
            player,
            intent,
            npc.id,
            -1,
            routeNs,
            0L,
            0L,
            if (handled) "GameEventBus" else CombatStartService::class.java.name,
            startNs,
        )
        return InteractionExecutionResult.COMPLETE
    }

    private fun processMagicOnPlayer(player: Client, intent: MagicOnPlayerIntent): InteractionExecutionResult {
        val startNs = System.nanoTime()
        val victim = PlayerRegistry.getClient(intent.victimIndex)
        if (victim == null) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.disconnected || player.randomed || player.UsingAgility || player.deathStage >= 1) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.pendingInteraction !== intent) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val routeStart = System.nanoTime()
        if (!player.goodDistanceEntity(victim, 5)) {
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
        player.magicId = intent.spellId
        val handled = GameEventBus.postWithResult(MagicOnPlayerEvent(player, intent.spellId, intent.victimIndex, victim))
        if (!handled) {
            CombatStartService.startPlayerAttack(player, victim, CombatIntent.MAGIC_ON_PLAYER)
        }
        clear(player)
        slowLogIfNeeded(
            player,
            intent,
            victim.slot,
            -1,
            routeNs,
            0L,
            0L,
            if (handled) "GameEventBus" else CombatStartService::class.java.name,
            startNs,
        )
        return InteractionExecutionResult.COMPLETE
    }

    private fun processAttackPlayer(player: Client, intent: AttackPlayerIntent): InteractionExecutionResult {
        val startNs = System.nanoTime()
        val victim = PlayerRegistry.getClient(intent.victimIndex)
        if (victim == null) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.disconnected || player.randomed || player.UsingAgility || player.deathStage >= 1) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (player.pendingInteraction !== intent) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
        CombatStartService.startPlayerAttack(player, victim, CombatIntent.ATTACK_PLAYER)
        clear(player)
        slowLogIfNeeded(
            player,
            intent,
            victim.slot,
            -1,
            0L,
            0L,
            0L,
            CombatStartService::class.java.name,
            startNs,
        )
        return InteractionExecutionResult.COMPLETE
    }

    private fun handleNpcClick1(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.NPC_INTERACTION, false, false, false, true)
        player.faceNpc(npc.slot)
        player.setInteractionAnchor(npc.position.x, npc.position.y, npc.position.z)
        return NpcContentDispatcher.tryHandleClickTimed(player, 1, npc)
    }

    private fun handleNpcClick2(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.NPC_INTERACTION, false, false, false, true)
        player.faceNpc(npc.slot)
        player.setInteractionAnchor(npc.position.x, npc.position.y, npc.position.z)
        return NpcContentDispatcher.tryHandleClickTimed(player, 2, npc)
    }

    private fun handleNpcClick3(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (player.isBusy) {
            return DispatchTiming(false, 0L, 0L, null)
        }
        PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.NPC_INTERACTION, false, false, false, true)
        player.faceNpc(npc.slot)
        player.setInteractionAnchor(npc.position.x, npc.position.y, npc.position.z)
        return NpcContentDispatcher.tryHandleClickTimed(player, 3, npc)
    }

    private fun handleNpcClick4(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (player.isBusy) {
            return DispatchTiming(false, 0L, 0L, null)
        }
        player.setInteractionAnchor(npc.position.x, npc.position.y, npc.position.z)
        return NpcContentDispatcher.tryHandleClickTimed(player, 4, npc)
    }

    private fun handleNpcAttack(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (player.magicId >= 0) {
            player.magicId = -1
        }
        if (player.deathStage >= 1) {
            return DispatchTiming(false, 0L, 0L, null)
        }
        val timing = NpcContentDispatcher.tryHandleAttackTimed(player, npc)
        if (timing.handled) {
            return timing
        }
        CombatStartService.beginAttackNow(player, npc, CombatIntent.ATTACK_NPC)
        return timing
    }

    private fun settleNpcInteractionMovement(player: Client) {
        if (player.newWalkCmdSteps > 0 || player.wQueueReadPtr != player.wQueueWritePtr) {
            player.resetWalkingQueue()
        }
    }

    private fun clear(player: Client) {
        player.pendingInteraction?.let { settledSinceCycle.remove(it) }
        player.pendingInteraction = null
        player.activeInteraction = null
        player.interactionEarliestCycle = 0
        player.interactionTaskHandle = null
    }

    private fun resolveTargetPosition(objectPosition: Position, player: Client): Position {
        return Position(objectPosition.x, objectPosition.y, player.position.z)
    }

    private fun resolveObjectSnapshot(
        objectId: Int,
        position: Position,
        fallbackData: GameObjectData?,
        fallbackDef: GameObjectDef?,
    ): ObjectSnapshot {
        val liveData = GameObjectData.forId(objectId) ?: fallbackData
        val liveDef = Misc.getObject(objectId, position.x, position.y, position.z) ?: fallbackDef
        return ObjectSnapshot(liveData, liveDef)
    }

    private fun isObjectStillPresent(
        objectId: Int,
        position: Position,
        objectDef: GameObjectDef?,
    ): Boolean {
        if (objectDef != null) {
            return true
        }
        return GlobalObject.hasGlobalObject(WorldObject(objectId, position.x, position.y, position.z, 10))
    }

    private fun resolveDistanceMode(distanceRule: ObjectInteractionPolicy.DistanceRule): ObjectInteractionDistance.DistanceMode {
        return when (distanceRule) {
            ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_CARDINAL ->
                ObjectInteractionDistance.DistanceMode.POLICY_NEAREST_BOUNDARY_CARDINAL
            ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_ANY ->
                ObjectInteractionDistance.DistanceMode.POLICY_NEAREST_BOUNDARY_ANY
            ObjectInteractionPolicy.DistanceRule.REACHABLE ->
                ObjectInteractionDistance.DistanceMode.REACHABLE
        }
    }

    private fun isSettleGateSatisfied(
        player: Client,
        intent: InteractionIntent,
        policy: ObjectInteractionPolicy,
    ): Boolean {
        if (!policy.requireMovementSettled) {
            settledSinceCycle.remove(intent)
            return true
        }
        if (!isMovementSettled(player)) {
            settledSinceCycle.remove(intent)
            return false
        }
        if (policy.settleTicks <= 0) {
            settledSinceCycle.remove(intent)
            return true
        }
        val settledSince = settledSinceCycle[intent]
        if (settledSince == null) {
            settledSinceCycle[intent] = PlayerRegistry.cycle.toLong()
            return false
        }
        if (PlayerRegistry.cycle.toLong() - settledSince < policy.settleTicks) {
            return false
        }
        settledSinceCycle.remove(intent)
        return true
    }

    private fun isMovementSettled(player: Client): Boolean {
        return player.primaryDirection == -1 &&
            player.secondaryDirection == -1 &&
            player.wQueueReadPtr == player.wQueueWritePtr
    }

    internal fun isLegendsGuardFrontLaneInteraction(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc, option: Int): Boolean {
        if (option !in 1..4) {
            return false
        }
        if (npc.id != LEGENDS_GUARD_NPC_ID || npc.position.z != LEGENDS_GATE_Z || player.position.z != LEGENDS_GATE_Z) {
            return false
        }
        val npcIsLegendsGuard =
            (npc.position.x == LEGENDS_GUARD_WEST_X || npc.position.x == LEGENDS_GUARD_EAST_X) &&
                npc.position.y == LEGENDS_GATE_Y
        if (!npcIsLegendsGuard) {
            return false
        }
        val playerFrontLane = player.position.x in LEGENDS_FRONT_LANE_X && player.position.y in LEGENDS_FRONT_LANE_Y
        return playerFrontLane
    }

    private fun slowLogIfNeeded(
        player: Client,
        intent: InteractionIntent,
        targetId: Int,
        option: Int,
        routeNs: Long,
        resolveNs: Long,
        handlerNs: Long,
        handlerName: String?,
        startNs: Long,
    ) {
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
        if (elapsedMs >= runtimePhaseWarnMs) {
            val routeMs = routeNs / 1_000_000L
            val resolveMs = resolveNs / 1_000_000L
            val handlerMs = handlerNs / 1_000_000L
            val accounted = routeMs + resolveMs + handlerMs
            val overheadMs = (elapsedMs - accounted).coerceAtLeast(0L)
            logger.warn(
                "Slow interaction: type={} player={} target={} option={} total={}ms route={}ms resolve={}ms handler={}ms overhead={}ms handlerName={}",
                intent::class.java.name,
                player.playerName,
                targetId,
                option,
                elapsedMs,
                routeMs,
                resolveMs,
                handlerMs,
                overheadMs,
                handlerName ?: "n/a",
            )
        }
    }

    private const val NPC_ATTACK_OPTION = 5
    private const val LEGENDS_GUARD_NPC_ID = 3951
    private const val LEGENDS_GUARD_WEST_X = 2727
    private const val LEGENDS_GUARD_EAST_X = 2730
    private const val LEGENDS_GATE_Y = 3349
    private const val LEGENDS_GATE_Z = 0
    private val LEGENDS_FRONT_LANE_X = 2728..2729
    private val LEGENDS_FRONT_LANE_Y = 3348..3350

    private data class ObjectSnapshot(
        val objectData: GameObjectData?,
        val objectDef: GameObjectDef?,
    )
}
