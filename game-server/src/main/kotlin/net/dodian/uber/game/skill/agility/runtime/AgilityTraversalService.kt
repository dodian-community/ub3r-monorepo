package net.dodian.uber.game.skill.agility.runtime

import net.dodian.uber.game.skill.runtime.SkillActionContext
import net.dodian.uber.game.skill.runtime.SkillTraversalMovementMode
import net.dodian.uber.game.skill.runtime.SkillTraversalPlan
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.api.content.ContentTiming
import net.dodian.uber.game.engine.systems.interaction.PersonalPassageService

object AgilityTraversalService {
    private const val PASSAGE_GRANT_BUFFER_MS = 1_200L

    @JvmStatic
    fun execute(
        context: SkillActionContext,
        plan: SkillTraversalPlan,
    ): Boolean {
        if (!plan.guard.tryBegin(context)) {
            return false
        }
        if (!passesRequirements(context, plan)) {
            finish(context, plan, completed = false)
            return false
        }
        if (!plan.preStep(context)) {
            finish(context, plan, completed = false)
            return false
        }

        val player = context.player
        val movement = plan.movement
        plan.onStart(context)

        val passageEdges = plan.passageEdges(context)
        if (passageEdges.isNotEmpty()) {
            PersonalPassageService.grantBidirectionalEdges(
                player = player,
                edges = passageEdges,
                durationMs = movement.durationMs.toLong() + PASSAGE_GRANT_BUFFER_MS,
            )
        }

        if (movement.movementAnimationId != null) {
            player.walkAnim = movement.movementAnimationId
        }
        when (movement.mode) {
            SkillTraversalMovementMode.RUN -> player.AddToRunCords(movement.deltaX, movement.deltaY, movement.durationMs.toLong())
            SkillTraversalMovementMode.WALK -> player.AddToWalkCords(movement.deltaX, movement.deltaY, movement.durationMs.toLong())
        }

        ContentTiming.runLaterMs(movement.durationMs) {
            if (player.disconnected || (plan.guard.lockMovement && !player.isMovementLocked)) {
                finish(context, plan, completed = false)
                return@runLaterMs
            }
            if (movement.restoreWeaponAnimations && movement.movementAnimationId != null) {
                player.requestWeaponAnims()
            }
            finish(context, plan, completed = true)
        }
        return true
    }

    @JvmStatic
    fun cancel(playerContext: SkillActionContext, plan: SkillTraversalPlan) {
        finish(playerContext, plan, completed = false)
    }

    private fun passesRequirements(
        context: SkillActionContext,
        plan: SkillTraversalPlan,
    ): Boolean {
        val requiredLevel = plan.requiredLevel ?: return true
        val player = context.player
        if (player.getLevel(Skill.AGILITY) >= requiredLevel) {
            return true
        }
        player.sendMessage("You need level $requiredLevel agility to use this!")
        return false
    }

    private fun finish(
        context: SkillActionContext,
        plan: SkillTraversalPlan,
        completed: Boolean,
    ) {
        PersonalPassageService.clearForPlayer(context.player)
        if (completed) {
            plan.onComplete(context)
        } else {
            plan.onCancel(context)
        }
        plan.guard.finish(context)
    }

    @JvmStatic
    fun straightPathEdges(
        start: Position,
        deltaX: Int,
        deltaY: Int,
    ): List<Pair<Position, Position>> {
        val stepX = deltaX.coerceIn(-1, 1)
        val stepY = deltaY.coerceIn(-1, 1)
        if ((stepX != 0 && stepY != 0) || (stepX == 0 && stepY == 0)) {
            return emptyList()
        }
        val steps = kotlin.math.max(kotlin.math.abs(deltaX), kotlin.math.abs(deltaY))
        if (steps <= 0) {
            return emptyList()
        }
        val edges = ArrayList<Pair<Position, Position>>(steps)
        var from = start.copy()
        repeat(steps) {
            val to = Position(from.x + stepX, from.y + stepY, from.z)
            edges += from to to
            from = to
        }
        return edges
    }
}
