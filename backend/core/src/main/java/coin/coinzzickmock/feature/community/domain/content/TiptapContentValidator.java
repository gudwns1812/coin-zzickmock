package coin.coinzzickmock.feature.community.domain.content;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TiptapContentValidator {
    public static final int MAX_JSON_BYTES = 256 * 1024;
    public static final int MAX_TEXT_LENGTH = 10_000;
    public static final int MAX_DEPTH = 20;
    public static final int MAX_IMAGE_COUNT = 10;

    private static final Set<String> NODE_TYPES = Set.of(
            "doc", "paragraph", "text", "heading", "bulletList", "orderedList", "listItem",
            "blockquote", "hardBreak", "codeBlock", "image"
    );
    private static final Set<String> MARK_TYPES = Set.of("bold", "italic", "code", "link");

    private TiptapContentValidator() {
    }

    public static Validation validate(String json, TiptapContentPolicy policy) {
        if (json == null || json.isBlank() || json.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES) {
            throw invalid();
        }
        Object parsed = new TiptapJsonReader(json).read();
        if (!(parsed instanceof Map<?, ?> document)) {
            throw invalid();
        }
        Object parsed = new Parser(rawJson).parse();
        if (!(parsed instanceof Map<?, ?>)) {
            invalid();
        }
        ValidationState state = new ValidationState();
        validateNode(asStringObjectMap((Map<?, ?>) parsed), 1, state);
    }

    private static void validateNode(Map<?, ?> node, String expectedType, int depth, State state) {
        if (depth > MAX_DEPTH) {
            throw invalid();
        }
        String type = stringField(node, "type", true);
        if (!NODE_TYPES.contains(type) || (expectedType != null && !expectedType.equals(type))) {
            throw invalid();
        }
        validateNodeKeys(node, type);
        validateMarks(node, type);

        switch (type) {
            case "doc" -> validateChildren(node, null, depth, state, true);
            case "paragraph", "blockquote", "listItem" -> validateChildren(node, null, depth, state, false);
            case "heading" -> {
                validateHeadingAttrs(node.get("attrs"));
                validateChildren(node, null, depth, state, false);
            }
            case "bulletList", "orderedList" -> validateChildren(node, "listItem", depth, state, false);
            case "codeBlock" -> validateChildren(node, "text", depth, state, false);
            case "text" -> validateTextNode(node, state);
            case "hardBreak" -> validateLeafWithoutAttrs(node);
            case "image" -> validateImageNode(node, state);
            default -> throw invalid();
        }
    }

    private static void validateNodeKeys(Map<?, ?> node, String type) {
        Set<String> allowed = new HashSet<>(Set.of("type", "content", "marks"));
        if ("text".equals(type)) {
            allowed.add("text");
        }
        if (node.containsKey("content")) {
            for (Object child : listValue(node.get("content"))) {
                if (!(child instanceof Map<?, ?>)) {
                    invalid();
                }
                validateNode(asStringObjectMap((Map<?, ?>) child), depth + 1, state);
            }
        }
    }

    private static void validateChildren(Map<?, ?> node, String expectedType, int depth, State state, boolean doc) {
        Object content = node.get("content");
        if (content == null) {
            if (doc) {
                throw invalid();
            }
            return;
        }
        if (!(content instanceof List<?> children)) {
            throw invalid();
        }
        for (Object child : children) {
            if (!(child instanceof Map<?, ?> childNode)) {
                throw invalid();
            }
            validateNode(childNode, expectedType, depth + 1, state);
        }
    }

    private static void validateTextNode(Map<?, ?> node, State state) {
        if (node.containsKey("content") || node.containsKey("attrs")) {
            throw invalid();
        }
        String text = stringField(node, "text", true);
        state.textLength += text.length();
        if (state.textLength > MAX_TEXT_LENGTH) {
            invalid();
        }
        if (node.containsKey("marks")) {
            for (Object mark : listValue(node.get("marks"))) {
                if (!(mark instanceof Map<?, ?>)) {
                    invalid();
                }
                validateMark(asStringObjectMap((Map<?, ?>) mark));
            }
        }
    }

    private static void validateLeafWithoutAttrs(Map<?, ?> node) {
        if (node.containsKey("content") || node.containsKey("text") || node.containsKey("attrs") || node.containsKey("marks")) {
            throw invalid();
        }
    }

    private static void validateImageNode(Map<?, ?> node, State state) {
        if (node.containsKey("content") || node.containsKey("marks") || node.containsKey("text")) {
            throw invalid();
        }
        Object attrs = node.get("attrs");
        if (!(attrs instanceof Map<?, ?> attrMap)) {
            throw invalid();
        }
        if (!attrMap.keySet().equals(Set.of("objectKey", "src"))) {
            throw invalid();
        }
        String objectKey = stringField(attrMap, "objectKey", true);
        String src = stringField(attrMap, "src", true);
        if (!state.policy.imageApproved(objectKey, src)) {
            throw invalid();
        }
        state.imageCount++;
        if (state.imageCount > MAX_IMAGE_COUNT) {
            throw invalid();
        }
        state.imageObjectKeys.add(objectKey);
    }

    private static void validateHeadingAttrs(Object attrs) {
        if (!(attrs instanceof Map<?, ?> attrMap) || !attrMap.keySet().equals(Set.of("level"))) {
            throw invalid();
        }
        Object level = attrMap.get("level");
        if (!(level instanceof Integer levelValue) || levelValue < 1 || levelValue > 4) {
            throw invalid();
        }
    }

    private static Map<String, Object> attrsValue(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            invalid();
        }
        return asStringObjectMap((Map<?, ?>) value);
    }

    private static List<?> listValue(Object value) {
        if (!(value instanceof List<?>)) {
            invalid();
        }
        return (List<?>) value;
    }

    private static String stringValue(Object value) {
        if (!(value instanceof String) || ((String) value).isEmpty()) {
            invalid();
        }
        return (String) value;
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
            if (!(entry.getKey() instanceof String)) {
                invalid();
            }
            typed.put((String) entry.getKey(), entry.getValue());
        }
    }

    private static void validateMarkKeys(Map<?, ?> mark, String type) {
        Set<String> allowed = "link".equals(type) ? Set.of("type", "attrs") : Set.of("type");
        for (Object key : mark.keySet()) {
            if (!(key instanceof String keyText) || !allowed.contains(keyText)) {
                throw invalid();
            }
        }
    }

    private static void validateLinkAttrs(Object attrs) {
        if (!(attrs instanceof Map<?, ?> attrMap) || !attrMap.keySet().equals(Set.of("href"))) {
            throw invalid();
        }
        String href = stringField(attrMap, "href", true).trim().toLowerCase();
        if (!(href.startsWith("http://") || href.startsWith("https://"))) {
            throw invalid();
        }
    }

    private static String stringField(Map<?, ?> map, String field, boolean required) {
        Object value = map.get(field);
        if (value == null) {
            if (required) {
                throw invalid();
            }
            return null;
        }
        if (!(value instanceof String text)) {
            throw invalid();
        }
        return text;
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }

    public record Validation(String json, TiptapContentValidationResult result) {
    }

    private static final class State {
        private final TiptapContentPolicy policy;
        private final Set<String> imageObjectKeys = new HashSet<>();
        private int textLength;
        private int imageCount;

        private State(TiptapContentPolicy policy) {
            this.policy = policy;
        }
    }
}
