package net.dodian.cache.tools;

import net.dodian.cache.Archive;
import net.dodian.cache.Cache;
import net.dodian.cache.index.impl.MapIndex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Exports cache-backed world data into the legacy server-side format used by clipping loaders.
 */
public final class CacheWorldExporter {

    private static final int VERSION_LIST_ARCHIVE = 5;
    private static final int CONFIG_ARCHIVE = 2;
    private static final int MAP_CACHE_INDEX = 4;

    private CacheWorldExporter() {
    }

    public static void main(String[] args) throws Exception {
        File cacheDir = new File(args.length > 0 ? args[0] : "./data/cache");
        File worldDir = new File(args.length > 1 ? args[1] : "./data/world");
        export(cacheDir, worldDir);
    }

    public static void export(File cacheDir, File worldDir) throws Exception {
        File mapDir = new File(worldDir, "map");
        File objectDir = new File(worldDir, "object");
        createDirectory(worldDir.toPath());
        createDirectory(mapDir.toPath());
        createDirectory(objectDir.toPath());

        try (Cache cache = new Cache(cacheDir)) {
            writeMapIndex(cache, worldDir.toPath());
            writeObjectDefinitions(cache, objectDir.toPath());
            int exported = writeMapFiles(cache, mapDir.toPath());
            System.out.println("[CacheWorldExporter] Export complete: " + exported + " map files.");
        }
    }

    private static void writeMapIndex(Cache cache, Path worldDir) throws IOException {
        Archive versionListArchive = new Archive(cache.getFile(0, VERSION_LIST_ARCHIVE));
        byte[] mapIndex = versionListArchive.getFile("map_index");
        if (mapIndex == null || mapIndex.length == 0) {
            throw new IOException("map_index was not found in version list archive.");
        }
        writeFile(worldDir.resolve("map_index"), mapIndex);
        System.out.println("[CacheWorldExporter] Wrote map_index (" + mapIndex.length + " bytes).");
    }

    private static void writeObjectDefinitions(Cache cache, Path objectDir) throws IOException {
        Archive configArchive = new Archive(cache.getFile(0, CONFIG_ARCHIVE));
        byte[] locDat = configArchive.getFile("loc.dat");
        byte[] locIdx = configArchive.getFile("loc.idx");
        if (locDat == null || locIdx == null) {
            throw new IOException("loc.dat/loc.idx missing from config archive.");
        }
        writeFile(objectDir.resolve("loc.dat"), locDat);
        writeFile(objectDir.resolve("loc.idx"), locIdx);
        System.out.println("[CacheWorldExporter] Wrote object definitions: loc.dat, loc.idx.");
    }

    private static int writeMapFiles(Cache cache, Path mapDir) throws IOException {
        Set<Integer> fileIds = new LinkedHashSet<>();
        for (MapIndex index : cache.getIndexTable().getMapIndices()) {
            addValidFileId(fileIds, index.getMapFile());
            addValidFileId(fileIds, index.getLandscapeFile());
        }

        int maxFileId = cache.getFileCount(MAP_CACHE_INDEX);
        int exported = 0;
        int skippedOutOfRange = 0;
        for (Integer fileId : fileIds) {
            if (fileId > maxFileId) {
                skippedOutOfRange++;
                continue;
            }
            try {
                byte[] bytes = cache.getFile(MAP_CACHE_INDEX, fileId).getBytes();
                writeFile(mapDir.resolve(fileId + ".gz"), bytes);
                exported++;
            } catch (Exception e) {
                System.err.println("[CacheWorldExporter] Failed map file " + fileId + ": " + e.getMessage());
            }
        }
        if (skippedOutOfRange > 0) {
            System.out.println("[CacheWorldExporter] Skipped out-of-range map ids: " + skippedOutOfRange);
        }
        return exported;
    }

    private static void addValidFileId(Set<Integer> fileIds, int fileId) {
        if (fileId >= 0 && fileId != 65535) {
            fileIds.add(fileId);
        }
    }

    private static void writeFile(Path path, byte[] bytes) throws IOException {
        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static void createDirectory(Path path) throws IOException {
        Files.createDirectories(path);
    }
}
