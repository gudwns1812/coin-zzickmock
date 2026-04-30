package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.feature.account.infrastructure.persistence.TradingAccountEntityRepository;
import coin.coinzzickmock.feature.account.infrastructure.persistence.WalletHistoryEntityRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberDataCleaner;
import coin.coinzzickmock.feature.order.infrastructure.persistence.FuturesOrderEntityRepository;
import coin.coinzzickmock.feature.position.infrastructure.persistence.OpenPositionEntityRepository;
import coin.coinzzickmock.feature.position.infrastructure.persistence.PositionHistoryEntityRepository;
import coin.coinzzickmock.feature.reward.infrastructure.persistence.RewardPointHistoryEntityRepository;
import coin.coinzzickmock.feature.reward.infrastructure.persistence.RewardPointWalletEntityRepository;
import coin.coinzzickmock.feature.reward.infrastructure.persistence.RewardRedemptionRequestEntityRepository;
import coin.coinzzickmock.feature.reward.infrastructure.persistence.RewardShopMemberItemUsageEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberDataCleanerImpl implements MemberDataCleaner {
    private final MemberCredentialEntityRepository memberCredentialEntityRepository;
    private final RewardPointWalletEntityRepository rewardPointWalletEntityRepository;
    private final RewardPointHistoryEntityRepository rewardPointHistoryEntityRepository;
    private final RewardRedemptionRequestEntityRepository rewardRedemptionRequestEntityRepository;
    private final RewardShopMemberItemUsageEntityRepository rewardShopMemberItemUsageEntityRepository;
    private final OpenPositionEntityRepository openPositionEntityRepository;
    private final PositionHistoryEntityRepository positionHistoryEntityRepository;
    private final FuturesOrderEntityRepository futuresOrderEntityRepository;
    private final WalletHistoryEntityRepository walletHistoryEntityRepository;
    private final TradingAccountEntityRepository tradingAccountEntityRepository;

    @Override
    @Transactional
    public void deleteAllByMemberId(Long memberId) {
        rewardRedemptionRequestEntityRepository.clearAdminMemberId(memberId);
        rewardRedemptionRequestEntityRepository.deleteAllByMemberId(memberId);
        rewardShopMemberItemUsageEntityRepository.deleteAllByMemberId(memberId);
        rewardPointHistoryEntityRepository.deleteAllByMemberId(memberId);
        openPositionEntityRepository.deleteAllByMemberId(memberId);
        positionHistoryEntityRepository.deleteAllByMemberId(memberId);
        futuresOrderEntityRepository.deleteAllByMemberId(memberId);
        walletHistoryEntityRepository.deleteAllByMemberId(memberId);
        tradingAccountEntityRepository.deleteAllByMemberId(memberId);
        rewardPointWalletEntityRepository.deleteAllByMemberId(memberId);
        memberCredentialEntityRepository.deleteById(memberId);
    }
}
