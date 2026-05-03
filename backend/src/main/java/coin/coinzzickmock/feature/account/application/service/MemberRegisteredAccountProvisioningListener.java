package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.feature.member.application.event.MemberRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberRegisteredAccountProvisioningListener {
    private final TradingAccountProvisioningService tradingAccountProvisioningService;

    @EventListener
    public void onMemberRegistered(MemberRegisteredEvent event) {
        tradingAccountProvisioningService.openForRegisteredMember(
                event.memberId(),
                event.memberEmail(),
                event.memberName()
        );
    }
}
