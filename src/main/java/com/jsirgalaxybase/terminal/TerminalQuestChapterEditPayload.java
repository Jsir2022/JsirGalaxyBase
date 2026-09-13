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

import com.jsirgalaxybase.quest.core.QuestChapterEditOperation;
import com.jsirgalaxybase.quest.core.QuestChapterEditRequest;
import com.jsirgalaxybase.quest.core.QuestVisibility;

/** Bounded Base-owned transport for one atomic chapter draft edit batch. */
public final class TerminalQuestChapterEditPayload {
    private static final int MAGIC = 0x47434531;
    private static final int MAX_ENCODED = 196608;
    private static final int MAX_OPERATIONS = 128;
    private final QuestChapterEditRequest request;
    private final String query;
    private final String lifecycle;
    private final int page;

    public TerminalQuestChapterEditPayload(QuestChapterEditRequest request, String query, String lifecycle, int page) {
        if (request == null) throw new IllegalArgumentException("request is required");
        this.request = request;
        this.query = bounded(query, 96);
        this.lifecycle = bounded(lifecycle, 16);
        this.page = Math.max(0, Math.min(100000, page));
    }

    public QuestChapterEditRequest getRequest() { return request; }
    public String getQuery() { return query; }
    public String getLifecycle() { return lifecycle; }
    public int getPage() { return page; }

    public String encode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(MAGIC);
            text(out, request.getChapterId().toString(), 36);
            out.writeInt(request.getVersion());
            text(out, request.getExpectedHash(), 128);
            text(out, query, 96);
            text(out, lifecycle, 16);
            out.writeInt(page);
            out.writeInt(request.getOperations().size());
            for (QuestChapterEditOperation operation : request.getOperations()) writeOperation(out, operation);
            out.flush();
            String result = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
            if (result.length() > MAX_ENCODED) throw new IllegalArgumentException("chapter payload is too large");
            return result;
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static TerminalQuestChapterEditPayload decode(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_ENCODED) {
            throw new IllegalArgumentException("invalid chapter payload size");
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(value)));
            if (in.readInt() != MAGIC) throw new IllegalArgumentException("unsupported chapter payload");
            UUID id = UUID.fromString(text(in, 36));
            int version = in.readInt();
            String hash = text(in, 128);
            String query = text(in, 96);
            String lifecycle = text(in, 16);
            int page = in.readInt();
            int count = in.readInt();
            if (count < 1 || count > MAX_OPERATIONS) throw new IllegalArgumentException("invalid operation count");
            List<QuestChapterEditOperation> operations = new ArrayList<QuestChapterEditOperation>(count);
            for (int i = 0; i < count; i++) operations.add(readOperation(in));
            if (in.available() != 0) throw new IllegalArgumentException("trailing chapter payload data");
            return new TerminalQuestChapterEditPayload(new QuestChapterEditRequest(id, version, hash, operations),
                query, lifecycle, page);
        } catch (IOException | RuntimeException invalid) {
            throw new IllegalArgumentException("invalid chapter edit payload", invalid);
        }
    }

    private static void writeOperation(DataOutputStream out, QuestChapterEditOperation operation) throws IOException {
        out.writeByte(operation.getKind().ordinal());
        switch (operation.getKind()) {
            case SET_NAME: text(out, operation.getText(), 256); break;
            case SET_DESCRIPTION: text(out, operation.getText(), 4096); break;
            case SET_ICON: case SET_BACKGROUND: text(out, operation.getText(), 256); break;
            case SET_BACKGROUND_SIZE: out.writeInt(operation.getFirst()); break;
            case SET_VISIBILITY: text(out, operation.getText(), 16); break;
            case PLACE:
                id(out, operation.getQuestId());
                out.writeInt(operation.getFirst()); out.writeInt(operation.getSecond());
                out.writeInt(operation.getThird()); out.writeInt(operation.getFourth());
                break;
            case MOVE: case RESIZE:
                id(out, operation.getQuestId());
                out.writeInt(operation.getFirst()); out.writeInt(operation.getSecond());
                break;
            case REMOVE: id(out, operation.getQuestId()); break;
            default: throw new IllegalArgumentException("unsupported chapter operation");
        }
    }

    private static QuestChapterEditOperation readOperation(DataInputStream in) throws IOException {
        int ordinal = in.readUnsignedByte();
        QuestChapterEditOperation.Kind[] kinds = QuestChapterEditOperation.Kind.values();
        if (ordinal >= kinds.length) throw new IllegalArgumentException("unknown chapter operation");
        switch (kinds[ordinal]) {
            case SET_NAME: return QuestChapterEditOperation.name(text(in, 256));
            case SET_DESCRIPTION: return QuestChapterEditOperation.description(text(in, 4096));
            case SET_ICON: return QuestChapterEditOperation.icon(text(in, 256));
            case SET_BACKGROUND: return QuestChapterEditOperation.background(text(in, 256));
            case SET_BACKGROUND_SIZE: return QuestChapterEditOperation.backgroundSize(in.readInt());
            case SET_VISIBILITY: return QuestChapterEditOperation.visibility(QuestVisibility.valueOf(text(in, 16)));
            case PLACE: return QuestChapterEditOperation.place(uuid(in), in.readInt(), in.readInt(), in.readInt(), in.readInt());
            case MOVE: return QuestChapterEditOperation.move(uuid(in), in.readInt(), in.readInt());
            case RESIZE: return QuestChapterEditOperation.resize(uuid(in), in.readInt(), in.readInt());
            case REMOVE: return QuestChapterEditOperation.remove(uuid(in));
            default: throw new IllegalArgumentException("unsupported chapter operation");
        }
    }

    private static void id(DataOutputStream out, UUID value) throws IOException { text(out, value.toString(), 36); }
    private static UUID uuid(DataInputStream in) throws IOException { return UUID.fromString(text(in, 36)); }
    private static void text(DataOutputStream out, String value, int max) throws IOException { out.writeUTF(bounded(value, max)); }
    private static String text(DataInputStream in, int max) throws IOException { return bounded(in.readUTF(), max); }
    private static String bounded(String value, int max) {
        String result = value == null ? "" : value;
        if (result.length() > max) throw new IllegalArgumentException("text exceeds " + max + " characters");
        return result;
    }
}
