package net.dodian.uber.game.systems.action

import net.dodian.uber.game.systems.policy.PolicyPreset
import net.dodian.uber.game.systems.policy.UnifiedPolicyDsl

data class PlayerActionInterruptPolicy(
    val cancelOnMovement: Boolean = false,
    val cancelOnCombatEntry: Boolean = true,
    val cancelOnDialogueOpen: Boolean = true,
    val cancelOnInterfaceClose: Boolean = true,
    val cancelOnDisconnect: Boolean = true,
    val cancelOnLogout: Boolean = true,
    val cancelOnDeath: Boolean = true,
    val cancelOnTeleport: Boolean = true,
) {
    companion object {
        @JvmField
        val DEFAULT = UnifiedPolicyDsl.toActionInterruptPolicy(PolicyPreset.PRODUCTION)

        @JvmField
        val TELEPORT =
            PlayerActionInterruptPolicy(
                cancelOnMovement = false,
                cancelOnCombatEntry = true,
                cancelOnDialogueOpen = false,
                cancelOnInterfaceClose = false,
                cancelOnDisconnect = true,
                cancelOnLogout = true,
                cancelOnDeath = true,
                cancelOnTeleport = false,
            )

        @JvmStatic
        fun fromPreset(preset: PolicyPreset): PlayerActionInterruptPolicy = UnifiedPolicyDsl.toActionInterruptPolicy(preset)
    }
}
