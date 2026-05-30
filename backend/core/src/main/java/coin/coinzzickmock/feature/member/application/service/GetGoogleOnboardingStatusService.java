package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.dto.GoogleOnboardingStatusResult;
import coin.coinzzickmock.feature.member.application.repository.MemberOAuthPendingLinkRepository;
import coin.coinzzickmock.feature.member.domain.MemberOAuthPendingLink;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetGoogleOnboardingStatusService {
    private final MemberOAuthPendingLinkRepository memberOAuthPendingLinkRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public GoogleOnboardingStatusResult get(String pendingTokenHash) {
        MemberOAuthPendingLink pendingLink = memberOAuthPendingLinkRepository.findByTokenHash(pendingTokenHash)
                .orElseThrow(() -> new CoreException(ErrorCode.OAUTH_ONBOARDING_EXPIRED));
        pendingLink.validateConsumable(Instant.now(clock));
        return GoogleOnboardingStatusResult.from(pendingLink);
    }
}
