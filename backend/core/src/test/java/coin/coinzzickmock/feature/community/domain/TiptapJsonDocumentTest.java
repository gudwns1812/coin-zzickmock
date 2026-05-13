package coin.coinzzickmock.feature.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
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
                new TiptapJsonImagePolicy("community/1/", java.util.List.of("https://cdn.example/community/"))
        );

        assertThat(document.value()).contains("\"type\":\"doc\"");
    }

    @Test
    void rejectsUnknownNodeAndJavascriptLinks() {
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"html","content":[]}]}
                """));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"x","marks":[{"type":"link","attrs":{"href":"javascript:alert(1)"}}]}]}]}
                """));
    }

    @Test
    void rejectsOversizedDepthTextAndImagePolicyViolations() {
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of(deepDocument(21)));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of(textDocument(10_001)));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of(
                imageDocument(11),
                new TiptapJsonImagePolicy("community/1/", java.util.List.of("https://cdn.example/community/"))
        ));
        assertThrows(CoreException.class, () -> TiptapJsonDocument.of(
                """
                {"type":"doc","content":[{"type":"image","attrs":{"src":"https://evil.example/image.webp","objectKey":"community/1/image.webp"}}]}
                """,
                new TiptapJsonImagePolicy("community/1/", java.util.List.of("https://cdn.example/community/"))
        ));
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
}
