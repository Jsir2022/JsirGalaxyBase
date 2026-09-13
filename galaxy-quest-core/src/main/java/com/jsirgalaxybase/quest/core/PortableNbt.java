package com.jsirgalaxybase.quest.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Platform-neutral NBT tree used to compare imported BQ data with live Minecraft facts. */
public final class PortableNbt {
    public enum Kind { COMPOUND, LIST, STRING, NUMBER, BYTE_ARRAY, INT_ARRAY }

    private final Kind kind;
    private final Map<String, PortableNbt> fields;
    private final List<PortableNbt> values;
    private final String scalar;

    private PortableNbt(Kind kind, Map<String, PortableNbt> fields, List<PortableNbt> values, String scalar) {
        this.kind = kind;
        this.fields = fields == null ? Collections.<String, PortableNbt>emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<String, PortableNbt>(fields));
        this.values = values == null ? Collections.<PortableNbt>emptyList()
            : Collections.unmodifiableList(new ArrayList<PortableNbt>(values));
        this.scalar = scalar == null ? "" : scalar;
    }

    public static PortableNbt compound(Map<String, PortableNbt> fields) {
        return new PortableNbt(Kind.COMPOUND, new TreeMap<String, PortableNbt>(fields), null, null);
    }
    public static PortableNbt list(List<PortableNbt> values) {
        return new PortableNbt(Kind.LIST, null, values, null);
    }
    public static PortableNbt string(String value) {
        return new PortableNbt(Kind.STRING, null, null, value);
    }
    public static PortableNbt number(String value) {
        return new PortableNbt(Kind.NUMBER, null, null, normalizeNumber(value));
    }
    public static PortableNbt byteArray(List<PortableNbt> values) {
        return new PortableNbt(Kind.BYTE_ARRAY, null, values, null);
    }
    public static PortableNbt intArray(List<PortableNbt> values) {
        return new PortableNbt(Kind.INT_ARRAY, null, values, null);
    }

    public Kind getKind() { return kind; }
    public Map<String, PortableNbt> getFields() { return fields; }
    public List<PortableNbt> getValues() { return values; }
    public String getScalar() { return scalar; }
    public boolean isEmpty() {
        return kind == Kind.COMPOUND && fields.isEmpty() || kind == Kind.LIST && values.isEmpty();
    }

    public String encode() {
        StringBuilder out = new StringBuilder();
        append(out);
        return out.toString();
    }

    public static PortableNbt decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return compound(Collections.<String, PortableNbt>emptyMap());
        Parser parser = new Parser(encoded);
        PortableNbt result = parser.node();
        if (!parser.done()) throw new IllegalArgumentException("trailing portable NBT data");
        return result;
    }

    private void append(StringBuilder out) {
        out.append(kindCode(kind));
        if (kind == Kind.STRING || kind == Kind.NUMBER) {
            appendText(out, scalar);
        } else if (kind == Kind.COMPOUND) {
            out.append(fields.size()).append(':');
            for (Map.Entry<String, PortableNbt> entry : fields.entrySet()) {
                appendText(out, entry.getKey());
                entry.getValue().append(out);
            }
        } else {
            out.append(values.size()).append(':');
            for (PortableNbt value : values) value.append(out);
        }
    }

    private static void appendText(StringBuilder out, String value) {
        out.append(value.length()).append(':').append(value);
    }

    private static char kindCode(Kind kind) {
        switch (kind) {
            case COMPOUND: return 'C';
            case LIST: return 'L';
            case STRING: return 'S';
            case NUMBER: return 'N';
            case BYTE_ARRAY: return 'B';
            case INT_ARRAY: return 'I';
            default: throw new IllegalStateException("unknown NBT kind");
        }
    }

    private static String normalizeNumber(String value) {
        try {
            BigDecimal decimal = new BigDecimal(value).stripTrailingZeros();
            return decimal.signum() == 0 ? "0" : decimal.toPlainString();
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("invalid numeric NBT value: " + value, invalid);
        }
    }

    private static final class Parser {
        private final String source;
        private int offset;
        private Parser(String source) { this.source = source; }
        private boolean done() { return offset == source.length(); }
        private PortableNbt node() {
            if (offset >= source.length()) throw new IllegalArgumentException("truncated portable NBT");
            char kind = source.charAt(offset++);
            if (kind == 'S') return string(text());
            if (kind == 'N') return number(text());
            int count = count();
            if (kind == 'C') {
                Map<String, PortableNbt> fields = new LinkedHashMap<String, PortableNbt>();
                for (int i = 0; i < count; i++) {
                    String key = text();
                    if (fields.put(key, node()) != null) throw new IllegalArgumentException("duplicate NBT key");
                }
                return compound(fields);
            }
            List<PortableNbt> values = new ArrayList<PortableNbt>(count);
            for (int i = 0; i < count; i++) values.add(node());
            if (kind == 'L') return list(values);
            if (kind == 'B') return byteArray(values);
            if (kind == 'I') return intArray(values);
            throw new IllegalArgumentException("unknown portable NBT kind: " + kind);
        }
        private int count() {
            int delimiter = source.indexOf(':', offset);
            if (delimiter < 0) throw new IllegalArgumentException("truncated portable NBT length");
            int value = Integer.parseInt(source.substring(offset, delimiter));
            if (value < 0) throw new IllegalArgumentException("negative portable NBT length");
            offset = delimiter + 1;
            return value;
        }
        private String text() {
            int length = count();
            if (source.length() - offset < length) throw new IllegalArgumentException("truncated portable NBT text");
            String result = source.substring(offset, offset + length);
            offset += length;
            return result;
        }
    }
}
