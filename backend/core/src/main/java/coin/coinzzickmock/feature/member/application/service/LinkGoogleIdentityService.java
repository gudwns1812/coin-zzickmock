package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.dto.MemberProfileResult;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberOAuthIdentityRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberOAuthPendingLinkRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberIdentityRules;
import coin.coinzzickmock.feature.member.domain.MemberOAuthIdentity;
import coin.coinzzickmock.feature.member.domain.MemberOAuthPendingLink;
import coin.coinzzickmock.feature.member.domain.OAuthProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class LinkGoogleIdentityService {
    private final MemberOAuthPendingLinkRepository memberOAuthPendingLinkRepository;
    private final MemberOAuthIdentityRepository memberOAuthIdentityRepository;
    private final MemberCredentialRepository memberCredentialRepository;
    private final MemberPasswordHasher memberPasswordHasher;
    private final TransactionTemplate transactionTemplate;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;

    public MemberProfileResult linkExisting(String pendingTokenHash, String account, String rawPassword) {
        LinkResult result;
        try {
            result = Objects.requireNonNull(transactionTemplate.execute(status -> linkExistingInTransaction(
                    pendingTokenHash,
                    account,
                    rawPassword
            )));
        } catch (CoreException exception) {
            if (exception.errorCode() == ErrorCode.OAUTH_IDENTITY_ALREADY_LINKED) {
                consumePendingAfterTerminalConflict(pendingTokenHash);
            }
            throw exception;
        }
        if (result.errorCode() != null) {
            throw new CoreException(result.errorCode());
        }
        return result.memberProfile();
    }

    private LinkResult linkExistingInTransaction(String pendingTokenHash, String account, String rawPassword) {
        Instant now = Instant.now(clock);
        MemberOAuthPendingLink pending = loadPending(pendingTokenHash, now);
        String normalizedAccount = MemberIdentityRules.normalizeAccount(account);
        Optional<MemberCredential> matchedMember = memberCredentialRepository.findActiveByAccount(normalizedAccount)
                .filter(candidate -> candidate.passwordHash() != null)
                .filter(candidate -> memberPasswordHasher.matches(rawPassword, candidate.passwordHash()));
        if (matchedMember.isEmpty()) {
            return LinkResult.error(invalidCredentials(pending, now));
        }
        MemberCredential member = matchedMember.get();

        Optional<ErrorCode> linkabilityError = linkabilityError(pending, member.memberId(), now);
        if (linkabilityError.isPresent()) {
            return LinkResult.error(linkabilityError.get());
        }
        memberOAuthIdentityRepository.create(MemberOAuthIdentity.google(
                member.memberId(),
                pending.providerSubject(),
                pending.providerEmail(),
                pending.providerName()
        ));
        memberOAuthPendingLinkRepository.save(pending.consume(now));
        return LinkResult.success(MemberProfileResult.from(member));
    }

    private ErrorCode invalidCredentials(MemberOAuthPendingLink pending, Instant now) {
        MemberOAuthPendingLink failed = memberOAuthPendingLinkRepository.save(pending.recordFailedAttempt(now));
        if (failed.attemptCount() >= MemberOAuthPendingLink.MAX_FAILED_LINK_ATTEMPTS) {
            return ErrorCode.OAUTH_LINK_TOO_MANY_ATTEMPTS;
        }
        return ErrorCode.INVALID_CREDENTIALS;
    }

    private MemberOAuthPendingLink loadPending(String tokenHash, Instant now) {
        MemberOAuthPendingLink pending = memberOAuthPendingLinkRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new CoreException(ErrorCode.OAUTH_ONBOARDING_EXPIRED));
        pending.validateConsumable(now);
        return pending;
    }

    private Optional<ErrorCode> linkabilityError(MemberOAuthPendingLink pending, Long memberId, Instant now) {
        if (memberOAuthIdentityRepository.findByProviderAndProviderSubject(pending.provider(), pending.providerSubject()).isPresent()) {
            memberOAuthPendingLinkRepository.save(pending.consume(now));
            return Optional.of(ErrorCode.OAUTH_IDENTITY_ALREADY_LINKED);
        }
        if (memberOAuthIdentityRepository.existsByMemberIdAndProvider(memberId, OAuthProvider.GOOGLE.value())) {
            memberOAuthPendingLinkRepository.save(pending.consume(now));
            return Optional.of(ErrorCode.OAUTH_IDENTITY_ALREADY_LINKED);
        }
        return Optional.empty();
    }

    private void consumePendingAfterTerminalConflict(String pendingTokenHash) {
        TransactionTemplate terminalTransaction = new TransactionTemplate(transactionManager);
        terminalTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        terminalTransaction.executeWithoutResult(status -> {
            Instant now = Instant.now(clock);
            memberOAuthPendingLinkRepository.findByTokenHashForUpdate(pendingTokenHash)
                    .filter(pending -> pending.consumedAt() == null)
                    .ifPresent(pending -> memberOAuthPendingLinkRepository.save(pending.consume(now)));
        });
    }

    private record LinkResult(MemberProfileResult memberProfile, ErrorCode errorCode) {
        private static LinkResult success(MemberProfileResult memberProfile) {
            return new LinkResult(memberProfile, null);
        }

        private static LinkResult error(ErrorCode errorCode) {
            return new LinkResult(null, errorCode);
        }
    }
}
