package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.economy.ShopTransactionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientShopSafetyTest {

    @Test
    void sellAmountUsesLongArithmeticBeforeNarrowing() {
        assertEquals(
                Integer.MAX_VALUE / 2,
                ShopTransactionService.capSellAmountForCurrency(Integer.MAX_VALUE, 2, 0)
        );
        assertEquals(
                2,
                ShopTransactionService.capSellAmountForCurrency(100, 5, Integer.MAX_VALUE - 10)
        );
        assertEquals(
                100,
                ShopTransactionService.capSellAmountForCurrency(100, 0, Integer.MAX_VALUE)
        );
    }

    @Test
    void buyAmountUsesLongArithmeticBeforeNarrowing() {
        // A shop slot with stock large enough that requestedAmount * unitPrice would overflow
        // a raw int (this is exactly the scenario an accumulated shop stock can reach): with
        // the old "amount * TotPrice2 > coins" check, this would wrap to a negative product and
        // silently let the full (unaffordable) amount through instead of clamping it.
        int unitPrice = 1_000;
        int hugeRequestedAmount = Integer.MAX_VALUE / 10; // huge * unitPrice overflows int
        int coins = 50_000; // can only actually afford 50 units
        assertEquals(
                50,
                ShopTransactionService.capBuyAmountForCurrency(hugeRequestedAmount, unitPrice, coins)
        );

        // Ordinary case: affordable amount is the binding constraint.
        assertEquals(
                2,
                ShopTransactionService.capBuyAmountForCurrency(100, 5, 12)
        );

        // Free item (unitPrice <= 0): request passes through unclamped by affordability.
        assertEquals(
                100,
                ShopTransactionService.capBuyAmountForCurrency(100, 0, 0)
        );

        // Nothing requested, or nothing affordable: zero.
        assertEquals(0, ShopTransactionService.capBuyAmountForCurrency(100, 5, 0));
        assertEquals(0, ShopTransactionService.capBuyAmountForCurrency(0, 5, 100));
    }

    @Test
    void shopViewerSelectionHandlesMissingAndIneligibleRegistryEntries() {
        assertNull(ShopTransactionService.eligibleViewer(null, 12, 1));

        Client viewer = new Client(null, 2);
        assertNull(ShopTransactionService.eligibleViewer(viewer, 12, 1));

        viewer.MyShopID = 12;
        assertSame(viewer, ShopTransactionService.eligibleViewer(viewer, 12, 1));
        assertNull(ShopTransactionService.eligibleViewer(viewer, 12, 2));
        assertNull(ShopTransactionService.eligibleViewer(viewer, 13, 1));
    }
}
