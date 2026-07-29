package net.runelite.client.plugins.accounts;

import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class AccountsPanelPreviewTest {

    @Test
    public void legacyPreviewRgbPixelsBecomeOpaqueWithoutChangingTheirColour() {
        BufferedImage legacy = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        legacy.setRGB(0, 0, 0x00000000);
        legacy.setRGB(1, 0, 0x00335577);

        BufferedImage repaired = AccountsPanel.normalizeLegacyPreview(legacy);

        assertEquals(0x00000000, repaired.getRGB(0, 0));
        assertEquals(0xFF335577, repaired.getRGB(1, 0));
    }

    @Test
    public void validPreviewDoesNotNeedAnImageCopy() {
        BufferedImage preview = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        preview.setRGB(0, 0, 0xFF123456);

        assertSame(preview, AccountsPanel.normalizeLegacyPreview(preview));
    }
}
