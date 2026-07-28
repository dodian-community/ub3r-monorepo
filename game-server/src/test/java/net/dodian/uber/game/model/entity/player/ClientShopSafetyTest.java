package net.dodian.uber.game.model.entity.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientShopSafetyTest {

    @Test
    void sellAmountUsesLongArithmeticBeforeNarrowing() {
        assertEquals(
                Integer.MAX_VALUE / 2,
                Client.capShopSellAmountForCurrency(Integer.MAX_VALUE, 2, 0)
        );
        assertEquals(
                2,
                Client.capShopSellAmountForCurrency(100, 5, Integer.MAX_VALUE - 10)
        );
        assertEquals(
                100,
                Client.capShopSellAmountForCurrency(100, 0, Integer.MAX_VALUE)
        );
    }

    @Test
    void shopViewerSelectionHandlesMissingAndIneligibleRegistryEntries() {
        assertNull(Client.eligibleShopViewer(null, 12, 1));

        Client viewer = new Client(null, 2);
        assertNull(Client.eligibleShopViewer(viewer, 12, 1));

        viewer.MyShopID = 12;
        assertSame(viewer, Client.eligibleShopViewer(viewer, 12, 1));
        assertNull(Client.eligibleShopViewer(viewer, 12, 2));
        assertNull(Client.eligibleShopViewer(viewer, 13, 1));
    }
}
