package coin.coinzzickmock.feature.community.domain.content;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.util.Set;

public record TiptapContentValidationResult(int textLength, int imageCount, Set<String> imageObjectKeys) {
    public TiptapContentValidationResult {
        if (textLength < 0 || imageCount < 0) {
            throw invalid();
        }
        imageObjectKeys = imageObjectKeys == null ? Set.of() : Set.copyOf(imageObjectKeys);
        if (imageObjectKeys.size() > imageCount) {
            throw invalid();
        }
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
