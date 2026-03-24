package net.dodian.uber.game.runtime.interaction

import net.dodian.uber.game.Server
import net.dodian.cache.`object`.GameObjectData
import net.dodian.cache.`object`.GameObjectDef
import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.content.objects.ObjectContentRegistry
import net.dodian.uber.game.content.objects.services.ObjectInteractionContext
import net.dodian.uber.game.content.objects.ObjectInteractionService
import net.dodian.uber.game.content.npc.NpcInteractionService
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.`object`.GlobalObject
import net.dodian.uber.game.model.`object`.Object as WorldObject
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.model.`object`.RS2Object
import net.dodian.uber.game.runtime.combat.CombatIntent
import net.dodian.uber.game.runtime.combat.CombatStartService
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
import net.dodian.uber.game.runtime.interaction.scheduler.InteractionExecutionResult
import net.dodian.uber.game.skills.fishing.FishingNpcInteractionService
import net.dodian.utilities.Misc
import net.dodian.utilities.runtimePhaseWarnMs
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
        if (PlayerHandler.cycle < player.interactionEarliestCycle) {
            return InteractionExecutionResult.WAITING
        }
        return when (intent) {
            is NpcInteractionIntent -> processNpcInteraction(player, intent)
            is ObjectClickIntent -> processObjectClick(player, intent)
            is ItemOnObjectIntent -> processItemOnObject(player, intent)
            is MagicOnObjectIntent -> processMagicOnObject(player, intent)
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

        val range =
            if (intent.option == NPC_ATTACK_OPTION && player.getAttackStyle() != 0) {
                5
            } else {
                1
        }

        val routeStart = System.nanoTime()
        if (!player.goodDistanceEntity(npc, range) || npc.position.withinDistance(player.position, 0)) {
            return InteractionExecutionResult.WAITING
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
        val policy =
            ObjectContentRegistry.resolvePolicy(
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
                resolveDistanceMode(policy.distanceRule, ObjectInteractionPolicy.InteractionType.CLICK),
            ) == null
        ) {
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        if (!isSettleGateSatisfied(player, intent, policy)) {
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
                    RS2Object(
                        intent.objectId,
                        targetPosition.x,
                        targetPosition.y,
                        1,
                    ),
                )
            }
            if (System.currentTimeMillis() < player.walkBlock || player.genie || player.antique) {
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
                    RS2Object(
                        intent.objectId,
                        targetPosition.x,
                        targetPosition.y,
                        2,
                    ),
                )
            }
            if (System.currentTimeMillis() < player.walkBlock) {
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
                ),
            )
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
        val policy =
            ObjectContentRegistry.resolvePolicy(
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
                resolveDistanceMode(policy.distanceRule, ObjectInteractionPolicy.InteractionType.ITEM_ON_OBJECT),
            ) == null
        ) {
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        if (!isSettleGateSatisfied(player, intent, policy)) {
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
                    ),
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
        val policy =
            ObjectContentRegistry.resolvePolicy(
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
                resolveDistanceMode(policy.distanceRule, ObjectInteractionPolicy.InteractionType.MAGIC),
            ) == null
        ) {
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        if (!isSettleGateSatisfied(player, intent, policy)) {
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
                ),
            )
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

    private fun handleNpcClick1(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (!npc.isAlive) {
            player.send(SendMessage("That monster has been killed!"))
            return DispatchTiming(false, 0L, 0L, null)
        }
        PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.NPC_INTERACTION, false, false, false, true)
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        if (FishingNpcInteractionService.handleNpcOption(player, npc.id, 1)) {
            return DispatchTiming(true, 0L, 0L, FishingNpcInteractionService::class.java.name)
        }
        return NpcInteractionService.tryHandleClickTimed(player, 1, npc)
    }

    private fun handleNpcClick2(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (!npc.isAlive) {
            player.send(SendMessage("That monster has been killed!"))
            return DispatchTiming(false, 0L, 0L, null)
        }
        PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.NPC_INTERACTION, false, false, false, true)
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        if (FishingNpcInteractionService.handleNpcOption(player, npc.id, 2)) {
            return DispatchTiming(true, 0L, 0L, FishingNpcInteractionService::class.java.name)
        }
        return NpcInteractionService.tryHandleClickTimed(player, 2, npc)
    }

    private fun handleNpcClick3(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (player.isBusy) {
            return DispatchTiming(false, 0L, 0L, null)
        }
        PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.NPC_INTERACTION, false, false, false, true)
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        return NpcInteractionService.tryHandleClickTimed(player, 3, npc)
    }

    private fun handleNpcClick4(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (player.isBusy) {
            return DispatchTiming(false, 0L, 0L, null)
        }
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        return NpcInteractionService.tryHandleClickTimed(player, 4, npc)
    }

    private fun handleNpcAttack(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (player.magicId >= 0) {
            player.magicId = -1
        }
        if (player.deathStage >= 1) {
            return DispatchTiming(false, 0L, 0L, null)
        }
        val timing = NpcInteractionService.tryHandleAttackTimed(player, npc)
        if (timing.handled) {
            return timing
        }
        CombatStartService.beginAttackNow(player, npc, CombatIntent.ATTACK_NPC)
        return timing
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

    private fun resolveDistanceMode(
        distanceRule: ObjectInteractionPolicy.DistanceRule,
        interactionType: ObjectInteractionPolicy.InteractionType,
    ): ObjectInteractionDistance.DistanceMode {
        return when (distanceRule) {
            ObjectInteractionPolicy.DistanceRule.LEGACY_OBJECT_DISTANCE -> when (interactionType) {
                ObjectInteractionPolicy.InteractionType.CLICK -> ObjectInteractionDistance.DistanceMode.CLICK
                ObjectInteractionPolicy.InteractionType.ITEM_ON_OBJECT -> ObjectInteractionDistance.DistanceMode.ITEM_ON_OBJECT
                ObjectInteractionPolicy.InteractionType.MAGIC -> ObjectInteractionDistance.DistanceMode.MAGIC
            }
            ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_CARDINAL ->
                ObjectInteractionDistance.DistanceMode.POLICY_NEAREST_BOUNDARY_CARDINAL
            ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_ANY ->
                ObjectInteractionDistance.DistanceMode.POLICY_NEAREST_BOUNDARY_ANY
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
            settledSinceCycle[intent] = PlayerHandler.cycle.toLong()
            return false
        }
        if (PlayerHandler.cycle.toLong() - settledSince < policy.settleTicks) {
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

    private data class ObjectSnapshot(
        val objectData: GameObjectData?,
        val objectDef: GameObjectDef?,
    )
}
