package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TiptapJsonDocument {
    private static final int MAX_JSON_BYTES = 256 * 1024;
    private static final int MAX_EXTRACTED_TEXT_LENGTH = 10_000;
    private static final int MAX_DEPTH = 20;
    private static final int MAX_IMAGES = 10;

    private static final Set<String> ALLOWED_NODE_TYPES = Set.of(
            "doc",
            "paragraph",
            "text",
            "heading",
            "bulletList",
            "orderedList",
            "listItem",
            "blockquote",
            "hardBreak",
            "codeBlock",
            "image"
    );

    private static final Set<String> ALLOWED_MARK_TYPES = Set.of(
            "bold",
            "italic",
            "code",
            "link"
    );

    private final String value;

    private TiptapJsonDocument(String value) {
        this.value = value;
    }

    public static TiptapJsonDocument of(String rawJson) {
        return of(rawJson, null);
    }

    public static TiptapJsonDocument of(String rawJson, TiptapJsonImagePolicy imagePolicy) {
        validate(rawJson, imagePolicy);
        return new TiptapJsonDocument(rawJson);
    }

    public String value() {
        return value;
    }

    private static void validate(String rawJson, TiptapJsonImagePolicy imagePolicy) {
        if (rawJson == null || rawJson.isBlank()) {
            throw invalid();
        }
        if (rawJson.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES) {
            throw invalid();
        }

        Object parsed = new JsonParser(rawJson).parse();
        ValidationState state = new ValidationState(imagePolicy);
        validateNode(parsed, 1, state);
        if (state.textLength > MAX_EXTRACTED_TEXT_LENGTH) {
            throw invalid();
        }
    }

    private static void validateNode(Object value, int depth, ValidationState state) {
        if (!(value instanceof Map<?, ?> map)) {
            throw invalid();
        }
        if (depth > MAX_DEPTH) {
            throw invalid();
        }

        String type = string(map, "type");
        if (!ALLOWED_NODE_TYPES.contains(type)) {
            throw invalid();
        }
        if (!map.containsKey("type")) {
            throw invalid();
        }

        switch (type) {
            case "doc" -> validateDoc(map, depth, state);
            case "paragraph", "bulletList", "orderedList", "listItem", "blockquote" -> validateContainerNode(map, depth, state, type);
            case "text" -> validateText(map, state);
            case "heading" -> validateHeading(map, depth, state);
            case "hardBreak" -> validateHardBreak(map);
            case "codeBlock" -> validateCodeBlock(map, depth, state);
            case "image" -> validateImage(map, state);
            default -> throw invalid();
        }
    }

    private static void validateDoc(Map<?, ?> map, int depth, ValidationState state) {
        ensureAllowedKeys(map, Set.of("type", "content"));
        validateChildren(array(map, "content"), depth + 1, state);
    }

    private static void validateContainerNode(Map<?, ?> map, int depth, ValidationState state, String type) {
        ensureAllowedKeys(map, Set.of("type", "content"));
        validateChildren(array(map, "content"), depth + 1, state);
    }

    private static void validateHeading(Map<?, ?> map, int depth, ValidationState state) {
        ensureAllowedKeys(map, Set.of("type", "attrs", "content"));
        Map<?, ?> attrs = object(map, "attrs");
        ensureAllowedKeys(attrs, Set.of("level"));
        Number level = number(attrs, "level");
        int headingLevel = level.intValue();
        if (headingLevel < 1 || headingLevel > 4 || headingLevel != level.doubleValue()) {
            throw invalid();
        }
        validateChildren(array(map, "content"), depth + 1, state);
    }

    private static void validateCodeBlock(Map<?, ?> map, int depth, ValidationState state) {
        ensureAllowedKeys(map, Set.of("type", "content"));
        validateChildren(array(map, "content"), depth + 1, state);
    }

    private static void validateHardBreak(Map<?, ?> map) {
        ensureAllowedKeys(map, Set.of("type"));
    }

    private static void validateText(Map<?, ?> map, ValidationState state) {
        ensureAllowedKeys(map, Set.of("type", "text", "marks"));
        String text = string(map, "text");
        state.textLength += text.length();

        Object marksValue = map.get("marks");
        if (marksValue == null) {
            return;
        }
        if (!(marksValue instanceof List<?> marks)) {
            throw invalid();
        }
        for (Object mark : marks) {
            validateMark(mark);
        }
    }

    private static void validateImage(Map<?, ?> map, ValidationState state) {
        ensureAllowedKeys(map, Set.of("type", "attrs"));
        Map<?, ?> attrs = object(map, "attrs");
        ensureAllowedKeys(attrs, Set.of("src", "objectKey", "alt", "title"));
        String src = string(attrs, "src");
        String objectKey = string(attrs, "objectKey");
        if (state.imagePolicy == null || !state.imagePolicy.accepts(objectKey, src)) {
            throw invalid();
        }
        state.imageCount++;
        if (state.imageCount > MAX_IMAGES) {
            throw invalid();
        }
    }

    private static void validateChildren(List<?> children, int depth, ValidationState state) {
        for (Object child : children) {
            validateNode(child, depth, state);
        }
    }

    private static void validateMark(Object markValue) {
        if (!(markValue instanceof Map<?, ?> mark)) {
            throw invalid();
        }
        String type = string(mark, "type");
        if (!ALLOWED_MARK_TYPES.contains(type)) {
            throw invalid();
        }
        switch (type) {
            case "bold", "italic", "code" -> ensureAllowedKeys(mark, Set.of("type"));
            case "link" -> validateLinkMark(mark);
            default -> throw invalid();
        }
    }

    private static void validateLinkMark(Map<?, ?> mark) {
        ensureAllowedKeys(mark, Set.of("type", "attrs"));
        Map<?, ?> attrs = object(mark, "attrs");
        ensureAllowedKeys(attrs, Set.of("href"));
        String href = string(attrs, "href");
        if (!(href.startsWith("http://") || href.startsWith("https://"))) {
            throw invalid();
        }
    }

    private static void ensureAllowedKeys(Map<?, ?> map, Set<String> allowedKeys) {
        for (Object key : map.keySet()) {
            if (!(key instanceof String stringKey) || !allowedKeys.contains(stringKey)) {
                throw invalid();
            }
        }
    }

    private static Map<?, ?> object(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Map<?, ?> object)) {
            throw invalid();
        }
        return object;
    }

    private static List<?> array(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> list)) {
            throw invalid();
        }
        return list;
    }

    private static String string(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String string)) {
            throw invalid();
        }
        return string;
    }

    private static Number number(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw invalid();
        }
        return number;
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }

    private static final class ValidationState {
        private final TiptapJsonImagePolicy imagePolicy;
        private int textLength;
        private int imageCount;

        private ValidationState(TiptapJsonImagePolicy imagePolicy) {
            this.imagePolicy = imagePolicy;
        }
    }

    private static final class JsonParser {
        private final String text;
        private int index;

        private JsonParser(String text) {
            this.text = Objects.requireNonNull(text, "text");
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (!isEof()) {
                throw invalid();
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isEof()) {
                throw invalid();
            }
            char ch = peek();
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (ch == '-' || Character.isDigit(ch)) {
                        yield parseNumber();
                    }
                    throw invalid();
                }
            };
        }

        private Map<String, Object> parseObject() {
            consume('{');
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            skipWhitespace();
            if (match('}')) {
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                consume(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (match('}')) {
                    return map;
                }
                consume(',');
            }
        }

        private List<Object> parseArray() {
            consume('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (match(']')) {
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (match(']')) {
                    return list;
                }
                consume(',');
            }
        }

        private String parseString() {
            consume('"');
            StringBuilder builder = new StringBuilder();
            while (!isEof()) {
                char ch = next();
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (isEof()) {
                        throw invalid();
                    }
                    char escaped = next();
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicodeEscape());
                        default -> throw invalid();
                    }
                } else {
                    if (ch < 0x20) {
                        throw invalid();
                    }
                    builder.append(ch);
                }
            }
            throw invalid();
        }

        private char parseUnicodeEscape() {
            if (index + 4 > text.length()) {
                throw invalid();
            }
            int codePoint;
            try {
                codePoint = Integer.parseInt(text.substring(index, index + 4), 16);
            } catch (NumberFormatException ex) {
                throw invalid();
            }
            index += 4;
            return (char) codePoint;
        }

        private Number parseNumber() {
            int start = index;
            if (peek() == '-') {
                index++;
            }
            consumeDigits();
            if (match('.')) {
                consumeDigits();
            }
            if (match('e') || match('E')) {
                if (match('+') || match('-')) {
                    // optional sign
                }
                consumeDigits();
            }
            String number = text.substring(start, index);
            try {
                return Double.valueOf(number);
            } catch (NumberFormatException ex) {
                throw invalid();
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw invalid();
            }
            index += literal.length();
            return value;
        }

        private void consumeDigits() {
            if (isEof() || !Character.isDigit(peek())) {
                throw invalid();
            }
            while (!isEof() && Character.isDigit(peek())) {
                index++;
            }
        }

        private void skipWhitespace() {
            while (!isEof() && Character.isWhitespace(peek())) {
                index++;
            }
        }

        private void consume(char expected) {
            if (isEof() || peek() != expected) {
                throw invalid();
            }
            index++;
        }

        private boolean match(char expected) {
            if (!isEof() && peek() == expected) {
                index++;
                return true;
            }
            return false;
        }

        private char peek() {
            return text.charAt(index);
        }

        private char next() {
            return text.charAt(index++);
        }

        private boolean isEof() {
            return index >= text.length();
        }
    }
}
