package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.command.CreateCommunityPostCommand;
import coin.coinzzickmock.feature.community.application.repository.CommunityImageRepository;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import coin.coinzzickmock.feature.community.application.result.CommunityPostResult;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCommunityPostService {
    private final CommunityPostRepository communityPostRepository;
    private final CommunityImageRepository communityImageRepository;
    private final Clock clock;

    @Transactional
    public CommunityPostResult create(CreateCommunityPostCommand command) {
        if (!CommunityPermissionPolicy.canCreatePost(command.actorAdmin(), command.category())) {
            throw CommunityApplicationSupport.forbidden();
        }
        Instant now = Instant.now(clock);
        var content = CommunityApplicationSupport.content(command.actorMemberId(), command.contentJson(),
                command.imageObjectKeys(), command.allowedImageSrcPrefixes(), communityImageRepository);
        CommunityPost saved = communityPostRepository.save(CommunityPost.create(
                command.actorMemberId(), command.authorNickname(), command.category(), command.title(), content, now));
        communityImageRepository.attachToPost(saved.id(), command.actorMemberId(), command.imageObjectKeys(), now);
        return CommunityPostResult.from(saved, command.actorMemberId(), command.actorAdmin(), false);
    }
}
