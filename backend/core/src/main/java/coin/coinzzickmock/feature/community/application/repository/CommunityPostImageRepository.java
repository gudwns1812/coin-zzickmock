package coin.coinzzickmock.feature.community.application.repository;

import coin.coinzzickmock.feature.community.domain.CommunityImageStatus;
import coin.coinzzickmock.feature.community.domain.CommunityPostImageIntent;
import java.util.Set;

public interface CommunityPostImageRepository {
    Set<String> findAttachableObjectKeys(Long uploaderMemberId, Set<String> objectKeys);

    void attachToPost(Long postId, Long uploaderMemberId, Set<String> objectKeys, CommunityImageStatus status);

    void detachMissingImages(Long postId, Set<String> retainedObjectKeys, CommunityImageStatus status);

    void saveImageIntent(CommunityPostImageIntent intent);
}
