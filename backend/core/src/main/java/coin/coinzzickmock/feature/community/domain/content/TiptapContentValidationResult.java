package coin.coinzzickmock.feature.community.domain.content;

import java.util.Set;

public record TiptapContentValidationResult(int textLength, int imageCount, Set<String> imageObjectKeys) {
    public TiptapContentValidationResult {
        imageObjectKeys = imageObjectKeys == null ? Set.of() : Set.copyOf(imageObjectKeys);
    }
}
