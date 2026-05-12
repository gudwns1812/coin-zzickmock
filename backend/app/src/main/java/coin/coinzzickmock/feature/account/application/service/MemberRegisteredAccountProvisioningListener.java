package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.event.MemberRegisteredEvent;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberRegisteredAccountProvisioningListener {
    private final MemberCredentialRepository memberCredentialRepository;
    private final TradingAccountProvisioningService tradingAccountProvisioningService;

    @EventListener
    public void onMemberRegistered(MemberRegisteredEvent event) {
        MemberCredential member = memberCredentialRepository.findActiveByMemberId(event.memberId())
                .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND));
        // Intentionally synchronous: registration must not commit without its default trading account.
        tradingAccountProvisioningService.openForRegisteredMember(
                member.memberId(),
                member.memberEmail(),
                member.memberName()
        );
    }
}
