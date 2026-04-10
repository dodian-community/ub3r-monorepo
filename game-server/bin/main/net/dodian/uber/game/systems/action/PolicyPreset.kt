package net.dodian.uber.game.systems.action

import net.dodian.uber.game.systems.action.PlayerActionInterruptPolicy
import net.dodian.uber.game.systems.interaction.ObjectInteractionPolicy

enum class PolicyQueueMode {
    ALWAYS,
    NEVER,
}

enum class PolicyWalkability {
    WALKABLE,
    NON_WALKABLE,
    FOLLOW,
}

data class PolicySettle(
    val requireMovementSettled: Boolean,
    val settleTicks: Int,
)

data class PolicyCancel(
    val cancelOnMovement: Boolean,
    val cancelOnCombatEntry: Boolean,
    val cancelOnDialogueOpen: Boolean,
    val cancelOnInterfaceClose: Boolean,
    val cancelOnDisconnect: Boolean,
    val cancelOnLogout: Boolean,
    val cancelOnDeath: Boolean,
    val cancelOnTeleport: Boolean,
)

data class UnifiedPolicySpec(
    val preset: PolicyPreset,
    val queue: PolicyQueueMode,
    val walkability: PolicyWalkability,
    val settle: PolicySettle,
    val cancel: PolicyCancel,
    val distanceRule: ObjectInteractionPolicy.DistanceRule,
)

enum class PolicyPreset {
    GATHERING,
    PRODUCTION,
    DIALOGUE,
    MOVEMENT_LOCKED,
}

object UnifiedPolicyDsl {
    private val specs: Map<PolicyPreset, UnifiedPolicySpec> = mapOf(
        PolicyPreset.GATHERING to UnifiedPolicySpec(
            preset = PolicyPreset.GATHERING,
            queue = PolicyQueueMode.NEVER,
            walkability = PolicyWalkability.NON_WALKABLE,
            settle = PolicySettle(requireMovementSettled = true, settleTicks = 1),
            cancel = PolicyCancel(
                cancelOnMovement = true,
                cancelOnCombatEntry = true,
                cancelOnDialogueOpen = true,
                cancelOnInterfaceClose = true,
                cancelOnDisconnect = true,
                cancelOnLogout = true,
                cancelOnDeath = true,
                cancelOnTeleport = true,
            ),
            distanceRule = ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_CARDINAL,
        ),
        PolicyPreset.PRODUCTION to UnifiedPolicySpec(
            preset = PolicyPreset.PRODUCTION,
            queue = PolicyQueueMode.NEVER,
            walkability = PolicyWalkability.NON_WALKABLE,
            settle = PolicySettle(requireMovementSettled = true, settleTicks = 1),
            cancel = PolicyCancel(
                cancelOnMovement = true,
                cancelOnCombatEntry = true,
                cancelOnDialogueOpen = true,
                cancelOnInterfaceClose = true,
                cancelOnDisconnect = true,
                cancelOnLogout = true,
                cancelOnDeath = true,
                cancelOnTeleport = true,
            ),
            distanceRule = ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_CARDINAL,
        ),
        PolicyPreset.DIALOGUE to UnifiedPolicySpec(
            preset = PolicyPreset.DIALOGUE,
            queue = PolicyQueueMode.ALWAYS,
            walkability = PolicyWalkability.NON_WALKABLE,
            settle = PolicySettle(requireMovementSettled = false, settleTicks = 0),
            cancel = PolicyCancel(
                cancelOnMovement = false,
                cancelOnCombatEntry = true,
                cancelOnDialogueOpen = false,
                cancelOnInterfaceClose = false,
                cancelOnDisconnect = true,
                cancelOnLogout = true,
                cancelOnDeath = true,
                cancelOnTeleport = true,
            ),
            distanceRule = ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_ANY,
        ),
        PolicyPreset.MOVEMENT_LOCKED to UnifiedPolicySpec(
            preset = PolicyPreset.MOVEMENT_LOCKED,
            queue = PolicyQueueMode.NEVER,
            walkability = PolicyWalkability.NON_WALKABLE,
            settle = PolicySettle(requireMovementSettled = true, settleTicks = 2),
            cancel = PolicyCancel(
                cancelOnMovement = true,
                cancelOnCombatEntry = true,
                cancelOnDialogueOpen = true,
                cancelOnInterfaceClose = true,
                cancelOnDisconnect = true,
                cancelOnLogout = true,
                cancelOnDeath = true,
                cancelOnTeleport = true,
            ),
            distanceRule = ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_CARDINAL,
        ),
    )

    @JvmStatic
    fun specFor(preset: PolicyPreset): UnifiedPolicySpec = specs.getValue(preset)

    @JvmStatic
    fun toObjectPolicy(preset: PolicyPreset): ObjectInteractionPolicy {
        val spec = specFor(preset)
        return ObjectInteractionPolicy(
            distanceRule = spec.distanceRule,
            requireMovementSettled = spec.settle.requireMovementSettled,
            settleTicks = spec.settle.settleTicks,
        )
    }

    @JvmStatic
    fun toActionInterruptPolicy(preset: PolicyPreset): PlayerActionInterruptPolicy {
        val cancel = specFor(preset).cancel
        return PlayerActionInterruptPolicy(
            cancelOnMovement = cancel.cancelOnMovement,
            cancelOnCombatEntry = cancel.cancelOnCombatEntry,
            cancelOnDialogueOpen = cancel.cancelOnDialogueOpen,
            cancelOnInterfaceClose = cancel.cancelOnInterfaceClose,
            cancelOnDisconnect = cancel.cancelOnDisconnect,
            cancelOnLogout = cancel.cancelOnLogout,
            cancelOnDeath = cancel.cancelOnDeath,
            cancelOnTeleport = cancel.cancelOnTeleport,
        )
    }
}

