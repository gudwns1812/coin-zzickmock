package coin.coinzzickmock.feature.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TiptapJsonDocumentTest {
    @Test
    void acceptsAllowedDocumentWithMarksAndImages() {
        TiptapJsonDocument document = TiptapJsonDocument.of(
                """
                {"type":"doc","content":[
                  {"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"Hello"}]},
                  {"type":"paragraph","content":[
                    {"type":"text","text":"Bold","marks":[{"type":"bold"}]},
                    {"type":"text","text":" link","marks":[{"type":"link","attrs":{"href":"https://example.com"}}]}
                  ]},
                  {"type":"image","attrs":{"src":"https://cdn.example/community/1/image.webp","objectKey":"community/1/image.webp","alt":"chart","title":"chart"}}
                ]}
                """,
                new TiptapJsonImagePolicy(
                        Set.of("community/1/image.webp"),
                        java.util.List.of("https://cdn.example/community/")
                )
        );

        assertThat(document.value()).contains("\"type\":\"doc\"");
    }

    @Test
    void acceptsTiptapEmptyBlockNodesWithoutContentArray() {
        TiptapJsonDocument document = TiptapJsonDocument.of(
                """
                {"type":"doc","content":[
                  {"type":"paragraph"},
                  {"type":"heading","attrs":{"level":2}},
                  {"type":"blockquote"},
                  {"type":"codeBlock"},
                  {"type":"bulletList","content":[{"type":"listItem","content":[{"type":"paragraph"}]}]}
                ]}
                """
        );

        assertThat(document.value()).contains("\"type\":\"paragraph\"");
    }

    @Test
    void restoresPersistedImageDocumentsWithoutUploadPolicy() {
        String storedContent = """
                {"type":"doc","content":[
                  {"type":"image","attrs":{"src":"https://cdn.example/community/1/image.webp","objectKey":"community/1/image.webp","alt":"chart"}}
                ]}
                """;

        assertThatErrorCode(() -> TiptapJsonDocument.of(storedContent), ErrorCode.COMMUNITY_POST_IMAGE_NOT_ATTACHABLE);

        TiptapJsonDocument document = TiptapJsonDocument.restore(storedContent);

        assertThat(document.value()).contains("\"objectKey\":\"community/1/image.webp\"");
    }

    @Test
    void rejectsNullContentAndMissingListContent() {
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"paragraph","content":null}]}
                """));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"bulletList"}]}
                """));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"orderedList"}]}
                """));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"bulletList","content":[{"type":"listItem"}]}]}
                """));
    }

    @Test
    void rejectsUnknownNodeAndJavascriptLinks() {
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"html","content":[]}]}
                """));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"x","marks":[{"type":"link","attrs":{"href":"javascript:alert(1)"}}]}]}]}
                """));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"paragraph","attrs":{"foo":"bar"},"content":[{"type":"text","text":"x"}]}]}
                """));
    }

    @Test
    void rejectsOversizedDepthTextAndImagePolicyViolations() {
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of(deepDocument(21)));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of(textDocument(10_001)));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of(
                imageDocument(11),
                new TiptapJsonImagePolicy(
                        imageKeys(11),
                        java.util.List.of("https://cdn.example/community/")
                )
        ));
        assertThatErrorCode(() -> TiptapJsonDocument.of(
                """
                {"type":"doc","content":[{"type":"image","attrs":{"src":"https://evil.example/image.webp","objectKey":"community/1/image.webp"}}]}
                """,
                new TiptapJsonImagePolicy(
                        Set.of("community/1/image.webp"),
                        java.util.List.of("https://cdn.example/community/")
                )
        ), ErrorCode.COMMUNITY_POST_IMAGE_NOT_ATTACHABLE);
        assertThatErrorCode(() -> TiptapJsonDocument.of(
                """
                {"type":"doc","content":[{"type":"image","attrs":{"src":"https://cdn.example/community/2/image.webp","objectKey":"community/2/image.webp"}}]}
                """,
                new TiptapJsonImagePolicy(
                        Set.of("community/1/image.webp"),
                        java.util.List.of("https://cdn.example/community/")
                )
        ), ErrorCode.COMMUNITY_POST_IMAGE_NOT_ATTACHABLE);
    }

    private String deepDocument(int depth) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            builder.append("{\"type\":\"blockquote\",\"content\":[");
        }
        builder.append("{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"x\"}]}");
        for (int i = 0; i < depth; i++) {
            builder.append("]}");
        }
        return "{\"type\":\"doc\",\"content\":[" + builder + "]}";
    }

    private String textDocument(int length) {
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\""
                + "x".repeat(length)
                + "\"}]}]}";
    }

    private String imageDocument(int count) {
        StringBuilder builder = new StringBuilder("{\"type\":\"doc\",\"content\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append("{\"type\":\"image\",\"attrs\":{\"src\":\"https://cdn.example/community/1/image")
                    .append(i)
                    .append(".webp\",\"objectKey\":\"community/1/image")
                    .append(i)
                    .append(".webp\"}}");
        }
        builder.append("]}");
        return builder.toString();
    }

    private Set<String> imageKeys(int count) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (int i = 0; i < count; i++) {
            keys.add("community/1/image" + i + ".webp");
        }
        return keys;
    }

    private static void assertThatErrorCode(Runnable runnable, ErrorCode errorCode) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(CoreException.class)
                .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(errorCode));
    }
}
