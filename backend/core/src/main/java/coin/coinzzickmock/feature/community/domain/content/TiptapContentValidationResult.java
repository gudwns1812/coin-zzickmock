package coin.coinzzickmock.feature.community.domain.content;

import java.util.Set;

public record TiptapContentValidationResult(int textLength, int imageCount, Set<String> imageObjectKeys) {
    public TiptapContentValidationResult {
        if (textLength < 0 || imageCount < 0) {
            throw new IllegalArgumentException("Tiptap content metrics must not be negative");
        }
        imageObjectKeys = imageObjectKeys == null ? Set.of() : Set.copyOf(imageObjectKeys);
        if (imageObjectKeys.size() > imageCount) {
            throw new IllegalArgumentException("Image object keys must not exceed image count");
        }
    }
}
