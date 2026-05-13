package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawMemberService {
    private final MemberCredentialRepository memberCredentialRepository;
    private final Clock clock;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Transactional
    public void withdraw(Long actorMemberId, Long requestedMemberId) {
        if (!actorMemberId.equals(requestedMemberId)) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }

        memberCredentialRepository.findActiveByMemberId(actorMemberId)
                .map(memberCredential -> memberCredential.withdraw(Instant.now(clock)))
                .map(memberCredentialRepository::save)
                .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND));
        afterCommitEventPublisher.publish(new WalletBalanceChangedEvent(actorMemberId));
    }
}
