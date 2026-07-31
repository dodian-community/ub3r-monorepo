package net.dodian.uber.game.engine.systems.interaction
import net.dodian.uber.game.api.content.ContentActions

import net.dodian.uber.game.Server
import net.dodian.cache.objects.GameObjectData
import net.dodian.cache.objects.GameObjectDef
import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.engine.config.gameWorldId
import net.dodian.uber.game.engine.systems.interaction.objects.ObjectContentRegistry
import net.dodian.uber.game.engine.systems.interaction.objects.ObjectClickLoggingService
import net.dodian.uber.game.engine.systems.interaction.objects.ObjectInteractionService
import net.dodian.uber.game.engine.systems.interaction.npcs.BankerApproachFallbackService
import net.dodian.uber.game.engine.systems.interaction.npcs.NpcContentDispatcher
import net.dodian.uber.game.engine.routing.WorldRouteService
import net.dodian.uber.game.engine.systems.interaction.items.ItemOnNpcContentService
import net.dodian.uber.game.objects.travel.LegendsGuildGateService
import net.dodian.uber.game.engine.systems.follow.FollowRouting
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.item.ItemOnNpcEvent
import net.dodian.uber.game.events.magic.MagicOnNpcEvent
import net.dodian.uber.game.events.magic.MagicOnPlayerEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.objects.GlobalObject
import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.engine.systems.combat.AttackStartDedupeService
import net.dodian.uber.game.engine.systems.combat.AttackStartDedupeService.Decision
import net.dodian.uber.game.engine.systems.combat.CombatCommandService
import net.dodian.uber.game.engine.systems.combat.CombatIntent
import net.dodian.uber.game.engine.systems.combat.CombatStartService
import net.dodian.uber.game.engine.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.engine.systems.interaction.scheduler.InteractionExecutionResult
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher
import net.dodian.uber.game.engine.systems.quests.QuestInteractionDispatcher
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.engine.systems.skills.SkillPolicyMetrics
import net.dodian.uber.game.engine.systems.skills.SkillPolicyResult
import net.dodian.uber.game.engine.systems.skills.SkillPolicyRoute
import net.dodian.uber.game.engine.util.Misc
import net.dodian.uber.game.engine.config.runtimePhaseWarnMs
import net.dodian.uber.game.engine.state.InteractionSessionStateAdapter
import net.dodian.uber.game.engine.state.TeleportIntentStateAdapter
import org.slf4j.LoggerFactory
import java.util.IdentityHashMap

object InteractionProcessor {
    private val logger = LoggerFactory.getLogger(InteractionProcessor::class.java)
    private val settledSinceCycle = IdentityHashMap<InteractionIntent, Long>()
    private val objectDistanceRejectLogged = IdentityHashMap<InteractionIntent, Boolean>()
    private val bankerRouteFailCount = IdentityHashMap<InteractionIntent, Int>()
    /**
     * Tracks the last position we routed to per intent — mirrors tarnish's [Waypoint.lastPosition].
     * When a route is in progress and the target hasn't moved, we skip re-routing and let the
     * walk queue drain naturally.
     */
    private val lastRoutePosition = IdentityHashMap<InteractionIntent, Position>()

    @JvmStatic
    fun process(player: Client): InteractionExecutionResult {
        return try {
            val intent = InteractionSessionStateAdapter.pending(player) ?: return InteractionExecutionResult.CANCELLED
            if (TeleportIntentStateAdapter.didTeleport(player) || player.disconnected || !player.isActive || !player.validClient) {
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
            if (PlayerRegistry.cycle < player.interactionEarliestCycle) {
                return InteractionExecutionResult.WAITING
            }
            when (intent) {
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
        } catch (throwable: Throwable) {
            logger.error(
                "Interaction processing failed slot={} name={} intent={}",
                player.slot,
                player.playerName,
                InteractionSessionStateAdapter.pending(player)?.javaClass?.simpleName ?: "none",
                throwable,
            )
            clear(player)
            InteractionExecutionResult.CANCELLED
        }
    }

    private fun processNpcInteraction(player: Client, intent: NpcInteractionIntent): InteractionExecutionResult {
        if (player.inDuel) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
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

        val legendsGuardFrontLane = LegendsGuildGateService.isFrontLaneInteraction(player, npc, intent.option)
        val nonCombatReach =
            if (intent.option == NPC_ATTACK_OPTION) {
                null
            } else {
                EntityInteractionReach.resolve(player, npc, range)
            }
        val overlaps =
            nonCombatReach?.let { it == EntityReachResult.OVERLAPPING }
                ?: (
                    player.position.z == npc.position.z &&
                        player.position.x >= npc.position.x &&
                        player.position.x < npc.position.x + npc.size &&
                        player.position.y >= npc.position.y &&
                        player.position.y < npc.position.y + npc.size
                )
        val outsideRange =
            nonCombatReach?.let { it != EntityReachResult.REACHED && it != EntityReachResult.OVERLAPPING }
                ?: !player.goodDistanceEntity(npc, range)

        val routeStart = System.nanoTime()
        if (overlaps || (!legendsGuardFrontLane && outsideRange)) {
            if (!overlaps && BankerApproachFallbackService.shouldAttemptFallback(player, npc, intent.option)) {
                if (player.position.withinDistance(npc.position, 2)
                    && WorldRouteService.hasLineOfSight(player.position, npc.position, player.size, npc.size)
                ) {
                    if (gameWorldId == 2) logger.debug("[W2-BANKER] Ap-range+LOS hit npcId={} player=({},{}) npc=({},{})", npc.id, player.position.x, player.position.y, npc.position.x, npc.position.y)
                } else {
                    if (gameWorldId == 2) logger.debug("[W2-BANKER] routing npcId={} player=({},{}) npc=({},{})", npc.id, player.position.x, player.position.y, npc.position.x, npc.position.y)
                    val routed = BankerApproachFallbackService.tryRouteCustomerSide(player, npc)
                    if (!routed) {
                        val fails = (bankerRouteFailCount[intent] ?: 0) + 1
                        bankerRouteFailCount[intent] = fails
                        if (fails >= 3) {
                            if (gameWorldId == 2) logger.debug("[W2-BANKER] routing failed {} times — cancelling", fails)
                            bankerRouteFailCount.remove(intent)
                            player.sendMessage("I can't reach that!")
                            clear(player)
                            return InteractionExecutionResult.CANCELLED
                        }
                    } else {
                        bankerRouteFailCount.remove(intent)
                    }
                    return InteractionExecutionResult.WAITING
                }
            } else {
                val walkInProgress = player.hasMovementRoute()
                val lastRouted = lastRoutePosition[intent]
                if (walkInProgress && lastRouted != null && lastRouted == npc.position) {
                    return InteractionExecutionResult.WAITING
                }
                val routed = FollowRouting.routeToEntityBoundary(player, npc.position.x, npc.position.y, npc.size, npc.position.z, targetNpc = npc)
                if (!routed) {
                    player.sendMessage("I can't reach that!")
                    clear(player)
                    return InteractionExecutionResult.CANCELLED
                }
                lastRoutePosition[intent] = npc.position
                return InteractionExecutionResult.WAITING
            }
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
        net.dodian.uber.game.persistence.audit.ConsoleAuditLog.npcInteraction(
            player = player,
            npc = npc,
            option = intent.option,
            opcode = intent.opcode,
            handled = timing.handled,
            handlerSource = timing.handlerName,
            elapsedNanos = System.nanoTime() - startNs,
        )
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
        if (player.inDuel) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        val startNs = System.nanoTime()
        if (player.disconnected || player.randomed || player.UsingAgility) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (!InteractionSessionStateAdapter.isPending(player, intent)) {
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
        val questObjectBinding = PluginRegistry.currentQuests().objectBinding(intent.option, intent.objectId)
        val clickContext =
            ObjectInteractionContext.click(
                client = player,
                option = intent.option,
                objectId = intent.objectId,
                position = targetPosition,
                obj = routeSnapshot.objectData,
                packetOpcode = intent.opcode,
            )
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

        val hasObjectContent = ObjectContentRegistry.resolveCandidates(intent.objectId, targetPosition).isNotEmpty()
        val cacheActionNoop = isCacheActionNoop(routeSnapshot.objectData)
        val shouldRouteToObject =
            skillObjectBinding != null ||
                questObjectBinding != null ||
                hasObjectContent ||
                cacheActionNoop

        val routeNs: Long
        if (shouldRouteToObject) {
            val approach =
                ensureObjectApproached(
                    player = player,
                    intent = intent,
                    objectId = intent.objectId,
                    targetPosition = targetPosition,
                    routeSnapshot = routeSnapshot,
                    policy = policy,
                    context = clickContext,
                    allowClosestCompletion = skillObjectBinding == null && questObjectBinding == null && !hasObjectContent && cacheActionNoop,
                ) {
                    skillObjectBinding?.let {
                        SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.OBJECT, SkillPolicyResult.POLICY_REJECT)
                    }
                }
            approach.result?.let { return it }
            routeNs = approach.routeNs
            if (!isSettleGateSatisfied(player, intent, policy)) {
                skillObjectBinding?.let {
                    SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.OBJECT, SkillPolicyResult.SETTLE_WAIT)
                }
                if (gameWorldId == 2) logger.debug("[W2-DISPATCH] settle gate WAITING objId={}", intent.objectId)
                return InteractionExecutionResult.WAITING
            }
        } else {
            // No cache action and no server handler: trust the client's own pathing, then log as unhandled.
            // Wait for movement to settle, then fall through so tryHandleTimed logs UNHANDLED.
            routeNs = 0L
            val settled =
                player.primaryDirection == -1 &&
                    player.secondaryDirection == -1 &&
                    !player.hasMovementRoute()
            if (!settled) return InteractionExecutionResult.WAITING
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
            ContentActions.cancel(player, PlayerActionCancelReason.OBJECT_INTERACTION, false, false, false, true)
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
        if (gameWorldId == 2) logger.debug("[W2-DISPATCH] pre-stillPresent objId={} option={} pos=({},{}) binding={} objectDefNull={}",
            intent.objectId, intent.option, targetPosition.x, targetPosition.y, skillObjectBinding != null, intent.objectDef == null)
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
        if (gameWorldId == 2) logger.debug("[W2-DISPATCH] result objId={} handled={} handlerName={} handlerNs={}",
            intent.objectId, timing.handled, timing.handlerName, timing.handlerNs)
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
        if (player.inDuel) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        val startNs = System.nanoTime()
        if (player.disconnected || player.randomed) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (!InteractionSessionStateAdapter.isPending(player, intent)) {
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
        val questItemOnObjectBinding = PluginRegistry.currentQuests().itemOnObjectBinding(intent.objectId, intent.itemId)
        val itemOnObjectContext =
            ObjectInteractionContext.useItem(
                client = player,
                objectId = intent.objectId,
                position = targetPosition,
                obj = routeSnapshot.objectData,
                itemId = intent.itemId,
                itemSlot = intent.itemSlot,
                interfaceId = intent.interfaceId,
                packetOpcode = intent.opcode,
            )
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

        val hasObjectContent = ObjectContentRegistry.resolveCandidates(intent.objectId, targetPosition).isNotEmpty()
        val cacheActionNoop = isCacheActionNoop(routeSnapshot.objectData)
        val shouldRouteToObject =
            skillItemOnObjectBinding != null ||
                questItemOnObjectBinding != null ||
                hasObjectContent ||
                cacheActionNoop

        val routeNs: Long
        if (shouldRouteToObject) {
            val approach =
                ensureObjectApproached(
                    player = player,
                    intent = intent,
                    objectId = intent.objectId,
                    targetPosition = targetPosition,
                    routeSnapshot = routeSnapshot,
                    policy = policy,
                    context = itemOnObjectContext,
                    allowClosestCompletion = skillItemOnObjectBinding == null && questItemOnObjectBinding == null && !hasObjectContent && cacheActionNoop,
                ) {
                    skillItemOnObjectBinding?.let {
                        SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.ITEM_ON_OBJECT, SkillPolicyResult.POLICY_REJECT)
                    }
                }
            approach.result?.let { return it }
            routeNs = approach.routeNs
            if (!isSettleGateSatisfied(player, intent, policy)) {
                skillItemOnObjectBinding?.let {
                    SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.ITEM_ON_OBJECT, SkillPolicyResult.SETTLE_WAIT)
                }
                return InteractionExecutionResult.WAITING
            }
        } else {
            routeNs = 0L
            val settled =
                player.primaryDirection == -1 &&
                    player.secondaryDirection == -1 &&
                    !player.hasMovementRoute()
            if (!settled) return InteractionExecutionResult.WAITING
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
        if (player.inDuel) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        val startNs = System.nanoTime()
        if (player.disconnected || player.randomed || player.UsingAgility) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
        if (!InteractionSessionStateAdapter.isPending(player, intent)) {
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
        val questMagicOnObjectBinding = PluginRegistry.currentQuests().magicOnObjectBinding(intent.objectId, intent.spellId)
        val magicOnObjectContext =
            ObjectInteractionContext.magic(
                client = player,
                objectId = intent.objectId,
                position = targetPosition,
                obj = routeSnapshot.objectData,
                spellId = intent.spellId,
                packetOpcode = intent.opcode,
            )
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

        val hasObjectContent = ObjectContentRegistry.resolveCandidates(intent.objectId, targetPosition).isNotEmpty()
        val cacheActionNoop = isCacheActionNoop(routeSnapshot.objectData)
        val shouldRouteToObject =
            skillMagicOnObjectBinding != null ||
                questMagicOnObjectBinding != null ||
                hasObjectContent ||
                cacheActionNoop

        val routeNs: Long
        if (shouldRouteToObject) {
            val approach =
                ensureObjectApproached(
                    player = player,
                    intent = intent,
                    objectId = intent.objectId,
                    targetPosition = targetPosition,
                    routeSnapshot = routeSnapshot,
                    policy = policy,
                    context = magicOnObjectContext,
                    allowClosestCompletion = skillMagicOnObjectBinding == null && questMagicOnObjectBinding == null && !hasObjectContent && cacheActionNoop,
                ) {
                    skillMagicOnObjectBinding?.let {
                        SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.MAGIC_ON_OBJECT, SkillPolicyResult.POLICY_REJECT)
                    }
                }
            approach.result?.let { return it }
            routeNs = approach.routeNs
            if (!isSettleGateSatisfied(player, intent, policy)) {
                skillMagicOnObjectBinding?.let {
                    SkillPolicyMetrics.record(it.preset, SkillPolicyRoute.MAGIC_ON_OBJECT, SkillPolicyResult.SETTLE_WAIT)
                }
                return InteractionExecutionResult.WAITING
            }
        } else {
            routeNs = 0L
            val settled =
                player.primaryDirection == -1 &&
                    player.secondaryDirection == -1 &&
                    !player.hasMovementRoute()
            if (!settled) return InteractionExecutionResult.WAITING
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
        if (player.inDuel) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
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
        if (!InteractionSessionStateAdapter.isPending(player, intent)) {
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

        val reach = EntityInteractionReach.resolve(player, npc, 1)
        val overlaps = reach == EntityReachResult.OVERLAPPING
        val outsideRange = reach != EntityReachResult.REACHED && !overlaps

        val routeStart = System.nanoTime()
        if (overlaps || outsideRange) {
            val walkInProgress = player.hasMovementRoute()
            val lastRouted = lastRoutePosition[intent]
            if (walkInProgress && lastRouted != null && lastRouted == npc.position) {
                return InteractionExecutionResult.WAITING
            }
            val routed = FollowRouting.routeToEntityBoundary(player, npc.position.x, npc.position.y, npc.size, npc.position.z, targetNpc = npc)
            if (!routed) {
                player.sendMessage("I can't reach that!")
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
            lastRoutePosition[intent] = npc.position
            return InteractionExecutionResult.WAITING
        }
        val routeNs = System.nanoTime() - routeStart

        player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
        player.faceNpc(intent.npcIndex)
        val handled = GameEventBus.postWithResult(ItemOnNpcEvent(player, intent.itemId, intent.itemSlot, intent.npcIndex, npc))
        val handledBySkill = !handled && SkillInteractionDispatcher.tryHandleItemOnNpc(player, npc, intent.itemId, intent.itemSlot)
        if (!handled && !handledBySkill) {
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
            if (handled) "GameEventBus" else if (handledBySkill) SkillInteractionDispatcher::class.java.name else ItemOnNpcContentService::class.java.name,
            startNs,
        )
        return InteractionExecutionResult.COMPLETE
    }

    private fun processMagicOnNpc(player: Client, intent: MagicOnNpcIntent): InteractionExecutionResult {
        if (player.inDuel) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
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
        if (!InteractionSessionStateAdapter.isPending(player, intent)) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        val overlaps = player.position.z == npc.position.z &&
                player.position.x >= npc.position.x &&
                player.position.x < npc.position.x + npc.size &&
                player.position.y >= npc.position.y &&
                player.position.y < npc.position.y + npc.size

        val routeStart = System.nanoTime()
        if (overlaps || !player.goodDistanceEntity(npc, 5)) {
            val walkInProgress = player.hasMovementRoute()
            val lastRouted = lastRoutePosition[intent]
            if (walkInProgress && lastRouted != null && lastRouted == npc.position) {
                return InteractionExecutionResult.WAITING
            }
            val routed = FollowRouting.routeToEntityBoundary(player, npc.position.x, npc.position.y, npc.size, npc.position.z, targetNpc = npc)
            if (!routed) {
                player.sendMessage("I can't reach that!")
                clear(player)
                return InteractionExecutionResult.CANCELLED
            }
            lastRoutePosition[intent] = npc.position
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
        if (player.inDuel && intent.victimIndex != player.duel_with) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
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
        if (!InteractionSessionStateAdapter.isPending(player, intent)) {
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
        if (player.inDuel && intent.victimIndex != player.duel_with) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }
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
        if (!InteractionSessionStateAdapter.isPending(player, intent)) {
            clear(player)
            return InteractionExecutionResult.CANCELLED
        }

        player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
        CombatCommandService.requestAttack(player, victim, CombatIntent.ATTACK_PLAYER)
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
        ContentActions.cancel(player, PlayerActionCancelReason.NPC_INTERACTION, false, false, false, true)
        player.faceNpc(npc.slot)
        player.setInteractionAnchor(npc.position.x, npc.position.y, npc.position.z)
        return NpcContentDispatcher.tryHandleClickTimed(player, 1, npc)
    }

    private fun handleNpcClick2(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        ContentActions.cancel(player, PlayerActionCancelReason.NPC_INTERACTION, false, false, false, true)
        player.faceNpc(npc.slot)
        player.setInteractionAnchor(npc.position.x, npc.position.y, npc.position.z)
        return NpcContentDispatcher.tryHandleClickTimed(player, 2, npc)
    }

    private fun handleNpcClick3(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc): DispatchTiming {
        if (player.isBusy) {
            return DispatchTiming(false, 0L, 0L, null)
        }
        ContentActions.cancel(player, PlayerActionCancelReason.NPC_INTERACTION, false, false, false, true)
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
        CombatCommandService.requestAttack(player, npc, CombatIntent.ATTACK_NPC)
        return timing
    }

    private fun refreshCombatTargetFacing(player: Client, target: Entity) {
        player.target = target
        if (target is net.dodian.uber.game.model.entity.npc.Npc) {
            player.faceNpc(target.slot)
        } else {
            player.facePlayer(target.slot)
        }
    }

    private fun settleNpcInteractionMovement(player: Client) {
        if (player.hasMovementRoute()) {
            player.resetWalkingQueue()
        }
    }

    private fun clear(player: Client) {
        InteractionSessionStateAdapter.pending(player)?.let {
            settledSinceCycle.remove(it)
            objectDistanceRejectLogged.remove(it)
            lastRoutePosition.remove(it)
            bankerRouteFailCount.remove(it)
        }
        InteractionSessionStateAdapter.clear(player)
        player.interactionEarliestCycle = 0
        player.interactionTaskHandle = null
    }

    @JvmStatic
    fun cancel(player: Client) {
        clear(player)
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
        if (GlobalObject.hasGlobalObject(WorldObject(objectId, position.x, position.y, position.z, 10))) {
            return true
        }
        return net.dodian.uber.game.Server.objects.any { obj ->
            obj.x == position.x && obj.y == position.y && obj.z == position.z &&
            (obj.id == objectId || GameObjectData.forId(obj.id).childIds?.contains(objectId) == true)
        }
    }

    private fun ensureObjectApproached(
        player: Client,
        intent: InteractionIntent,
        objectId: Int,
        targetPosition: Position,
        routeSnapshot: ObjectSnapshot,
        policy: ObjectInteractionPolicy,
        context: ObjectInteractionContext,
        allowClosestCompletion: Boolean,
        recordReject: () -> Unit,
    ): ObjectApproachCheck {
        val routeStart = System.nanoTime()
        if (isPolicyDistanceSatisfied(player, targetPosition, objectId, routeSnapshot, policy)) {
            ObjectInteractionMovementSettleService.clearQueuedWalkIfReached(player, targetPosition, objectId, routeSnapshot.objectDef)
            lastRoutePosition.remove(intent)
            return ObjectApproachCheck(result = null, routeNs = System.nanoTime() - routeStart)
        }

        // Tarnish Waypoint.lastPosition guard: if the target hasn't moved and a walk is already
        // in progress, skip re-routing and let the queue drain. Re-routing every tick would stomp
        // the walk queue before the player reaches the object.
        val walkInProgress = player.hasMovementRoute()
        val lastRouted = lastRoutePosition[intent]
        if (walkInProgress && lastRouted != null && lastRouted == targetPosition) {
            return ObjectApproachCheck(result = InteractionExecutionResult.WAITING, routeNs = System.nanoTime() - routeStart)
        }

        val approach =
            ObjectApproachRoutingService.ensureReached(
                player = player,
                objectId = objectId,
                targetPosition = targetPosition,
                objectData = routeSnapshot.objectData,
                objectDef = routeSnapshot.objectDef,
            )
        val routeNs = System.nanoTime() - routeStart
        return when (approach.status) {
            ApproachStatus.REACHED -> {
                lastRoutePosition.remove(intent)
                ObjectApproachCheck(result = null, routeNs = routeNs)
            }
            ApproachStatus.ROUTED -> {
                // Record that we just routed to this position so we don't re-route next tick.
                lastRoutePosition[intent] = targetPosition
                ObjectApproachCheck(result = InteractionExecutionResult.WAITING, routeNs = routeNs)
            }
            ApproachStatus.PARKED_CLOSEST -> {
                lastRoutePosition.remove(intent)
                if (allowClosestCompletion) {
                    completeCacheActionNoop(player, context, routeNs)
                    ObjectApproachCheck(result = InteractionExecutionResult.COMPLETE, routeNs = routeNs)
                } else {
                    if (objectDistanceRejectLogged.putIfAbsent(intent, true) == null) {
                        ObjectClickLoggingService.logRouteReject(
                            context = context,
                            status = approach.status.name,
                            elapsedNanos = routeNs,
                        )
                    }
                    recordReject()
                    clear(player)
                    ObjectApproachCheck(result = InteractionExecutionResult.CANCELLED, routeNs = routeNs)
                }
            }
            ApproachStatus.UNREACHABLE,
            ApproachStatus.MISSING_OBJECT -> {
                lastRoutePosition.remove(intent)
                if (objectDistanceRejectLogged.putIfAbsent(intent, true) == null) {
                    ObjectClickLoggingService.logRouteReject(
                        context = context,
                        status = approach.status.name,
                        elapsedNanos = routeNs,
                    )
                }
                recordReject()
                clear(player)
                ObjectApproachCheck(result = InteractionExecutionResult.CANCELLED, routeNs = routeNs)
            }
        }
    }

    private fun isPolicyDistanceSatisfied(
        player: Client,
        targetPosition: Position,
        objectId: Int,
        routeSnapshot: ObjectSnapshot,
        policy: ObjectInteractionPolicy,
    ): Boolean {
        if (policy.distanceRule == ObjectInteractionPolicy.DistanceRule.REACHABLE) {
            return false
        }
        return ObjectInteractionDistance.resolveDistancePosition(
            player,
            targetPosition,
            objectId,
            routeSnapshot.objectData,
            routeSnapshot.objectDef,
            resolveDistanceMode(policy.distanceRule),
        ) != null
    }

    private fun isCacheActionNoop(objectData: GameObjectData?): Boolean =
        objectData != null && ObjectClickLoggingService.isCacheActionObject(objectData)

    private fun completeCacheActionNoop(player: Client, context: ObjectInteractionContext, routeNs: Long) {
        ObjectClickLoggingService.logReachedNoHandler(
            context,
            routeOutcome = ApproachStatus.PARKED_CLOSEST.name,
            elapsedNanos = routeNs,
        )
        if (player.hasMovementRoute()) {
            player.resetWalkingQueue()
        }
        clear(player)
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
            !player.hasMovementRoute()
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

    private data class ObjectApproachCheck(
        val result: InteractionExecutionResult?,
        val routeNs: Long,
    )
}
