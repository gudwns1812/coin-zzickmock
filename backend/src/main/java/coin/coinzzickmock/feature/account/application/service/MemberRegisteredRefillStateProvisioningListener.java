package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.member.application.event.MemberRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberRegisteredRefillStateProvisioningListener {
    private final AccountRefillStateRepository accountRefillStateRepository;
    private final AccountRefillDatePolicy datePolicy;

    @EventListener
    public void onMemberRegistered(MemberRegisteredEvent event) {
        accountRefillStateRepository.provisionDailyStateIfAbsent(event.memberId(), datePolicy.today());
    }
}
