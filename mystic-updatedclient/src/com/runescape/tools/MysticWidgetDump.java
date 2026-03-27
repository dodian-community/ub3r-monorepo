package com.runescape.tools;

import com.runescape.Client;
import com.runescape.cache.FileArchive;
import com.runescape.cache.FileStore;
import com.runescape.cache.graphics.GameFont;
import com.runescape.cache.graphics.Sprite;
import com.runescape.cache.graphics.Widget;
import com.runescape.io.jaggrab.JagGrab;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MysticWidgetDump {

    private static final String DEFAULT_CACHE_DIR = "Cache";
    private static final String DEFAULT_OUTPUT = "build/reports/widgets/mystic-widgets.jsonl";

    private MysticWidgetDump() {
    }

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        Widget[] widgets = loadWidgets(options.cacheDir);
        dumpWidgets(widgets, options);
    }

    private static Widget[] loadWidgets(Path cacheDir) throws IOException {
        Path dataPath = cacheDir.resolve("main_file_cache.dat");
        Path indexPath = cacheDir.resolve("main_file_cache.idx0");
        if (!Files.exists(dataPath) || !Files.exists(indexPath)) {
            throw new IOException("Missing cache files under " + cacheDir.toAbsolutePath());
        }

        try (RandomAccessFile data = new RandomAccessFile(dataPath.toFile(), "r");
             RandomAccessFile index = new RandomAccessFile(indexPath.toFile(), "r")) {
            FileStore archiveStore = new FileStore(data, index, 1);
            FileArchive titleArchive = readArchive(archiveStore, JagGrab.TITLE_CRC, "title");
            FileArchive interfaceArchive = readArchive(archiveStore, JagGrab.INTERFACE_CRC, "interface");
            FileArchive mediaArchive = readArchive(archiveStore, JagGrab.MEDIA_CRC, "media");
            installPlaceholderSprites();
            GameFont[] fonts = {
                    new GameFont(false, "p11_full", titleArchive),
                    new GameFont(false, "p12_full", titleArchive),
                    new GameFont(false, "b12_full", titleArchive),
                    new GameFont(true, "q8_full", titleArchive),
            };
            Widget.load(interfaceArchive, fonts, mediaArchive);
            return Widget.interfaceCache;
        }
    }

    private static void installPlaceholderSprites() {
        Sprite[] placeholders = new Sprite[2048];
        for (int i = 0; i < placeholders.length; i++) {
            placeholders[i] = new Sprite(1, 1);
        }
        Client.cacheSprite = placeholders;
        Client.cacheSprite3 = placeholders;
        Client.cacheSprite4 = placeholders;
    }

    private static FileArchive readArchive(FileStore archiveStore, int id, String name) throws IOException {
        byte[] data = archiveStore.decompress(id);
        if (data == null) {
            throw new IOException("Unable to read " + name + " archive from cache");
        }
        return new FileArchive(data);
    }

    private static void dumpWidgets(Widget[] widgets, Options options) throws IOException {
        Files.createDirectories(options.output.getParent());
        Map<Integer, Integer> childIndexes = buildChildIndexLookup(widgets);
        List<DumpRow> rows = new ArrayList<>();
        for (Widget widget : widgets) {
            if (widget == null || !isInteractive(widget)) {
                continue;
            }
            DumpRow row = DumpRow.from(widget, childIndexes.get(widget.id));
            if (!row.matches(options)) {
                continue;
            }
            rows.add(row);
        }

        rows.sort(Comparator
                .comparingInt((DumpRow row) -> row.parent)
                .thenComparingInt(row -> row.childIndex == null ? Integer.MAX_VALUE : row.childIndex)
                .thenComparingInt(row -> row.id));

        try (BufferedWriter writer = Files.newBufferedWriter(options.output, StandardCharsets.UTF_8)) {
            for (DumpRow row : rows) {
                writer.write(row.toJson());
                writer.newLine();
            }
        }

        System.out.println("Wrote " + rows.size() + " widgets to " + options.output.toAbsolutePath());
    }

    private static Map<Integer, Integer> buildChildIndexLookup(Widget[] widgets) {
        Map<Integer, Integer> childIndexes = new HashMap<>();
        for (Widget widget : widgets) {
            if (widget == null || widget.children == null) {
                continue;
            }
            for (int i = 0; i < widget.children.length; i++) {
                childIndexes.putIfAbsent(widget.children[i], i);
            }
        }
        return childIndexes;
    }

    private static boolean isInteractive(Widget widget) {
        return widget.atActionType != 0
                || hasValue(widget.tooltip)
                || hasValue(widget.defaultText)
                || hasValue(widget.selectedActionName)
                || hasValue(widget.spellName)
                || hasAnyValue(widget.actions);
    }

    private static boolean hasAnyValue(String[] values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (hasValue(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final class Options {
        private final Path cacheDir;
        private final Path output;
        private final Set<Integer> parents;
        private final List<String> textTerms;

        private Options(Path cacheDir, Path output, Set<Integer> parents, List<String> textTerms) {
            this.cacheDir = cacheDir;
            this.output = output;
            this.parents = parents;
            this.textTerms = textTerms;
        }

        private static Options parse(String[] args) {
            Path cacheDir = Paths.get(DEFAULT_CACHE_DIR);
            Path output = Paths.get(DEFAULT_OUTPUT);
            Set<Integer> parents = new HashSet<>();
            List<String> textTerms = new ArrayList<>();

            for (String arg : args) {
                if (arg.startsWith("--cache-dir=")) {
                    cacheDir = Paths.get(arg.substring("--cache-dir=".length()));
                } else if (arg.startsWith("--output=")) {
                    output = Paths.get(arg.substring("--output=".length()));
                } else if (arg.startsWith("--parent=")) {
                    for (String value : arg.substring("--parent=".length()).split(",")) {
                        if (!value.trim().isEmpty()) {
                            parents.add(Integer.parseInt(value.trim()));
                        }
                    }
                } else if (arg.startsWith("--text=")) {
                    for (String value : arg.substring("--text=".length()).split(",")) {
                        String trimmed = value.trim().toLowerCase(Locale.ROOT);
                        if (!trimmed.isEmpty()) {
                            textTerms.add(trimmed);
                        }
                    }
                }
            }

            return new Options(cacheDir, output, parents, textTerms);
        }
    }

    private static final class DumpRow {
        private final int id;
        private final int parent;
        private final Integer childIndex;
        private final int type;
        private final int atActionType;
        private final int contentType;
        private final String tooltip;
        private final String defaultText;
        private final String[] actions;
        private final String selectedActionName;
        private final String spellName;

        private DumpRow(
                int id,
                int parent,
                Integer childIndex,
                int type,
                int atActionType,
                int contentType,
                String tooltip,
                String defaultText,
                String[] actions,
                String selectedActionName,
                String spellName
        ) {
            this.id = id;
            this.parent = parent;
            this.childIndex = childIndex;
            this.type = type;
            this.atActionType = atActionType;
            this.contentType = contentType;
            this.tooltip = tooltip;
            this.defaultText = defaultText;
            this.actions = actions;
            this.selectedActionName = selectedActionName;
            this.spellName = spellName;
        }

        private static DumpRow from(Widget widget, Integer childIndex) {
            return new DumpRow(
                    widget.id,
                    widget.parent,
                    childIndex,
                    widget.type,
                    widget.atActionType,
                    widget.contentType,
                    widget.tooltip,
                    widget.defaultText,
                    widget.actions == null ? null : Arrays.copyOf(widget.actions, widget.actions.length),
                    widget.selectedActionName,
                    widget.spellName
            );
        }

        private boolean matches(Options options) {
            if (!options.parents.isEmpty() && !options.parents.contains(parent) && !options.parents.contains(id)) {
                return false;
            }
            if (options.textTerms.isEmpty()) {
                return true;
            }
            String haystack = String.join("\n",
                    normalize(tooltip),
                    normalize(defaultText),
                    normalize(selectedActionName),
                    normalize(spellName),
                    normalize(actions == null ? null : String.join("\n", actions)));
            for (String term : options.textTerms) {
                if (haystack.contains(term)) {
                    return true;
                }
            }
            return false;
        }

        private static String normalize(String value) {
            return value == null ? "" : value.toLowerCase(Locale.ROOT);
        }

        private String toJson() {
            StringBuilder builder = new StringBuilder(256);
            builder.append('{');
            append(builder, "id", id).append(',');
            append(builder, "parent", parent).append(',');
            if (childIndex == null) {
                appendNull(builder, "childIndex").append(',');
            } else {
                append(builder, "childIndex", childIndex).append(',');
            }
            append(builder, "type", type).append(',');
            append(builder, "atActionType", atActionType).append(',');
            append(builder, "contentType", contentType).append(',');
            append(builder, "tooltip", tooltip).append(',');
            append(builder, "defaultText", defaultText).append(',');
            appendArray(builder, "actions", actions).append(',');
            append(builder, "selectedActionName", selectedActionName).append(',');
            append(builder, "spellName", spellName);
            builder.append('}');
            return builder.toString();
        }

        private static StringBuilder append(StringBuilder builder, String name, Integer value) {
            builder.append('"').append(name).append('"').append(':').append(value);
            return builder;
        }

        private static StringBuilder append(StringBuilder builder, String name, String value) {
            builder.append('"').append(name).append('"').append(':');
            if (value == null) {
                builder.append("null");
            } else {
                builder.append('"').append(escape(value)).append('"');
            }
            return builder;
        }

        private static StringBuilder appendNull(StringBuilder builder, String name) {
            builder.append('"').append(name).append('"').append(':').append("null");
            return builder;
        }

        private static StringBuilder appendArray(StringBuilder builder, String name, String[] values) {
            builder.append('"').append(name).append('"').append(':');
            if (values == null) {
                builder.append("null");
                return builder;
            }
            builder.append('[');
            boolean first = true;
            for (String value : values) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                if (value == null) {
                    builder.append("null");
                } else {
                    builder.append('"').append(escape(value)).append('"');
                }
            }
            builder.append(']');
            return builder;
        }

        private static String escape(String value) {
            StringBuilder escaped = new StringBuilder(value.length() + 16);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '\\':
                    case '"':
                        escaped.append('\\').append(c);
                        break;
                    case '\n':
                        escaped.append("\\n");
                        break;
                    case '\r':
                        escaped.append("\\r");
                        break;
                    case '\t':
                        escaped.append("\\t");
                        break;
                    default:
                        escaped.append(c);
                        break;
                }
            }
            return escaped.toString();
        }
    }
}
