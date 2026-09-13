package com.jsirgalaxybase.quest.bqimport;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jsirgalaxybase.quest.core.QuestChapterDefinition;
import com.jsirgalaxybase.quest.core.QuestChapterEntry;
import com.jsirgalaxybase.quest.core.QuestVisibility;

/** Reads GTNH BetterQuesting's split DefaultQuests/QuestLines export without loading the mod. */
public final class BqQuestChapterImporter {
    private final Gson gson = new Gson();

    public List<BqImportedChapter> readDirectory(Path defaultQuests) throws IOException {
        Path lines = defaultQuests.resolve("QuestLines");
        Path orderFile = defaultQuests.resolve("QuestLinesOrder.txt");
        if (!Files.isDirectory(lines)) throw new IOException("Missing BetterQuesting QuestLines directory: " + lines);
        if (!Files.isRegularFile(orderFile)) throw new IOException("Missing BetterQuesting QuestLinesOrder.txt: " + orderFile);

        Map<UUID, OrderedName> order = readOrder(orderFile);
        List<Path> directories = new ArrayList<Path>();
        try (java.util.stream.Stream<Path> stream = Files.list(lines)) {
            stream.filter(Files::isDirectory).forEach(directories::add);
        }
        Collections.sort(directories);
        List<BqImportedChapter> result = new ArrayList<BqImportedChapter>();
        for (Path directory : directories) {
            UUID chapterId = idFromFileName(directory.getFileName().toString());
            OrderedName metadata = order.get(chapterId);
            ChapterMetadata properties = readMetadata(directory.resolve("QuestLine.json"), chapterId);
            String name = properties.name.isEmpty()
                ? (metadata == null ? directory.getFileName().toString() : metadata.name) : properties.name;
            int displayOrder = metadata == null ? Integer.MAX_VALUE : metadata.order;
            result.add(new BqImportedChapter(new QuestChapterDefinition(chapterId, 1, name, properties.description,
                properties.iconReference, properties.backgroundReference, properties.backgroundSize,
                properties.visibility, readEntries(directory)), directory,
                displayOrder));
        }
        Collections.sort(result, Comparator.comparingInt(BqImportedChapter::getDisplayOrder)
            .thenComparing(value -> value.getDefinition().getId().toString()));
        return result;
    }

    private List<QuestChapterEntry> readEntries(Path directory) throws IOException {
        List<Path> files = new ArrayList<Path>();
        try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
            stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                .filter(path -> !"QuestLine.json".equals(path.getFileName().toString()))
                .forEach(files::add);
        }
        Collections.sort(files);
        List<QuestChapterEntry> entries = new ArrayList<QuestChapterEntry>();
        for (Path file : files) {
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                root = gson.fromJson(reader, JsonObject.class);
            }
            entries.add(new QuestChapterEntry(BqQuestDefinitionImporter.uuid(root, "questIDHigh", "questIDLow"),
                (int) BqTypedJson.number(root, "x", 0L), (int) BqTypedJson.number(root, "y", 0L),
                positiveDimension(root, "sizeX", file), positiveDimension(root, "sizeY", file)));
        }
        return entries;
    }

    private ChapterMetadata readMetadata(Path source, UUID directoryId) throws IOException {
        if (!Files.isRegularFile(source)) return ChapterMetadata.EMPTY;
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            root = gson.fromJson(reader, JsonObject.class);
        }
        UUID declared = BqQuestDefinitionImporter.uuid(root, "questLineIDHigh", "questLineIDLow");
        if (!directoryId.equals(declared)) throw new IOException(
            "QuestLine metadata UUID does not match directory: " + source);
        JsonObject properties = BqTypedJson.object(BqTypedJson.object(root, "properties"), "betterquesting");
        JsonObject icon = BqTypedJson.object(properties, "icon");
        return new ChapterMetadata(BqTypedJson.string(properties, "name", ""),
            BqTypedJson.string(properties, "desc", ""), icon.size() == 0 ? "" : gson.toJson(icon),
            BqTypedJson.string(properties, "bg_image", ""),
            (int) BqTypedJson.number(properties, "bg_size", 256L),
            visibility(BqTypedJson.string(properties, "visibility", "NORMAL")));
    }

    private static int positiveDimension(JsonObject root, String key, Path source) throws IOException {
        long value = BqTypedJson.number(root, key, 24L);
        if (value < 1L || value > Integer.MAX_VALUE) throw new IOException(
            "Invalid " + key + " in " + source + ": " + value);
        return (int) value;
    }

    private static Map<UUID, OrderedName> readOrder(Path source) throws IOException {
        Map<UUID, OrderedName> result = new LinkedHashMap<UUID, OrderedName>();
        int index = 0;
        for (String line : Files.readAllLines(source, StandardCharsets.UTF_8)) {
            String normalized = line.trim();
            if (normalized.isEmpty()) continue;
            int delimiter = normalized.indexOf(':');
            if (delimiter <= 0) throw new IOException("Malformed quest line order entry: " + line);
            UUID id = decodeUuid(normalized.substring(0, delimiter).trim());
            String name = normalized.substring(delimiter + 1).trim();
            if (name.isEmpty()) throw new IOException("Quest line name is blank for " + id);
            if (result.put(id, new OrderedName(index++, name)) != null) throw new IOException(
                "Duplicate quest line UUID in order file: " + id);
        }
        return result;
    }

    static UUID idFromFileName(String name) throws IOException {
        IOException lastFailure = null;
        for (int separator = name.indexOf('-'); separator >= 0; separator = name.indexOf('-', separator + 1)) {
            String candidate = name.substring(separator + 1);
            if (candidate.length() != 22 && candidate.length() != 24) continue;
            try {
                return decodeUuid(candidate);
            } catch (IOException malformed) {
                lastFailure = malformed;
            }
        }
        throw new IOException("Quest line directory has no encoded UUID suffix: " + name, lastFailure);
    }

    static UUID decodeUuid(String encoded) throws IOException {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            if (bytes.length != 16) throw new IOException("Encoded UUID must contain 16 bytes: " + encoded);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        } catch (IllegalArgumentException malformed) {
            throw new IOException("Invalid encoded quest line UUID: " + encoded, malformed);
        }
    }

    private static QuestVisibility visibility(String raw) {
        try { return QuestVisibility.valueOf(raw.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { return QuestVisibility.NORMAL; }
    }

    private static final class OrderedName {
        final int order;
        final String name;
        OrderedName(int order, String name) { this.order = order; this.name = name; }
    }

    private static final class ChapterMetadata {
        static final ChapterMetadata EMPTY = new ChapterMetadata("", "", "", "", 256, QuestVisibility.NORMAL);
        final String name;
        final String description;
        final String iconReference;
        final String backgroundReference;
        final int backgroundSize;
        final QuestVisibility visibility;
        ChapterMetadata(String name, String description, String iconReference, String backgroundReference,
            int backgroundSize, QuestVisibility visibility) {
            this.name = name;
            this.description = description;
            this.iconReference = iconReference;
            this.backgroundReference = backgroundReference;
            this.backgroundSize = backgroundSize > 0 ? backgroundSize : 256;
            this.visibility = visibility;
        }
    }
}
