package com.osroyale;

import com.osroyale.profile.Profile;
import com.osroyale.profile.ProfileManager;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ProfileImageTest {

    @Test
    public void profileSpriteLoadsSynchronouslyAndRejectsMalformedOrWrongSizedPngs() throws Exception {
        Path directory = Files.createTempDirectory("tarnish-profile-image");
        Path valid = directory.resolve("valid.png");
        ProfileManager.saveImage(image(50, 50, 0xFF336699), valid.toString());

        Sprite sprite = new Sprite(valid.toString(), 50, 50);
        assertNotNull(sprite.raster);
        assertEquals(2_500, sprite.raster.length);

        Path malformed = directory.resolve("malformed.png");
        Files.write(malformed, new byte[]{(byte) 0x89, 'P', 'N', 'G'});
        assertNull(new Sprite(malformed.toString(), 50, 50).raster);

        Path wrongSize = directory.resolve("wrong-size.png");
        ImageIO.write(image(10, 10, 0xFFFFFFFF), "png", wrongSize.toFile());
        assertNull(new Sprite(wrongSize.toString(), 50, 50).raster);
    }

    @Test
    public void atomicProfileReplacementNeverExposesAPartialPng() throws Exception {
        Path directory = Files.createTempDirectory("tarnish-profile-race");
        Path profile = directory.resolve("profile.png");
        ProfileManager.saveImage(image(50, 50, 0xFF112233), profile.toString());

        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        threads.add(new Thread(() -> runSafely(failure, () -> {
            start.await();
            for (int iteration = 0; iteration < 100; iteration++) {
                ProfileManager.saveImage(image(50, 50, 0xFF000000 | iteration), profile.toString());
            }
        }), "profile-writer"));
        threads.add(new Thread(() -> runSafely(failure, () -> {
            start.await();
            for (int iteration = 0; iteration < 500; iteration++) {
                Sprite sprite = new Sprite(profile.toString(), 50, 50);
                assertNotNull("atomic replacement exposed an invalid PNG", sprite.raster);
                assertEquals(2_500, sprite.raster.length);
            }
        }), "profile-reader"));

        threads.forEach(Thread::start);
        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        if (failure.get() != null) {
            throw new AssertionError("concurrent profile image test failed", failure.get());
        }
        assertNotNull(ImageIO.read(profile.toFile()));
        try (var files = Files.list(directory)) {
            assertFalse("temporary PNG files must be cleaned up",
                    files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    public void profileRasterConversionKeepsTheBackgroundTransparentAndCharacterPixelsOpaque() {
        BufferedImage image = ProfileManager.createImageFromPixels(
                new int[]{0x00000000, 0x00123456, 0x00ABCDEF, 0x00000000}, 2, 2);

        assertEquals(0x00000000, image.getRGB(0, 0));
        assertEquals(0xFF123456, image.getRGB(1, 0));
        assertEquals(0xFFABCDEF, image.getRGB(0, 1));
        assertEquals(0x00000000, image.getRGB(1, 1));
    }

    @Test
    public void bodyPreviewRestoresTheLiveRasterStateWhenModelRenderingFails() {
        int[] originalPixels = Rasterizer2D.pixels;
        int originalWidth = Rasterizer2D.width;
        int originalHeight = Rasterizer2D.height;
        int originalOriginX = Rasterizer3D.originViewX;
        int originalOriginY = Rasterizer3D.originViewY;
        int[] originalScanOffsets = Rasterizer3D.scanOffsets;
        boolean originalRestrictEdges = Rasterizer3D.aBoolean1464;
        boolean originalWorld = Rasterizer3D.world;
        boolean originalRenderOnGpu = Rasterizer3D.renderOnGpu;

        int[] expectedPixels = new int[35];
        int[] expectedScanOffsets = new int[]{1, 2, 3};
        try {
            Rasterizer2D.initDrawingArea(expectedPixels, 7, 5);
            Rasterizer3D.originViewX = 91;
            Rasterizer3D.originViewY = 73;
            Rasterizer3D.scanOffsets = expectedScanOffsets;
            Rasterizer3D.aBoolean1464 = true;
            Rasterizer3D.world = true;
            Rasterizer3D.renderOnGpu = false;

            try {
                new Profile().convertBodyModelToSprite(new FailingModel());
                throw new AssertionError("Expected model rendering to fail");
            } catch (ExpectedRenderFailure ignored) {
                // The renderer must restore the client canvas in its finally block.
            }

            assertSame(expectedPixels, Rasterizer2D.pixels);
            assertEquals(7, Rasterizer2D.width);
            assertEquals(5, Rasterizer2D.height);
            assertEquals(91, Rasterizer3D.originViewX);
            assertEquals(73, Rasterizer3D.originViewY);
            assertSame(expectedScanOffsets, Rasterizer3D.scanOffsets);
            assertTrue(Rasterizer3D.aBoolean1464);
            assertTrue(Rasterizer3D.world);
            assertFalse(Rasterizer3D.renderOnGpu);
        } finally {
            Rasterizer2D.pixels = originalPixels;
            Rasterizer2D.width = originalWidth;
            Rasterizer2D.height = originalHeight;
            Rasterizer3D.originViewX = originalOriginX;
            Rasterizer3D.originViewY = originalOriginY;
            Rasterizer3D.scanOffsets = originalScanOffsets;
            Rasterizer3D.aBoolean1464 = originalRestrictEdges;
            Rasterizer3D.world = originalWorld;
            Rasterizer3D.renderOnGpu = originalRenderOnGpu;
        }
    }

    private static BufferedImage image(int width, int height, int color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    private static void runSafely(AtomicReference<Throwable> failure, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            failure.compareAndSet(null, throwable);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class FailingModel extends Model {
        @Override
        public void renderModel(int rotationY, int rotationZ, int rotationXW,
                                int translationX, int translationY, int translationZ) {
            throw new ExpectedRenderFailure();
        }
    }

    private static final class ExpectedRenderFailure extends RuntimeException {
    }
}
