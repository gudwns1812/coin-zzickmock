package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.dto.GoogleOAuthLoginResolution;
import coin.coinzzickmock.feature.member.application.dto.GoogleOAuthProfile;
import coin.coinzzickmock.feature.member.application.dto.MemberProfileResult;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberOAuthIdentityRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberOAuthPendingLinkRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberOAuthPendingLink;
import coin.coinzzickmock.feature.member.domain.OAuthProvider;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResolveGoogleOAuthLoginService {
    private final MemberOAuthIdentityRepository memberOAuthIdentityRepository;
    private final MemberOAuthPendingLinkRepository memberOAuthPendingLinkRepository;
    private final MemberCredentialRepository memberCredentialRepository;

    @Transactional
    public GoogleOAuthLoginResolution resolve(GoogleOAuthProfile profile, String pendingTokenHash, Instant expiresAt) {
        return memberOAuthIdentityRepository
                .findByProviderAndProviderSubject(OAuthProvider.GOOGLE.value(), profile.subject())
                .map(identity -> linkedMember(identity.memberId()))
                .orElseGet(() -> createPending(profile, pendingTokenHash, expiresAt));
    }

    private GoogleOAuthLoginResolution linkedMember(Long memberId) {
        MemberCredential member = memberCredentialRepository.findActiveByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND));
        return GoogleOAuthLoginResolution.linked(MemberProfileResult.from(member));
    }

    private GoogleOAuthLoginResolution createPending(GoogleOAuthProfile profile, String pendingTokenHash, Instant expiresAt) {
        memberOAuthPendingLinkRepository.create(MemberOAuthPendingLink.create(
                pendingTokenHash,
                OAuthProvider.GOOGLE.value(),
                profile.subject(),
                profile.email(),
                profile.name(),
                expiresAt
        ));
        return GoogleOAuthLoginResolution.needsOnboarding();
    }
}
