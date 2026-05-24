package coin.coinzzickmock.feature.member.application.implement;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.event.TradingAccountOpenedEvent;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.service.AccountRefillDatePolicy;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberRegistrationProvisioner {
    private final MemberCredentialRepository memberCredentialRepository;
    private final AccountRepository accountRepository;
    private final AccountRefillStateRepository accountRefillStateRepository;
    private final AccountRefillDatePolicy accountRefillDatePolicy;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Transactional
    public MemberCredential provision(MemberCredential memberCredential) {
        MemberCredential savedMemberCredential = memberCredentialRepository.create(memberCredential);
        TradingAccount account = accountRepository.create(TradingAccount.openDefault(
                savedMemberCredential.memberId(),
                savedMemberCredential.memberEmail(),
                savedMemberCredential.memberName()
        ));
        accountRefillStateRepository.provisionWeeklyStateIfAbsent(
                savedMemberCredential.memberId(),
                accountRefillDatePolicy.currentRefillDate()
        );
        afterCommitEventPublisher.publish(TradingAccountOpenedEvent.from(
                account,
                savedMemberCredential.nickname()
        ));
        return savedMemberCredential;
    }
}
