package net.dodian.uber.skills.smithing

/** The selected anvil and bar tier for an in-progress Smithing interface/action. */
data class ActiveSmithingSelection(
    val tierId: Int,
    val barId: Int,
    val anvilX: Int,
    val anvilY: Int,
)
