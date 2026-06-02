package coin.coinzzickmock.feature.community.web.request;

import jakarta.validation.constraints.NotBlank;

public record CommunityCommentCreateRequest(@NotBlank String content) {
}
