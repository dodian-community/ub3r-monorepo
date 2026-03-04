package net.dodian.uber.game.runtime.interaction

import net.dodian.uber.game.Server
import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.content.objects.services.ObjectInteractionContext
import net.dodian.uber.game.content.objects.ObjectContentDispatcher
import net.dodian.uber.game.content.npcs.spawns.NpcContentDispatcher
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.model.`object`.RS2Object
import net.dodian.uber.game.runtime.interaction.task.InteractionExecutionResult
import org.slf4j.LoggerFactory

object InteractionProcessor {
    private val logger = LoggerFactory.getLogger(InteractionProcessor::class.java)

    @JvmStatic
    fun process(player: Client): InteractionExecutionResult {
        val intent = player.pendingInteraction ?: return InteractionExecutionResult.CANCELLED
        if (player.didTeleport() || player.didMapRegionChange()) {
            player.walkToTask = null
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
        if (player.walkToTask !== intent.task) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val routeStart = System.nanoTime()
        if (
            ObjectInteractionDistance.resolveDistancePosition(
                player,
                intent.task,
                intent.objectId,
                intent.objectData,
                intent.objectDef,
                ObjectInteractionDistance.DistanceMode.CLICK,
            ) == null
        ) {
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        if (intent.option == 1) {
            if (!player.validClient || player.randomed) {
                player.walkToTask = null
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
            if (player.adding) {
                // Preserve legacy debug behavior.
                player.objects.add(
                    RS2Object(
                        intent.objectId,
                        intent.objectPosition.x,
                        intent.objectPosition.y,
                        1,
                    ),
                )
            }
            if (System.currentTimeMillis() < player.walkBlock || player.genie || player.antique) {
                player.walkToTask = null
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
            val playerPos = player.position.copy()
            val xDiff = kotlin.math.abs(playerPos.x - intent.objectPosition.x)
            val yDiff = kotlin.math.abs(playerPos.y - intent.objectPosition.y)
            player.resetAction(false)
            player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
            if (xDiff > 5 || yDiff > 5) {
                player.walkToTask = null
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
        } else if (intent.option == 2) {
            if (player.adding) {
                player.objects.add(
                    RS2Object(
                        intent.objectId,
                        intent.objectPosition.x,
                        intent.objectPosition.y,
                        2,
                    ),
                )
            }
            if (System.currentTimeMillis() < player.walkBlock) {
                player.walkToTask = null
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
            player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
        } else if (intent.option == 3) {
            player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
        }

        val timing =
            ObjectContentDispatcher.tryHandleTimed(
                ObjectInteractionContext.click(
                    client = player,
                    option = intent.option,
                    objectId = intent.task.walkToId,
                    position = intent.task.walkToPosition,
                    obj = intent.objectData,
                ),
            )
        player.walkToTask = null
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
        if (player.walkToTask !== intent.task) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        val routeStart = System.nanoTime()
        if (
            ObjectInteractionDistance.resolveDistancePosition(
                player,
                intent.task,
                intent.objectId,
                intent.objectData,
                intent.objectDef,
                ObjectInteractionDistance.DistanceMode.ITEM_ON_OBJECT,
            ) == null
        ) {
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        var timing = DispatchTiming(false, 0L, 0L, null)
        if (player.playerHasItem(intent.itemId)) {
            player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
            timing =
                ObjectContentDispatcher.tryHandleTimed(
                    ObjectInteractionContext.useItem(
                        client = player,
                        objectId = intent.objectId,
                        position = intent.task.walkToPosition,
                        obj = intent.objectData,
                        itemId = intent.itemId,
                        itemSlot = intent.itemSlot,
                        interfaceId = intent.interfaceId,
                    ),
                )
        }
        player.walkToTask = null
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
        if (player.walkToTask !== intent.task) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        val routeStart = System.nanoTime()
        if (
            ObjectInteractionDistance.resolveDistancePosition(
                player,
                intent.task,
                intent.objectId,
                intent.objectData,
                intent.objectDef,
                ObjectInteractionDistance.DistanceMode.MAGIC,
            ) == null
        ) {
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
        val timing =
            ObjectContentDispatcher.tryHandleTimed(
                ObjectInteractionContext.magic(
                    client = player,
                    objectId = intent.task.walkToId,
                    position = intent.task.walkToPosition,
                    obj = intent.objectData,
                    spellId = intent.spellId,
                ),
            )
        player.walkToTask = null
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
        player.resetAction()
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        player.startFishing(npc.id, 1)
        return NpcContentDispatcher.tryHandleClickTimed(player, 1, npc)
    }

    private fun handleNpcClick2(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (!npc.isAlive) {
            player.send(SendMessage("That monster has been killed!"))
            return DispatchTiming(false, 0L, 0L, null)
        }
        player.resetAction()
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        player.startFishing(npc.id, 2)
        return NpcContentDispatcher.tryHandleClickTimed(player, 2, npc)
    }

    private fun handleNpcClick3(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (player.isBusy) {
            return DispatchTiming(false, 0L, 0L, null)
        }
        player.resetAction()
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        return NpcContentDispatcher.tryHandleClickTimed(player, 3, npc)
    }

    private fun handleNpcClick4(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (player.isBusy) {
            return DispatchTiming(false, 0L, 0L, null)
        }
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
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
        player.resetWalkingQueue()
        player.startAttack(npc)
        return timing
    }

    private fun clear(player: Client) {
        player.pendingInteraction = null
        player.activeInteraction = null
        player.interactionEarliestCycle = 0
        player.interactionTaskHandle = null
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
        if (elapsedMs >= 2L) {
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
}
