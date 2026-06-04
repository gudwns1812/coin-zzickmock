package coin.coinzzickmock.feature.community.application.dto;

import java.util.Objects;

public record CommunityPostCountDelta(Long postId, long likeDelta, long commentDelta) {
    public CommunityPostCountDelta {
        Objects.requireNonNull(postId, "postId must not be null");
    }

    public boolean hasChanges() {
        return likeDelta != 0 || commentDelta != 0;
    }
}
