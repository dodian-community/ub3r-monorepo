package net.dodian.uber.game.api.plugin.social.runtime

object ExchangeAmount {
    /**
     * Resolves every 1/5/10/all/X request against the authoritative container amount.
     * Invalid client values are rejected rather than wrapped or silently made positive.
     */
    fun resolve(requested: Int, available: Int): Int? {
        if (requested <= 0 || available <= 0) return null
        return requested.coerceAtMost(available)
    }
}
