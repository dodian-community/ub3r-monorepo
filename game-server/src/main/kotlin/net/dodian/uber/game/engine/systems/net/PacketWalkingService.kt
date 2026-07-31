package net.dodian.uber.game.engine.systems.net
import net.dodian.uber.game.api.content.ContentActions

import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.skills.thieving.ThievingModule
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.engine.lifecycle.PlayerDeferredLifecycleService
import net.dodian.uber.game.events.player.WalkEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.RouteDestination
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.engine.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.engine.systems.action.PlayerActionController
import net.dodian.uber.game.engine.systems.follow.FollowService
import net.dodian.uber.game.engine.systems.interaction.ui.PlayerSocialApproachService
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService
import net.dodian.uber.game.engine.systems.interaction.InteractionProcessor
import net.dodian.uber.game.engine.state.GroundItemIntentStateAdapter
import net.dodian.uber.game.engine.state.TeleportIntentStateAdapter
import net.dodian.uber.game.economy.PriceCheckerService
import net.dodian.uber.game.engine.systems.skills.SkillItemListMenuService
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
            player.isWalkBlocked
        ) {
            return
        }
        if (TeleportIntentStateAdapter.isTeleporting(player) || ThievingModule.isLooting(player.asSkillPlayer())) {
            return
        }
        if (net.dodian.uber.game.engine.systems.action.VerticalTransitionService.isActive(player)) {
            player.resetWalkingQueue()
            return
        }

        if (PriceCheckerService.isOpen(player)) {
            PriceCheckerService.close(player)
        }

        if (request.opcode == 164 || request.opcode == 248) {
            if (player.inTrade) {
                TradeDuelSessionService.closeOpenTrade(player)
            } else if (player.inDuel && !player.duelFight) {
                TradeDuelSessionService.closeOpenDuel(player)
            }
        }

        if (player.genie) {
            player.genie = false
        }
        if (player.antique) {
            player.antique = false
        }
        player.clearPlayerPotatoState()
        player.asSkillPlayer().farmingState.refreshVisuals()

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
        val stepCount = request.deltasX.size
        if (stepCount <= 0 || stepCount > RouteDestination.MAX_WAYPOINTS || request.deltasY.size != stepCount) {
            player.resetWalkingQueue()
            return
        }

        val routeDecision =
            if (WalkingRouteService.isPlainWalkOpcode(request.opcode)) {
                WalkingRouteService.preparePlainRoute(player, request)
            } else {
                WalkingRouteService.RouteDecision(
                    WalkingRouteService.requestWaypoints(request, player.position.z),
                    WalkingRouteService.destination(request, player.position.z),
                )
            }
        if (routeDecision == null) {
            return
        }
        val eventDestination = routeDecision.destination
        // The client sends the walk-to-item request and the pickup-item request together
        // from the same click; only treat this as "the player changed their mind and is
        // walking elsewhere" if the new destination isn't the tile they're already trying
        // to pick up from — otherwise this walk request (which is what carries them TO the
        // item) cancels the pickup intent before they ever arrive, forcing a second click.
        if (GroundItemIntentStateAdapter.wantsPickup(player)) {
            val pendingTarget = GroundItemIntentStateAdapter.target(player)
            val walkingTowardPendingTarget = pendingTarget != null &&
                eventDestination.x == pendingTarget.x &&
                eventDestination.y == pendingTarget.y
            if (!walkingTowardPendingTarget) {
                GroundItemIntentStateAdapter.clearPickup(player)
                PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(player)
            }
        }
        player.replaceMovementRoute(routeDecision.waypoints, request.running)
        logger.debug(
            "Walk waypoints {} destinationX {} destinationY {} running {}",
            player.movementRouteSize(),
            eventDestination.x,
            eventDestination.y,
            request.running,
        )

        if (player.hasMovementRoute()) {
            DialogueService.closeBlockingDialogue(player, false)

            // Manual click-walk should break follow intent (Luna parity) and object interaction.
            if (request.opcode == 164 || request.opcode == 248) {
                FollowService.cancelFollowIntent(player)
                PlayerSocialApproachService.cancel(player)
                InteractionProcessor.cancel(player)
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
            if (request.opcode != 98) {
                ContentActions.cancel(
                    player = player,
                    reason = PlayerActionCancelReason.MOVEMENT,
                    fullResetAnimation = true,
                    clearDialogue = false,
                    closeInterfaces = false,
                    resetCompatibilityState = true,
                )
            } else {
                PlayerActionController.cancel(player, PlayerActionCancelReason.MOVEMENT)
                PlayerActionCancellationService.resetCompatibilityState(player, true)
            }
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
                eventDestination,
            ),
        )

        if (player.skillingEventState.isChestEventPendingMove && request.opcode != 98) {
            player.skillingEventState = player.skillingEventState.withChestEventPendingMove(false)
        }
        player.convoId = -1
        if (DialogueService.hasBlockingDialogue(player)) {
            DialogueService.closeBlockingDialogue(player, true)
        }
        if (player.refundSlot != -1) {
            player.refundSlot = -1
        }
        SkillItemListMenuService.clear(player)
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
