package coin.coinzzickmock.feature.community.domain.content;

import java.util.List;
import java.util.Set;

public record TiptapContentPolicy(
        Set<String> approvedImageObjectKeys,
        List<String> allowedImageSrcPrefixes
) {
    public TiptapContentPolicy {
        approvedImageObjectKeys = approvedImageObjectKeys == null ? Set.of() : Set.copyOf(approvedImageObjectKeys);
        allowedImageSrcPrefixes = allowedImageSrcPrefixes == null ? List.of() : List.copyOf(allowedImageSrcPrefixes);
    }

    public static TiptapContentPolicy withoutImages() {
        return new TiptapContentPolicy(Set.of(), List.of());
    }

    public static TiptapContentPolicy withImages(Set<String> approvedImageObjectKeys, List<String> allowedImageSrcPrefixes) {
        return new TiptapContentPolicy(approvedImageObjectKeys, allowedImageSrcPrefixes);
    }

    boolean imageApproved(String objectKey, String src) {
        if (objectKey == null || objectKey.isBlank() || src == null || src.isBlank()) {
            return false;
        }
        if (approvedImageObjectKeys.isEmpty() || !approvedImageObjectKeys.contains(objectKey)) {
            return false;
        }
        if (!objectKey.startsWith("community/")) {
            return false;
        }
        return allowedImageSrcPrefixes.stream().anyMatch(src::startsWith);
    }
}
