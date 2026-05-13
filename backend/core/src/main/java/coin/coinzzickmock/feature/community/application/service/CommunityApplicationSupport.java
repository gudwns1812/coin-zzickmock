package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.repository.CommunityImageRepository;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageIntent;
import coin.coinzzickmock.feature.community.domain.TiptapJsonDocument;
import coin.coinzzickmock.feature.community.domain.TiptapJsonImagePolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CommunityApplicationSupport {
    private CommunityApplicationSupport() {
    }

    static CoreException notFound() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }

    static CoreException forbidden() {
        return new CoreException(ErrorCode.FORBIDDEN);
    }

    static TiptapJsonDocument content(
            Long memberId,
            String contentJson,
            List<String> imageObjectKeys,
            List<String> allowedImageSrcPrefixes,
            CommunityImageRepository imageRepository
    ) {
        List<String> keys = imageObjectKeys == null ? List.of() : List.copyOf(imageObjectKeys);
        if (keys.isEmpty()) {
            return TiptapJsonDocument.of(contentJson);
        }
        validateOwnedAttachableImages(memberId, keys, imageRepository);
        return TiptapJsonDocument.of(contentJson, new TiptapJsonImagePolicy("community/" + memberId + "/", allowedImageSrcPrefixes));
    }

    static void validateOwnedAttachableImages(Long memberId, List<String> objectKeys, CommunityImageRepository imageRepository) {
        Set<String> requested = new HashSet<>(objectKeys == null ? List.of() : objectKeys);
        if (requested.isEmpty()) {
            return;
        }
        List<CommunityPostImageIntent> images = imageRepository.findOwnedAttachable(memberId, requested);
        Set<String> found = images.stream()
                .filter(image -> image.attachableBy(memberId))
                .map(CommunityPostImageIntent::objectKey)
                .collect(java.util.stream.Collectors.toSet());
        if (!found.equals(requested)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
