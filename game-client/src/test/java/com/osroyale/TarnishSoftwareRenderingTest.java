package com.osroyale;

import org.junit.Assume;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TarnishSoftwareRenderingTest {

    @Test
    public void brightnessVarpBuildsAVisibleTwoThousandCoinSprite() throws Exception {
        Assume.assumeTrue(
                "Requires the shipped Tarnish binary cache under game-server/data/cache - skipped without it (e.g. in CI)",
                findShippedCache() != null);
        Path cache = shippedCache();
        StreamLoader config = configArchive(cache);
        Varp.unpackConfig(config);
        assertEquals("Tarnish brightness varp must retain renderer type 1", 1, Varp.varps[166].anInt709);

        ItemDefinition.unpackConfig(config);
        ItemDefinition.mruNodes1 = new Cache(100);
        ItemDefinition.mruNodes2 = new Cache(50);
        Model.init();

        ItemDefinition coins = ItemDefinition.lookup(995);
        int stackItem = stackItemFor(coins, 2_000);
        ItemDefinition stackDefinition = ItemDefinition.lookup(stackItem);
        Model.loadModel(readGzipArchive(cache, 1, stackDefinition.inventoryModel), stackDefinition.inventoryModel);

        Client previousClient = Client.instance;
        Client.instance = new Client();
        try {
            Rasterizer2D.initDrawingArea(new int[765 * 503], 765, 503);
            Rasterizer3D.useViewport();
            // Brightness varp value 3 maps to 0.7 in Client.method33.
            Rasterizer3D.Rasterizer3D_buildPalette(0.7D, 0, 512);

            Sprite sprite = ItemDefinition.getSprite(995, 2_000, 0);
            assertNotNull(sprite);
            assertTrue("2,000 coins must render visible software pixels", visiblePixelCount(sprite) > 0);
        } finally {
            Client.instance = previousClient;
        }
    }

    private static int stackItemFor(ItemDefinition definition, int amount) {
        int selected = -1;
        for (int index = 0; index < definition.stackIDs.length; index++) {
            if (definition.stackAmounts[index] != 0 && amount >= definition.stackAmounts[index]) {
                selected = definition.stackIDs[index];
            }
        }
        return selected;
    }

    private static long visiblePixelCount(Sprite sprite) {
        long count = 0;
        for (int pixel : sprite.raster) {
            if ((pixel & 0x00FFFFFF) != 0) {
                count++;
            }
        }
        return count;
    }

    private static StreamLoader configArchive(Path cache) throws Exception {
        try (RandomAccessFile data = new RandomAccessFile(cache.resolve("main_file_cache.dat").toFile(), "r");
             RandomAccessFile index = new RandomAccessFile(cache.resolve("main_file_cache.idx0").toFile(), "r")) {
            byte[] archive = new RSFileStore(data, index, 1).readFile(2);
            assertNotNull("shipped config archive", archive);
            return new StreamLoader(archive);
        }
    }

    private static byte[] readGzipArchive(Path cache, int indexId, int fileId) throws Exception {
        try (RandomAccessFile data = new RandomAccessFile(cache.resolve("main_file_cache.dat").toFile(), "r");
             RandomAccessFile index = new RandomAccessFile(cache.resolve("main_file_cache.idx" + indexId).toFile(), "r")) {
            byte[] compressed = new RSFileStore(data, index, indexId + 1).readFile(fileId);
            assertNotNull("shipped on-demand archive " + indexId + ":" + fileId, compressed);
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                return gzip.readAllBytes();
            }
        }
    }

    private static Path shippedCache() {
        Path found = findShippedCache();
        if (found == null) {
            throw new IllegalStateException("Unable to locate the shipped Tarnish cache");
        }
        return found;
    }

    private static Path findShippedCache() {
        Path[] candidates = {
                Path.of("game-server", "data", "cache"),
                Path.of("..", "game-server", "data", "cache"),
                Path.of("data", "cache")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve("main_file_cache.dat"))) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }
}
