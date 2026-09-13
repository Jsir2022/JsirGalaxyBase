package com.jsirgalaxybase.terminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestChapterCloneRequest;
import com.jsirgalaxybase.quest.core.QuestDefinitionCloneRequest;

/** Bounded Base-owned transport for an atomic chapter task-copy command. */
public final class TerminalQuestChapterClonePayload {
    private static final int MAGIC = 0x47434331; // GCC1
    private static final int MAX_ENCODED = 196608;
    private static final int MAX_SOURCES = 128;
    private final QuestChapterCloneRequest request;
    private final String query;
    private final String lifecycle;
    private final int page;

    public TerminalQuestChapterClonePayload(QuestChapterCloneRequest request, String query, String lifecycle, int page) {
        if (request == null) throw new IllegalArgumentException("request is required");
        this.request = request;
        this.query = bounded(query, 96);
        this.lifecycle = bounded(lifecycle, 16);
        this.page = Math.max(0, Math.min(100000, page));
    }

    public QuestChapterCloneRequest getRequest() { return request; }
    public String getQuery() { return query; }
    public String getLifecycle() { return lifecycle; }
    public int getPage() { return page; }

    public String encode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(MAGIC);
            text(out, request.getChapterId().toString(), 36);
            out.writeInt(request.getChapterVersion());
            text(out, request.getExpectedChapterHash(), 128);
            out.writeInt(request.getAnchorX());
            out.writeInt(request.getAnchorY());
            text(out, query, 96);
            text(out, lifecycle, 16);
            out.writeInt(page);
            List<QuestDefinitionCloneRequest.Source> sources = request.getSources().getSources();
            out.writeInt(sources.size());
            for (QuestDefinitionCloneRequest.Source source : sources) {
                text(out, source.getQuestId().toString(), 36);
                out.writeInt(source.getVersion());
            }
            out.flush();
            String result = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
            if (result.length() > MAX_ENCODED) throw new IllegalArgumentException("chapter clone payload is too large");
            return result;
        } catch (IOException impossible) { throw new IllegalStateException(impossible); }
    }

    public static TerminalQuestChapterClonePayload decode(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_ENCODED) {
            throw new IllegalArgumentException("invalid chapter clone payload size");
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(value)));
            if (in.readInt() != MAGIC) throw new IllegalArgumentException("unsupported chapter clone payload");
            UUID chapterId = UUID.fromString(text(in, 36));
            int chapterVersion = in.readInt();
            String hash = text(in, 128);
            int anchorX = in.readInt();
            int anchorY = in.readInt();
            String query = text(in, 96);
            String lifecycle = text(in, 16);
            int page = in.readInt();
            int count = in.readInt();
            if (count < 1 || count > MAX_SOURCES) throw new IllegalArgumentException("invalid clone source count");
            List<QuestDefinitionCloneRequest.Source> sources = new ArrayList<QuestDefinitionCloneRequest.Source>(count);
            for (int index = 0; index < count; index++) {
                sources.add(new QuestDefinitionCloneRequest.Source(UUID.fromString(text(in, 36)), in.readInt()));
            }
            if (in.available() != 0) throw new IllegalArgumentException("trailing chapter clone payload data");
            return new TerminalQuestChapterClonePayload(new QuestChapterCloneRequest(chapterId, chapterVersion, hash,
                new QuestDefinitionCloneRequest(sources), anchorX, anchorY), query, lifecycle, page);
        } catch (IOException | RuntimeException invalid) {
            throw new IllegalArgumentException("invalid chapter clone payload", invalid);
        }
    }

    private static void text(DataOutputStream out, String value, int max) throws IOException { out.writeUTF(bounded(value, max)); }
    private static String text(DataInputStream in, int max) throws IOException { return bounded(in.readUTF(), max); }
    private static String bounded(String value, int max) {
        String result = value == null ? "" : value;
        if (result.length() > max) throw new IllegalArgumentException("text exceeds " + max + " characters");
        return result;
    }
}
