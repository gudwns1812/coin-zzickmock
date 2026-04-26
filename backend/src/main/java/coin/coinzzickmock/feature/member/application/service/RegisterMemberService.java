package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberIdentityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterMemberService {
    private final MemberCredentialRepository memberCredentialRepository;
    private final AccountRepository accountRepository;
    private final MemberPasswordHasher memberPasswordHasher;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Transactional
    public MemberProfileResult register(
            String memberId,
            String rawPassword,
            String memberName,
            String memberEmail,
            String phoneNumber,
            String zipCode,
            String address,
            String addressDetail
    ) {
        String normalizedMemberId = MemberIdentityRules.normalizeMemberId(memberId);
        String normalizedPassword = MemberIdentityRules.validateRawPassword(rawPassword);

        if (memberCredentialRepository.existsByMemberId(normalizedMemberId)) {
            throw new CoreException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        MemberCredential memberCredential = MemberCredential.register(
                normalizedMemberId,
                memberPasswordHasher.hash(normalizedPassword),
                memberName,
                memberEmail,
                phoneNumber,
                zipCode,
                address,
                addressDetail,
                0
        );

        accountRepository.save(TradingAccount.openDefault(
                memberCredential.memberId(),
                memberCredential.memberEmail(),
                memberCredential.memberName()
        ));
        memberCredentialRepository.save(memberCredential);
        afterCommitEventPublisher.publish(new WalletBalanceChangedEvent(memberCredential.memberId()));
        return MemberProfileResult.from(memberCredential);
    }
}
