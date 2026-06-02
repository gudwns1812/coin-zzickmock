package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.dto.GoogleSignupProfileCommand;
import coin.coinzzickmock.feature.member.application.dto.MemberProfileResult;
import coin.coinzzickmock.feature.member.application.implement.MemberRegistrationProvisioner;
import coin.coinzzickmock.feature.member.application.repository.MemberOAuthIdentityRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberOAuthPendingLinkRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberOAuthIdentity;
import coin.coinzzickmock.feature.member.domain.MemberOAuthPendingLink;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class CompleteGoogleSignupService {
    private final MemberOAuthPendingLinkRepository memberOAuthPendingLinkRepository;
    private final MemberOAuthIdentityRepository memberOAuthIdentityRepository;
    private final MemberRegistrationProvisioner memberRegistrationProvisioner;
    private final TransactionTemplate transactionTemplate;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;

    public MemberProfileResult complete(String pendingTokenHash, GoogleSignupProfileCommand command) {
        CompletionResult result;
        try {
            result = Objects.requireNonNull(transactionTemplate.execute(
                    status -> completeInTransaction(pendingTokenHash, command)
            ));
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

    private CompletionResult completeInTransaction(String pendingTokenHash, GoogleSignupProfileCommand command) {
        Instant now = Instant.now(clock);
        MemberOAuthPendingLink pending = memberOAuthPendingLinkRepository.findByTokenHashForUpdate(pendingTokenHash)
                .orElseThrow(() -> new CoreException(ErrorCode.OAUTH_ONBOARDING_EXPIRED));
        pending.validateConsumable(now);
        String googleEmail = verifiedGoogleEmail(pending, command);
        if (memberOAuthIdentityRepository.findByProviderAndProviderSubject(pending.provider(), pending.providerSubject()).isPresent()) {
            memberOAuthPendingLinkRepository.save(pending.consume(now));
            return CompletionResult.error(ErrorCode.OAUTH_IDENTITY_ALREADY_LINKED);
        }

        MemberCredential savedMember = memberRegistrationProvisioner.provision(MemberCredential.registerGoogleOnly(
                command.memberName(),
                command.nickname(),
                googleEmail,
                command.phoneNumber(),
                0
        ));
        memberOAuthIdentityRepository.create(MemberOAuthIdentity.google(
                savedMember.memberId(),
                pending.providerSubject(),
                pending.providerEmail(),
                pending.providerName()
        ));
        memberOAuthPendingLinkRepository.save(pending.consume(now));
        return CompletionResult.success(MemberProfileResult.from(savedMember));
    }

    private String verifiedGoogleEmail(MemberOAuthPendingLink pending, GoogleSignupProfileCommand command) {
        String googleEmail = normalizeRequiredEmail(pending.providerEmail());
        String requestedEmail = normalizeRequiredEmail(command.memberEmail());
        if (!googleEmail.equalsIgnoreCase(requestedEmail)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return googleEmail;
    }

    private String normalizeRequiredEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return email.trim();
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

    private record CompletionResult(MemberProfileResult memberProfile, ErrorCode errorCode) {
        private static CompletionResult success(MemberProfileResult memberProfile) {
            return new CompletionResult(memberProfile, null);
        }

        private static CompletionResult error(ErrorCode errorCode) {
            return new CompletionResult(null, errorCode);
        }
    }
}
