package net.dodian.uber.social.trading

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TradingModuleTest {
    @Test fun `declares offer and withdrawal ownership`() {
        assertTrue(TradingModule.contentManifest.declaredRouteKeys.any { it.contains("item_withdraw:3415") })
    }
}
