package com.aetherium.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-contained JSON reader/writer for the config file. Aetherium does not pull
 * in Gson: the runtime Minecraft jar has it, but {@code common} must stay
 * loadable in a bare JVM for the unit tests in {@code common/src/test/java}, and
 * a hand-written parser is 200 lines versus a version-pinned transitive dep.
 *
 * <p>Supported input is JSON plus the two conveniences users actually need in a
 * hand-edited file: {@code //} line comments and trailing commas in objects and
 * arrays. Anything else is a hard error — a silently mis-parsed config that
 * turns off the renderer is worse than a stack trace at boot.</p>
 *
 * <p>Value model: {@link Map}&lt;String,Object&gt;, {@link List}&lt;Object&gt;,
 * {@link String}, {@link Boolean}, {@link Long}, {@link Double}, null. That is
 * deliberately the same shape Gson produces, so a future switch is local.</p>
 */
public final class Json {
    private Json() {
    }

    // ------------------------------------------------------------------ writing

    public static String write(final Object value) {
        final StringBuilder builder = new StringBuilder(1024);
        writeValue(builder, value, 0);
        builder.append('\n');
        return builder.toString();
    }

    private static void writeValue(final StringBuilder builder, final Object value, final int depth) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof Boolean) {
            builder.append(((Boolean) value) ? "true" : "false");
        } else if (value instanceof Long) {
            builder.append(((Long) value).longValue());
        } else if (value instanceof Integer) {
            builder.append(((Integer) value).intValue());
        } else if (value instanceof Double) {
            final double number = ((Double) value).doubleValue();
            if (!Double.isFinite(number)) {
                // JSON has no NaN/Infinity; emit null so the reader can fall back.
                builder.append("null");
            } else if (number == Math.rint(number) && Math.abs(number) < 1.0E15d) {
                builder.append((long) number).append(".0");
            } else {
                builder.append(number);
            }
        } else if (value instanceof Number) {
            builder.append(value);
        } else if (value instanceof CharSequence) {
            writeString(builder, value.toString());
        } else if (value instanceof Map) {
            writeObject(builder, (Map<?, ?>) value, depth);
        } else if (value instanceof List) {
            writeArray(builder, (List<?>) value, depth);
        } else if (value instanceof Object[]) {
            final List<Object> boxed = new ArrayList<>();
            for (final Object element : (Object[]) value) {
                boxed.add(element);
            }
            writeArray(builder, boxed, depth);
        } else {
            writeString(builder, String.valueOf(value));
        }
    }

    private static void writeObject(final StringBuilder builder, final Map<?, ?> map, final int depth) {
        if (map.isEmpty()) {
            builder.append("{}");
            return;
        }
        builder.append("{\n");
        final String indent = repeat(depth + 1);
        boolean first = true;
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                builder.append(",\n");
            }
            first = false;
            builder.append(indent);
            writeString(builder, String.valueOf(entry.getKey()));
            builder.append(": ");
            writeValue(builder, entry.getValue(), depth + 1);
        }
        builder.append('\n').append(repeat(depth)).append('}');
    }

    private static void writeArray(final StringBuilder builder, final List<?> list, final int depth) {
        if (list.isEmpty()) {
            builder.append("[]");
            return;
        }
        builder.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                builder.append(",\n");
            }
            builder.append(repeat(depth + 1));
            writeValue(builder, list.get(i), depth + 1);
        }
        builder.append('\n').append(repeat(depth)).append(']');
    }

    private static void writeString(final StringBuilder builder, final String text) {
        builder.append('"');
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                default:
                    if (c < 0x20 || c == 0x7F) {
                        builder.append(String.format("\\u%04X", (int) c));
                    } else {
                        builder.append(c);
                    }
            }
        }
        builder.append('"');
    }

    private static String repeat(final int depth) {
        final char[] chars = new char[depth * 2];
        java.util.Arrays.fill(chars, ' ');
        return new String(chars);
    }

    // ------------------------------------------------------------------ reading

    /** @throws IllegalArgumentException with a line/column hint on malformed input */
    public static Object parse(final String text) {
        return new Reader(text).readDocument();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(final String text) {
        final Object root = parse(text);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("expected a JSON object at the document root, found "
                    + (root == null ? "null" : root.getClass().getSimpleName()));
        }
        return (Map<String, Object>) root;
    }

    /** Flat lookup of "a.b.c" in a parsed document; used by the config store. */
    public static Object path(final Map<String, Object> root, final String dotted) {
        Object current = root;
        for (final String part : dotted.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /** Flatten a nested document into dotted keys — the config format is 2 deep. */
    public static Map<String, Object> flatten(final Map<String, Object> root) {
        final Map<String, Object> out = new LinkedHashMap<>();
        flattenInto("", root, out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void flattenInto(final String prefix, final Map<String, Object> source, final Map<String, Object> out) {
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            final String key = prefix.isEmpty() ? entry.getKey() : prefix + '.' + entry.getKey();
            final Object value = entry.getValue();
            if (value instanceof Map) {
                flattenInto(key, (Map<String, Object>) value, out);
            } else {
                out.put(key, value);
            }
        }
    }

    /** Recursive-descent reader. Single-threaded by construction (one instance per parse). */
    private static final class Reader {
        private final String text;
        private int index;

        Reader(final String text) {
            this.text = text;
        }

        Object readDocument() {
            this.skipTrivia();
            final Object value = this.readValue();
            this.skipTrivia();
            if (this.index < this.text.length()) {
                throw this.error("trailing characters after document");
            }
            return value;
        }

        private Object readValue() {
            this.skipTrivia();
            if (this.index >= this.text.length()) {
                throw this.error("unexpected end of input");
            }
            final char c = this.text.charAt(this.index);
            switch (c) {
                case '{':
                    return this.readObject();
                case '[':
                    return this.readArray();
                case '"':
                    return this.readString();
                case 't':
                    this.expectWord("true");
                    return Boolean.TRUE;
                case 'f':
                    this.expectWord("false");
                    return Boolean.FALSE;
                case 'n':
                    this.expectWord("null");
                    return null;
                default:
                    return this.readNumber();
            }
        }

        private Map<String, Object> readObject() {
            final Map<String, Object> out = new LinkedHashMap<>();
            this.index++; // '{'
            this.skipTrivia();
            if (this.peek() == '}') {
                this.index++;
                return out;
            }
            while (true) {
                this.skipTrivia();
                if (this.peek() == '}') { // trailing comma tolerance
                    this.index++;
                    return out;
                }
                if (this.peek() != '"') {
                    throw this.error("expected a quoted object key");
                }
                final String key = (String) this.readString();
                this.skipTrivia();
                if (this.peek() != ':') {
                    throw this.error("expected ':' after key \"" + key + '"');
                }
                this.index++;
                out.put(key, this.readValue());
                this.skipTrivia();
                final char next = this.peek();
                if (next == ',') {
                    this.index++;
                } else if (next == '}') {
                    this.index++;
                    return out;
                } else {
                    throw this.error("expected ',' or '}' in object");
                }
            }
        }

        private List<Object> readArray() {
            final List<Object> out = new ArrayList<>();
            this.index++; // '['
            this.skipTrivia();
            if (this.peek() == ']') {
                this.index++;
                return out;
            }
            while (true) {
                this.skipTrivia();
                if (this.peek() == ']') {
                    this.index++;
                    return out;
                }
                out.add(this.readValue());
                this.skipTrivia();
                final char next = this.peek();
                if (next == ',') {
                    this.index++;
                } else if (next == ']') {
                    this.index++;
                    return out;
                } else {
                    throw this.error("expected ',' or ']' in array");
                }
            }
        }

        private String readString() {
            this.index++; // opening quote
            final StringBuilder builder = new StringBuilder(32);
            while (true) {
                if (this.index >= this.text.length()) {
                    throw this.error("unterminated string");
                }
                final char c = this.text.charAt(this.index++);
                if (c == '"') {
                    return builder.toString();
                }
                if (c != '\\') {
                    builder.append(c);
                    continue;
                }
                if (this.index >= this.text.length()) {
                    throw this.error("unterminated escape");
                }
                final char escape = this.text.charAt(this.index++);
                switch (escape) {
                    case 'n':
                        builder.append('\n');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case '/':
                        builder.append('/');
                        break;
                    case '"':
                        builder.append('"');
                        break;
                    case '\\':
                        builder.append('\\');
                        break;
                    case 'u': {
                        if (this.index + 4 > this.text.length()) {
                            throw this.error("truncated \\u escape");
                        }
                        final String hex = this.text.substring(this.index, this.index + 4);
                        try {
                            builder.append((char) Integer.parseInt(hex, 16));
                        } catch (final NumberFormatException error) {
                            throw new IllegalArgumentException("bad \\u escape '" + hex + "' at offset " + this.index, error);
                        }
                        this.index += 4;
                        break;
                    }
                    default:
                        throw this.error("unsupported escape '\\" + escape + "'");
                }
            }
        }

        private Object readNumber() {
            final int start = this.index;
            boolean floating = false;
            if (this.peek() == '-' || this.peek() == '+') {
                this.index++;
            }
            while (this.index < this.text.length()) {
                final char c = this.text.charAt(this.index);
                if (c >= '0' && c <= '9') {
                    this.index++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') {
                    floating = floating || c == '.' || c == 'e' || c == 'E';
                    this.index++;
                } else {
                    break;
                }
            }
            final String raw = this.text.substring(start, this.index);
            if (raw.isEmpty() || raw.equals("-") || raw.equals("+")) {
                throw this.error("expected a number");
            }
            try {
                if (floating) {
                    return Double.valueOf(raw);
                }
                return Long.valueOf(Long.parseLong(raw));
            } catch (final NumberFormatException error) {
                throw new IllegalArgumentException("bad number '" + raw + "' at offset " + start, error);
            }
        }

        private void expectWord(final String word) {
            if (!this.text.startsWith(word, this.index)) {
                throw this.error("expected '" + word + "'");
            }
            this.index += word.length();
        }

        private char peek() {
            return this.index < this.text.length() ? this.text.charAt(this.index) : '\0';
        }

        /** Skips whitespace, // line comments and /*&#42; block comments. */
        private void skipTrivia() {
            while (this.index < this.text.length()) {
                final char c = this.text.charAt(this.index);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == 11) {
                    this.index++;
                } else if (c == '/' && this.text.startsWith("//", this.index)) {
                    final int newline = this.text.indexOf('\n', this.index);
                    this.index = newline < 0 ? this.text.length() : newline + 1;
                } else if (c == '/' && this.text.startsWith("/*", this.index)) {
                    final int end = this.text.indexOf("*/", this.index + 2);
                    if (end < 0) {
                        throw this.error("unterminated block comment");
                    }
                    this.index = end + 2;
                } else {
                    return;
                }
            }
        }

        private IllegalArgumentException error(final String message) {
            int line = 1;
            int column = 1;
            for (int i = 0; i < this.index && i < this.text.length(); i++) {
                if (this.text.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            return new IllegalArgumentException("JSON " + message + " at line " + line + ", column " + column);
        }
    }
}
