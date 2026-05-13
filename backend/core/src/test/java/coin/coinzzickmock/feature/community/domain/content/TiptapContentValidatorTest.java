package coin.coinzzickmock.feature.community.domain.content;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TiptapContentValidatorTest {
    @Test
    void acceptsAllowedDocumentShapeNodesMarksLinksAndOwnedImages() {
        String json = """
                {
                  "type": "doc",
                  "content": [
                    {"type": "heading", "attrs": {"level": 2}, "content": [{"type": "text", "text": "Chart note"}]},
                    {"type": "paragraph", "content": [{"type": "text", "text": "read docs", "marks": [
                      {"type": "bold"},
                      {"type": "italic"},
                      {"type": "code"},
                      {"type": "link", "attrs": {"href": "https://example.com/post"}}
                    ]}]},
                    {"type": "bulletList", "content": [{"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "item"}]}]}]},
                    {"type": "orderedList", "content": [{"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "one"}]}]}]},
                    {"type": "blockquote", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "quote"}]}]},
                    {"type": "codeBlock", "content": [{"type": "text", "text": "const x = 1;"}]},
                    {"type": "paragraph", "content": [{"type": "text", "text": "line"}, {"type": "hardBreak"}, {"type": "text", "text": "break"}]},
                    {"type": "image", "attrs": {"objectKey": "community/7/chart.webp", "src": "https://cdn.example/community/7/chart.webp"}}
                  ]
                }
                """;

        assertThatCode(() -> TiptapContentJson.of(json, TiptapContentPolicy.withImages(java.util.Set.of("community/7/chart.webp"), java.util.List.of("https://cdn.example/community/")))).doesNotThrowAnyException();
    }

    @Test
    void rejectsRawHtmlInsteadOfJsonDocument() {
        assertInvalid("<p>raw html</p>");
    }

    @Test
    void rejectsUnknownNodeType() {
        assertInvalid(doc("{\"type\":\"script\",\"content\":[{\"type\":\"text\",\"text\":\"x\"}]}"));
    }

    @Test
    void rejectsUnknownNodeAttributes() {
        assertInvalid(doc("{\"type\":\"paragraph\",\"attrs\":{\"class\":\"danger\"},\"content\":[{\"type\":\"text\",\"text\":\"x\"}]}"));
    }

    @Test
    void rejectsUnknownMarkTypeAndAttributes() {
        assertInvalid(doc(paragraphTextWithMarks("x", "[{\"type\":\"underline\"}]")));
        assertInvalid(doc(paragraphTextWithMarks("x", "[{\"type\":\"bold\",\"attrs\":{\"class\":\"x\"}}]")));
    }

    @Test
    void rejectsJavascriptLinks() {
        assertInvalid(doc(paragraphTextWithMarks("click", "[{\"type\":\"link\",\"attrs\":{\"href\":\"javascript:alert(1)\"}}]")));
    }

    @Test
    void rejectsExternalOrUnapprovedImageObjectKey() {
        assertInvalid(doc("{\"type\":\"image\",\"attrs\":{\"objectKey\":\"https://evil.example/a.webp\",\"src\":\"https://evil.example/a.webp\"}}"));
        assertInvalid(doc("{\"type\":\"image\",\"attrs\":{\"objectKey\":\"community/7/chart.webp\",\"src\":\"https://evil.example/other/chart.webp\"}}"));
    }

    @Test
    void rejectsOversizeJson() {
        String tooLarge = "x".repeat(TiptapContentValidator.MAX_JSON_BYTES + 1);
        assertInvalid(doc(paragraphText(tooLarge)));
    }

    @Test
    void rejectsDocumentDepthAboveLimit() {
        String nested = paragraphText("deep");
        for (int i = 0; i < TiptapContentValidator.MAX_DEPTH; i++) {
            nested = "{\"type\":\"blockquote\",\"content\":[" + nested + "]}";
        }
        assertInvalid(doc(nested));
    }

    @Test
    void rejectsTextLengthAboveLimit() {
        assertInvalid(doc(paragraphText("가".repeat(TiptapContentValidator.MAX_TEXT_LENGTH + 1))));
    }

    @Test
    void rejectsImageCountAboveLimit() {
        String images = IntStream.rangeClosed(1, TiptapContentValidator.MAX_IMAGE_COUNT + 1)
                .mapToObj(index -> "{\"type\":\"image\",\"attrs\":{\"objectKey\":\"community/7/chart" + index + ".webp\",\"src\":\"https://cdn.example/community/7/chart" + index + ".webp\"}}")
                .collect(Collectors.joining(","));
        assertInvalid("{\"type\":\"doc\",\"content\":[" + images + "]}");
    }

    private static String doc(String child) {
        return "{\"type\":\"doc\",\"content\":[" + child + "]}";
    }

    private static String paragraphText(String text) {
        return "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\"}]}";
    }

    private static String paragraphTextWithMarks(String text, String marks) {
        return "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\",\"marks\":" + marks + "}]}";
    }

    private static void assertInvalid(String json) {
        assertThatThrownBy(() -> TiptapContentValidator.validate(json, TiptapContentPolicy.withoutImages()))
                .isInstanceOf(CoreException.class)
                .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
