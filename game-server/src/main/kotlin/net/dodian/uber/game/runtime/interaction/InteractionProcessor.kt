package net.dodian.uber.game.runtime.interaction

import net.dodian.uber.game.Server
import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.content.objects.ObjectContentDispatcher
import net.dodian.uber.game.content.npcs.spawns.NpcContentDispatcher
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.model.`object`.RS2Object
import org.slf4j.LoggerFactory

object InteractionProcessor {
    private val logger = LoggerFactory.getLogger(InteractionProcessor::class.java)

    @JvmStatic
    fun process(player: Client) {
        val intent = player.pendingInteraction ?: return
        if (player.didTeleport() || player.didMapRegionChange()) {
            player.walkToTask = null
            clear(player)
            return
        }
        if (PlayerHandler.cycle < player.interactionEarliestCycle) {
            return
        }
        when (intent) {
            is NpcInteractionIntent -> processNpcInteraction(player, intent)
            is ObjectClickIntent -> processObjectClick(player, intent)
            is ItemOnObjectIntent -> processItemOnObject(player, intent)
            is MagicOnObjectIntent -> processMagicOnObject(player, intent)
        }
    }

    private fun processNpcInteraction(player: Client, intent: NpcInteractionIntent) {
        val startNs = System.nanoTime()
        val npc = Server.npcManager.getNpc(intent.npcIndex)
        if (npc == null) {
            clear(player)
            return
        }

        if (player.randomed || player.UsingAgility) {
            clear(player)
            return
        }

        val range =
            if (intent.option == NPC_ATTACK_OPTION && player.getAttackStyle() != 0) {
                5
            } else {
                1
            }

        if (!player.goodDistanceEntity(npc, range) || npc.position.withinDistance(player.position, 0)) {
            return
        }

        player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
        when (intent.option) {
            1 -> handleNpcClick1(player, npc)
            2 -> handleNpcClick2(player, npc)
            3 -> handleNpcClick3(player, npc)
            4 -> handleNpcClick4(player, npc)
            NPC_ATTACK_OPTION -> handleNpcAttack(player, npc)
        }
        clear(player)
        slowLogIfNeeded(player, intent, startNs)
    }

    private fun processObjectClick(player: Client, intent: ObjectClickIntent) {
        val startNs = System.nanoTime()
        if (player.disconnected || player.randomed || player.UsingAgility) {
            clear(player)
            return
        }
        if (player.walkToTask !== intent.task) {
            clear(player)
            return
        }

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
            return
        }

        if (intent.option == 1) {
            if (!player.validClient || player.randomed) {
                player.walkToTask = null
                clear(player)
                return
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
                return
            }
            val playerPos = player.position.copy()
            val xDiff = kotlin.math.abs(playerPos.x - intent.objectPosition.x)
            val yDiff = kotlin.math.abs(playerPos.y - intent.objectPosition.y)
            player.resetAction(false)
            player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
            if (xDiff > 5 || yDiff > 5) {
                player.walkToTask = null
                clear(player)
                return
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
                return
            }
            player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
        } else if (intent.option == 3) {
            player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
        }

        ObjectContentDispatcher.tryHandleClick(player, intent.option, intent.task.walkToId, intent.task.walkToPosition, intent.objectData)
        player.walkToTask = null
        clear(player)
        slowLogIfNeeded(player, intent, startNs)
    }

    private fun processItemOnObject(player: Client, intent: ItemOnObjectIntent) {
        val startNs = System.nanoTime()
        if (player.disconnected || player.randomed) {
            clear(player); return
        }
        if (player.walkToTask !== intent.task) {
            clear(player); return
        }
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
            return
        }

        if (player.playerHasItem(intent.itemId)) {
            player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
            ObjectContentDispatcher.tryHandleUseItem(
                player,
                intent.objectId,
                intent.task.walkToPosition,
                intent.objectData,
                intent.itemId,
                intent.itemSlot,
                intent.interfaceId,
            )
        }
        player.walkToTask = null
        clear(player)
        slowLogIfNeeded(player, intent, startNs)
    }

    private fun processMagicOnObject(player: Client, intent: MagicOnObjectIntent) {
        val startNs = System.nanoTime()
        if (player.disconnected || player.randomed || player.UsingAgility) {
            clear(player); return
        }
        if (player.walkToTask !== intent.task) {
            clear(player); return
        }
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
            return
        }

        player.setFocus(intent.objectPosition.x, intent.objectPosition.y)
        ObjectContentDispatcher.tryHandleMagic(player, intent.task.walkToId, intent.task.walkToPosition, intent.objectData, intent.spellId)
        player.walkToTask = null
        clear(player)
        slowLogIfNeeded(player, intent, startNs)
    }

    private fun handleNpcClick1(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (!npc.isAlive) {
            player.send(SendMessage("That monster has been killed!"))
            return
        }
        player.resetAction()
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        player.startFishing(npc.id, 1)
        NpcContentDispatcher.tryHandleClick(player, 1, npc)
    }

    private fun handleNpcClick2(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (!npc.isAlive) {
            player.send(SendMessage("That monster has been killed!"))
            return
        }
        player.resetAction()
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        player.startFishing(npc.id, 2)
        NpcContentDispatcher.tryHandleClick(player, 2, npc)
    }

    private fun handleNpcClick3(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (player.isBusy) {
            return
        }
        player.resetAction()
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        NpcContentDispatcher.tryHandleClick(player, 3, npc)
    }

    private fun handleNpcClick4(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (player.isBusy) {
            return
        }
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        NpcContentDispatcher.tryHandleClick(player, 4, npc)
    }

    private fun handleNpcAttack(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (player.magicId >= 0) {
            player.magicId = -1
        }
        if (player.deathStage >= 1) {
            return
        }
        if (NpcContentDispatcher.tryHandleAttack(player, npc)) {
            return
        }
        player.resetWalkingQueue()
        player.startAttack(npc)
    }

    private fun clear(player: Client) {
        player.pendingInteraction = null
        player.activeInteraction = null
        player.interactionEarliestCycle = 0
    }

    private fun slowLogIfNeeded(player: Client, intent: InteractionIntent, startNs: Long) {
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
        if (elapsedMs >= 2L) {
            logger.warn("Slow interaction: type={} player={} {}ms", intent::class.java.name, player.playerName, elapsedMs)
        }
    }

    private const val NPC_ATTACK_OPTION = 5
}
