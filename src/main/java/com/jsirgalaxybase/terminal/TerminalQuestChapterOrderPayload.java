package com.jsirgalaxybase.terminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

/** Bounded intent for moving one chapter before another in the authoritative catalog. */
public final class TerminalQuestChapterOrderPayload {
    private static final int MAGIC = 0x47434F31; // GCO1
    private final UUID chapterId, beforeChapterId;
    private final String query, lifecycle;
    private final int page;
    public TerminalQuestChapterOrderPayload(UUID chapterId, UUID beforeChapterId, String query, String lifecycle, int page) {
        if (chapterId == null || chapterId.equals(beforeChapterId)) throw new IllegalArgumentException("distinct chapter identities are required");
        this.chapterId = chapterId; this.beforeChapterId = beforeChapterId; this.query = text(query, 96); this.lifecycle = text(lifecycle, 16); this.page = Math.max(0, Math.min(100000, page));
    }
    public UUID getChapterId() { return chapterId; } public UUID getBeforeChapterId() { return beforeChapterId; }
    public String getQuery() { return query; } public String getLifecycle() { return lifecycle; } public int getPage() { return page; }
    public String encode() { try { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(MAGIC); out.writeUTF(chapterId.toString()); out.writeBoolean(beforeChapterId != null); if (beforeChapterId != null) out.writeUTF(beforeChapterId.toString()); out.writeUTF(query); out.writeUTF(lifecycle); out.writeInt(page); out.flush();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    } catch (IOException impossible) { throw new IllegalStateException(impossible); } }
    public static TerminalQuestChapterOrderPayload decode(String value) { try { if (value == null || value.isEmpty() || value.length() > 1024) throw new IllegalArgumentException("invalid chapter order payload size");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(value))); if (in.readInt() != MAGIC) throw new IllegalArgumentException("unsupported chapter order payload");
        UUID chapter = UUID.fromString(text(in.readUTF(), 36)); UUID before = in.readBoolean() ? UUID.fromString(text(in.readUTF(), 36)) : null; String query = text(in.readUTF(), 96), lifecycle = text(in.readUTF(), 16); int page = in.readInt();
        if (in.available() != 0) throw new IllegalArgumentException("trailing chapter order payload data"); return new TerminalQuestChapterOrderPayload(chapter, before, query, lifecycle, page);
    } catch (IOException | RuntimeException invalid) { throw new IllegalArgumentException("invalid chapter order payload", invalid); } }
    private static String text(String value, int max) { String result = value == null ? "" : value; if (result.length() > max) throw new IllegalArgumentException("text exceeds limit"); return result; }
}
