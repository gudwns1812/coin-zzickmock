package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.feature.account.infrastructure.persistence.TradingAccountSpringDataRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberDataCleaner;
import coin.coinzzickmock.feature.order.infrastructure.persistence.FuturesOrderSpringDataRepository;
import coin.coinzzickmock.feature.position.infrastructure.persistence.OpenPositionSpringDataRepository;
import coin.coinzzickmock.feature.reward.infrastructure.persistence.RewardPointWalletSpringDataRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MemberDataCleanerImpl implements MemberDataCleaner {
    private final MemberCredentialSpringDataRepository memberCredentialSpringDataRepository;
    private final RewardPointWalletSpringDataRepository rewardPointWalletSpringDataRepository;
    private final OpenPositionSpringDataRepository openPositionSpringDataRepository;
    private final FuturesOrderSpringDataRepository futuresOrderSpringDataRepository;
    private final TradingAccountSpringDataRepository tradingAccountSpringDataRepository;

    public MemberDataCleanerImpl(
            MemberCredentialSpringDataRepository memberCredentialSpringDataRepository,
            RewardPointWalletSpringDataRepository rewardPointWalletSpringDataRepository,
            OpenPositionSpringDataRepository openPositionSpringDataRepository,
            FuturesOrderSpringDataRepository futuresOrderSpringDataRepository,
            TradingAccountSpringDataRepository tradingAccountSpringDataRepository
    ) {
        this.memberCredentialSpringDataRepository = memberCredentialSpringDataRepository;
        this.rewardPointWalletSpringDataRepository = rewardPointWalletSpringDataRepository;
        this.openPositionSpringDataRepository = openPositionSpringDataRepository;
        this.futuresOrderSpringDataRepository = futuresOrderSpringDataRepository;
        this.tradingAccountSpringDataRepository = tradingAccountSpringDataRepository;
    }

    @Override
    @Transactional
    public void deleteAllByMemberId(String memberId) {
        memberCredentialSpringDataRepository.deleteAllByMemberId(memberId);
        rewardPointWalletSpringDataRepository.deleteAllByMemberId(memberId);
        openPositionSpringDataRepository.deleteAllByMemberId(memberId);
        futuresOrderSpringDataRepository.deleteAllByMemberId(memberId);
        tradingAccountSpringDataRepository.deleteAllByMemberId(memberId);
    }
}
