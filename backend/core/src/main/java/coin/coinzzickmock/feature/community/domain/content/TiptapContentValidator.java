package coin.coinzzickmock.feature.community.domain.content;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class TiptapContentValidator {
    public static final int MAX_JSON_BYTES = 256 * 1024;
    public static final int MAX_TEXT_LENGTH = 10_000;
    public static final int MAX_DOCUMENT_DEPTH = 20;
    public static final int MAX_IMAGE_COUNT = 10;

    private static final Set<String> NODE_TYPES = Set.of(
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
    private static final Set<String> MARK_TYPES = Set.of("bold", "italic", "code", "link");
    private static final Set<String> NODE_KEYS = Set.of("type", "content", "text", "marks", "attrs");
    private static final Set<String> TEXT_NODE_KEYS = Set.of("type", "text", "marks");
    private static final Set<String> MARK_KEYS = Set.of("type", "attrs");
    private static final Set<String> LINK_ATTR_KEYS = Set.of("href");
    private static final Set<String> HEADING_ATTR_KEYS = Set.of("level");
    private static final Set<String> IMAGE_ATTR_KEYS = Set.of("objectKey", "src");
    private static final Pattern COMMUNITY_OBJECT_KEY = Pattern.compile("^community/[1-9][0-9]*/[A-Za-z0-9][A-Za-z0-9._-]*$");

    private TiptapContentValidator() {
    }

    public static void validate(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            invalid();
        }
        if (rawJson.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES) {
            invalid();
        }
        Object parsed = new Parser(rawJson).parse();
        if (!(parsed instanceof Map<?, ?> root)) {
            invalid();
        }
        ValidationState state = new ValidationState();
        validateNode(asStringObjectMap(root), 1, state);
    }

    private static void validateNode(Map<String, Object> node, int depth, ValidationState state) {
        if (depth > MAX_DOCUMENT_DEPTH) {
            invalid();
        }
        if (!NODE_KEYS.containsAll(node.keySet())) {
            invalid();
        }
        String type = stringValue(node.get("type"));
        if (!NODE_TYPES.contains(type)) {
            invalid();
        }

        if ("text".equals(type)) {
            validateTextNode(node, state);
            return;
        }
        if (node.containsKey("text") || node.containsKey("marks")) {
            invalid();
        }
        switch (type) {
            case "doc" -> validateNoAttrs(node);
            case "paragraph", "bulletList", "orderedList", "listItem", "blockquote", "codeBlock" -> validateNoAttrs(node);
            case "heading" -> validateHeadingAttrs(node);
            case "hardBreak" -> validateLeafNode(node);
            case "image" -> validateImageNode(node, state);
            default -> invalid();
        }

        if ("hardBreak".equals(type) || "image".equals(type)) {
            return;
        }
        if (node.containsKey("content")) {
            for (Object child : listValue(node.get("content"))) {
                if (!(child instanceof Map<?, ?> childMap)) {
                    invalid();
                }
                validateNode(asStringObjectMap(childMap), depth + 1, state);
            }
        }
    }

    private static void validateTextNode(Map<String, Object> node, ValidationState state) {
        if (!TEXT_NODE_KEYS.containsAll(node.keySet()) || node.containsKey("attrs") || node.containsKey("content")) {
            invalid();
        }
        String text = stringValue(node.get("text"));
        state.textLength += text.codePointCount(0, text.length());
        if (state.textLength > MAX_TEXT_LENGTH) {
            invalid();
        }
        if (node.containsKey("marks")) {
            for (Object mark : listValue(node.get("marks"))) {
                if (!(mark instanceof Map<?, ?> markMap)) {
                    invalid();
                }
                validateMark(asStringObjectMap(markMap));
            }
        }
    }

    private static void validateMark(Map<String, Object> mark) {
        if (!MARK_KEYS.containsAll(mark.keySet())) {
            invalid();
        }
        String type = stringValue(mark.get("type"));
        if (!MARK_TYPES.contains(type)) {
            invalid();
        }
        if ("link".equals(type)) {
            Map<String, Object> attrs = attrsValue(mark.get("attrs"));
            if (!attrs.keySet().equals(LINK_ATTR_KEYS)) {
                invalid();
            }
            String href = stringValue(attrs.get("href"));
            String normalized = href.toLowerCase(Locale.ROOT);
            if (!(normalized.startsWith("http://") || normalized.startsWith("https://"))) {
                invalid();
            }
            return;
        }
        if (mark.containsKey("attrs")) {
            invalid();
        }
    }

    private static void validateNoAttrs(Map<String, Object> node) {
        if (node.containsKey("attrs")) {
            invalid();
        }
    }

    private static void validateHeadingAttrs(Map<String, Object> node) {
        Map<String, Object> attrs = attrsValue(node.get("attrs"));
        if (!attrs.keySet().equals(HEADING_ATTR_KEYS)) {
            invalid();
        }
        int level = intValue(attrs.get("level"));
        if (level < 1 || level > 4) {
            invalid();
        }
    }

    private static void validateLeafNode(Map<String, Object> node) {
        validateNoAttrs(node);
        if (node.containsKey("content")) {
            invalid();
        }
    }

    private static void validateImageNode(Map<String, Object> node, ValidationState state) {
        if (node.containsKey("content")) {
            invalid();
        }
        Map<String, Object> attrs = attrsValue(node.get("attrs"));
        if (!attrs.keySet().equals(IMAGE_ATTR_KEYS)) {
            invalid();
        }
        String objectKey = stringValue(attrs.get("objectKey"));
        String src = stringValue(attrs.get("src"));
        if (!COMMUNITY_OBJECT_KEY.matcher(objectKey).matches()) {
            invalid();
        }
        String normalizedSrc = src.toLowerCase(Locale.ROOT);
        if (!(normalizedSrc.startsWith("http://") || normalizedSrc.startsWith("https://"))) {
            invalid();
        }
        if (!src.endsWith("/" + objectKey)) {
            invalid();
        }
        state.imageCount++;
        if (state.imageCount > MAX_IMAGE_COUNT) {
            invalid();
        }
    }

    private static Map<String, Object> attrsValue(Object value) {
        if (!(value instanceof Map<?, ?> attrs)) {
            invalid();
        }
        return asStringObjectMap(attrs);
    }

    private static List<?> listValue(Object value) {
        if (!(value instanceof List<?> list)) {
            invalid();
        }
        return list;
    }

    private static String stringValue(Object value) {
        if (!(value instanceof String string) || string.isEmpty()) {
            invalid();
        }
        return string;
    }

    private static int intValue(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue && longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
            return longValue.intValue();
        }
        invalid();
        return 0;
    }

    private static Map<String, Object> asStringObjectMap(Map<?, ?> map) {
        Map<String, Object> typed = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                invalid();
            }
            typed.put(key, entry.getValue());
        }
        return typed;
    }

    private static void invalid() {
        throw new CoreException(ErrorCode.INVALID_REQUEST);
    }

    private static final class ValidationState {
        private int textLength;
        private int imageCount;
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue(1);
            skipWhitespace();
            if (index != input.length()) {
                invalid();
            }
            return value;
        }

        private Object parseValue(int depth) {
            if (depth > MAX_DOCUMENT_DEPTH + 5) {
                invalid();
            }
            skipWhitespace();
            if (index >= input.length()) {
                invalid();
            }
            char current = input.charAt(index);
            return switch (current) {
                case '{' -> parseObject(depth + 1);
                case '[' -> parseArray(depth + 1);
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (current == '-' || Character.isDigit(current)) {
                        yield parseNumber();
                    }
                    invalid();
                    yield null;
                }
            };
        }

        private Map<String, Object> parseObject(int depth) {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            do {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                if (object.put(key, parseValue(depth + 1)) != null) {
                    invalid();
                }
                skipWhitespace();
            } while (consume(','));
            expect('}');
            return object;
        }

        private List<Object> parseArray(int depth) {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return array;
            }
            do {
                array.add(parseValue(depth + 1));
                skipWhitespace();
            } while (consume(','));
            expect(']');
            return array;
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < input.length()) {
                char current = input.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current < 0x20) {
                    invalid();
                }
                if (current == '\\') {
                    if (index >= input.length()) {
                        invalid();
                    }
                    char escaped = input.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicodeEscape());
                        default -> invalid();
                    }
                } else {
                    builder.append(current);
                }
            }
            invalid();
            return "";
        }

        private char parseUnicodeEscape() {
            if (index + 4 > input.length()) {
                invalid();
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                char hex = input.charAt(index++);
                int digit = Character.digit(hex, 16);
                if (digit < 0) {
                    invalid();
                }
                value = (value << 4) + digit;
            }
            return (char) value;
        }

        private Object parseNumber() {
            int start = index;
            if (consume('-') && index >= input.length()) {
                invalid();
            }
            if (consume('0')) {
                // leading zero is only valid as the entire integer part.
            } else {
                readDigits();
            }
            boolean fractionalOrExponent = false;
            if (consume('.')) {
                fractionalOrExponent = true;
                readDigits();
            }
            if (index < input.length() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                fractionalOrExponent = true;
                index++;
                if (index < input.length() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                    index++;
                }
                readDigits();
            }
            try {
                String number = input.substring(start, index);
                if (fractionalOrExponent) {
                    return Double.parseDouble(number);
                }
                return Long.parseLong(number);
            } catch (NumberFormatException exception) {
                invalid();
                return null;
            }
        }

        private void readDigits() {
            int start = index;
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (start == index) {
                invalid();
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!input.startsWith(literal, index)) {
                invalid();
            }
            index += literal.length();
            return value;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                invalid();
            }
        }
    }
}
