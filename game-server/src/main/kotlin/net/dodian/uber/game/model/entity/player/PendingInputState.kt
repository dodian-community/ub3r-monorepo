package net.dodian.uber.game.model.entity.player

/**
 * The single owner of a player's next text-input packet.  A text entry cannot
 * safely satisfy more than one UI flow, so replacing the previous request is
 * deliberate and makes interface-close/logout cleanup deterministic.
 */
enum class PendingInputState {
    NONE,
    BANK_SEARCH,
    MODERATION_SEARCH,
    PRICE_CHECKER_SEARCH,
    ADD_FRIEND,
    REMOVE_FRIEND,
    ADD_IGNORE,
    REMOVE_IGNORE,
}
