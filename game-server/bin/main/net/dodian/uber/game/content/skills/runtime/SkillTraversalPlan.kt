package net.dodian.uber.game.content.skills.runtime

import net.dodian.uber.game.model.Position

enum class SkillTraversalMovementMode {
    WALK,
    RUN,
}

data class SkillTraversalMovement(
    val deltaX: Int,
    val deltaY: Int,
    val durationMs: Int,
    val mode: SkillTraversalMovementMode = SkillTraversalMovementMode.WALK,
    val movementAnimationId: Int? = null,
    val restoreWeaponAnimations: Boolean = true,
)

data class SkillTraversalPlan(
    val name: String,
    val requiredLevel: Int? = null,
    val movement: SkillTraversalMovement,
    val guard: SkillExecutionGuard = SkillExecutionGuard.agilityDefault(),
    val preStep: (SkillActionContext) -> Boolean = { true },
    val onStart: (SkillActionContext) -> Unit = {},
    val onComplete: (SkillActionContext) -> Unit = {},
    val onCancel: (SkillActionContext) -> Unit = {},
    val passageEdges: (SkillActionContext) -> List<Pair<Position, Position>> = { emptyList() },
)
