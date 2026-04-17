package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.feature.account.infrastructure.persistence.TradingAccountEntityRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberDataCleaner;
import coin.coinzzickmock.feature.order.infrastructure.persistence.FuturesOrderEntityRepository;
import coin.coinzzickmock.feature.position.infrastructure.persistence.OpenPositionEntityRepository;
import coin.coinzzickmock.feature.reward.infrastructure.persistence.RewardPointWalletEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberDataCleanerImpl implements MemberDataCleaner {
    private final MemberCredentialEntityRepository memberCredentialEntityRepository;
    private final RewardPointWalletEntityRepository rewardPointWalletEntityRepository;
    private final OpenPositionEntityRepository openPositionEntityRepository;
    private final FuturesOrderEntityRepository futuresOrderEntityRepository;
    private final TradingAccountEntityRepository tradingAccountEntityRepository;

    @Override
    @Transactional
    public void deleteAllByMemberId(String memberId) {
        memberCredentialEntityRepository.deleteAllByMemberId(memberId);
        rewardPointWalletEntityRepository.deleteAllByMemberId(memberId);
        openPositionEntityRepository.deleteAllByMemberId(memberId);
        futuresOrderEntityRepository.deleteAllByMemberId(memberId);
        tradingAccountEntityRepository.deleteAllByMemberId(memberId);
    }
}
