package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.skill.thieving.PyramidPlunder
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.engine.lifecycle.PlayerDeferredLifecycleService
import net.dodian.uber.game.events.player.WalkEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.engine.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.engine.systems.follow.FollowService
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

object PacketWalkingService {
    private const val MALFORMED_LOG_INTERVAL_MS = 5_000L
    private val logger = LoggerFactory.getLogger(PacketWalkingService::class.java)
    private val lastMalformedLogMs = AtomicLong(0L)

    @JvmStatic
    fun handle(player: Client, request: WalkRequest) {
        if (player.deathStage > 0 ||
            player.getCurrentHealth() < 1 ||
            player.randomed ||
            !player.validClient ||
            !player.pLoaded ||
            player.isWalkBlocked()
        ) {
            return
        }
        if (player.doingTeleport() || PyramidPlunder.isLooting(player)) {
            return
        }
        if (player.isVerticalTransitionActive) {
            player.resetWalkingQueue()
            return
        }

        if (request.opcode == 164 || request.opcode == 248) {
            if (player.inTrade) {
                player.declineTrade()
            } else if (player.inDuel && !player.duelFight) {
                player.declineDuel()
            }
        }

        if (player.genie) {
            player.genie = false
        }
        if (player.antique) {
            player.antique = false
        }
        player.playerPotato.clear()
        with(player.farming) {
            player.updateCompost()
            player.updateFarmPatch()
        }

        if ((player.getStunTimer() > 0 || player.getSnareTimer() > 0) && request.opcode != 98) {
            player.send(SendMessage(if (player.getSnareTimer() > 0) "You are ensnared!" else "You are currently stunned!"))
            player.resetWalkingQueue()
            return
        }

        if (player.morph) {
            player.unMorph()
        }
        if (player.checkInv) {
            player.checkInv = false
            player.resetItems(3214)
        }
        if (player.pickupWanted) {
            player.pickupWanted = false
            player.attemptGround = null
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(player)
        }

        val stepCount = request.deltasX.size
        if (stepCount <= 0 || stepCount > Player.WALKING_QUEUE_SIZE || request.deltasY.size != stepCount) {
            player.resetWalkingQueue()
            return
        }

        val firstStepX = request.firstStepXAbs - player.mapRegionX * 8
        val firstStepY = request.firstStepYAbs - player.mapRegionY * 8

        player.newWalkCmdSteps = stepCount
        player.newWalkCmdIsRunning = request.running
        player.newWalkCmdX[0] = 0
        player.newWalkCmdY[0] = 0
        player.tmpNWCX[0] = 0
        player.tmpNWCY[0] = 0

        for (i in 1 until stepCount) {
            player.newWalkCmdX[i] = request.deltasX[i]
            player.newWalkCmdY[i] = request.deltasY[i]
            player.tmpNWCX[i] = request.deltasX[i]
            player.tmpNWCY[i] = request.deltasY[i]
        }

        logger.debug(
            "Walk steps {} firstX {} firstY {} running {}",
            player.newWalkCmdSteps,
            firstStepX,
            firstStepY,
            player.newWalkCmdIsRunning,
        )
        for (i in 0 until player.newWalkCmdSteps) {
            player.newWalkCmdX[i] += firstStepX
            player.newWalkCmdY[i] += firstStepY
        }

        if (player.newWalkCmdSteps > 0) {
            DialogueService.closeBlockingDialogue(player, false)

            // Manual click-walk should break follow intent (Luna parity).
            if (request.opcode == 164 || request.opcode == 248) {
                FollowService.cancelFollowIntent(player)
            }

            if (player.inDuel) {
                if (request.opcode != 98) {
                    player.send(SendMessage("You cannot move during this duel!"))
                }
                player.resetWalkingQueue()
                return
            }
            if (player.NpcWanneTalk > 0) {
                player.send(RemoveInterfaces())
                player.NpcWanneTalk = -1
            } else if (!player.isBusy) {
                player.send(RemoveInterfaces())
            }
            player.rerequestAnim()
            PlayerActionCancellationService.cancel(
                player,
                PlayerActionCancelReason.MOVEMENT,
                true,
                false,
                false,
                true,
            )
            player.discord = false
            if (player.checkInv) {
                player.checkInv = false
                player.resetItems(3214)
            }
            player.faceTarget(65535)
        }

        GameEventBus.post(
            WalkEvent(
                player,
                Position(request.firstStepXAbs, request.firstStepYAbs, player.position.z),
            ),
        )

        if (player.chestEventOccur && request.opcode != 98) {
            player.chestEventOccur = false
        }
        player.convoId = -1
        if (DialogueService.hasBlockingDialogue(player)) {
            DialogueService.closeBlockingDialogue(player, true)
        }
        if (player.refundSlot != -1) {
            player.refundSlot = -1
        }
        if (player.herbMaking != -1) {
            player.herbMaking = -1
        }
        if (player.IsBanking) {
            player.IsBanking = false
            player.send(RemoveInterfaces())
            player.checkItemUpdate()
        }
        if (player.checkBankInterface) {
            player.checkBankInterface = false
            player.send(RemoveInterfaces())
            player.checkItemUpdate()
        }
        if (player.bankStyleViewOpen) {
            player.clearBankStyleView()
            player.send(RemoveInterfaces())
            player.checkItemUpdate()
        }
        if (player.isPartyInterface) {
            player.isPartyInterface = false
            player.send(RemoveInterfaces())
            player.checkItemUpdate()
        }
        if (player.isShopping) {
            player.MyShopID = -1
            player.send(RemoveInterfaces())
            player.checkItemUpdate()
        }
    }

    @JvmStatic
    fun rejectMalformedWalk(
        player: Client,
        opcode: Int,
        packetSize: Int,
        firstStepXAbs: Int,
        firstStepYAbs: Int,
        reason: String,
    ) {
        val now = System.currentTimeMillis()
        val last = lastMalformedLogMs.get()
        player.resetWalkingQueue()
        if (now - last < MALFORMED_LOG_INTERVAL_MS || !lastMalformedLogMs.compareAndSet(last, now)) {
            return
        }
        logger.warn(
            "Rejected malformed walk packet player={} opcode={} size={} firstStep=({}, {}) region=({}, {}) reason={}",
            player.getPlayerName(),
            opcode,
            packetSize,
            firstStepXAbs,
            firstStepYAbs,
            player.mapRegionX,
            player.mapRegionY,
            reason,
        )
    }
}
