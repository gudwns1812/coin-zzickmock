package coin.coinzzickmock.feature.community.application.repository;

import coin.coinzzickmock.feature.community.domain.CommunityPostImageIntent;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CommunityImageRepository {
    List<CommunityPostImageIntent> findOwnedAttachable(Long uploaderMemberId, Collection<String> objectKeys);

    List<CommunityPostImageIntent> attachToPost(Long postId, Long uploaderMemberId, Collection<String> objectKeys, Instant attachedAt);
}
