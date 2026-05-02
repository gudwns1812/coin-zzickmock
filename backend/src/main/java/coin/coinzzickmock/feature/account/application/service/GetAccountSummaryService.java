package coin.coinzzickmock.feature.account.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.account.application.query.GetAccountSummaryQuery;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountSummaryResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAccountSummaryService {
    private final AccountRepository accountRepository;
    private final RewardPointRepository rewardPointRepository;
    private final PositionRepository positionRepository;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final MemberCredentialRepository memberCredentialRepository;

    @Transactional(readOnly = true)
    public AccountSummaryResult execute(GetAccountSummaryQuery query) {
        TradingAccount account = accountRepository.findByMemberId(query.memberId())
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        MemberCredential member = memberCredentialRepository.findActiveByMemberId(query.memberId())
                .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND));
        RewardPointWallet rewardPointWallet = rewardPointRepository.findByMemberId(query.memberId())
                .orElse(RewardPointWallet.empty(query.memberId()));
        List<PositionSnapshot> positions = positionRepository.findOpenPositions(query.memberId()).stream()
                .map(this::markToMarketForRead)
                .toList();
        double totalUnrealizedPnl = positions.stream()
                .mapToDouble(PositionSnapshot::unrealizedPnl)
                .sum();
        double totalOpenInitialMargin = positions.stream()
                .mapToDouble(PositionSnapshot::initialMargin)
                .sum();
        double roi = totalOpenInitialMargin == 0 ? 0 : totalUnrealizedPnl / totalOpenInitialMargin;

        return new AccountSummaryResult(
                account.memberId(),
                member.account(),
                account.memberName(),
                member.nickname(),
                account.walletBalance() + totalUnrealizedPnl,
                account.walletBalance(),
                account.availableMargin(),
                totalUnrealizedPnl,
                roi,
                rewardPointWallet.rewardPoint()
        );
    }

    private PositionSnapshot markToMarketForRead(PositionSnapshot snapshot) {
        return realtimeMarketPriceReader.freshMarkPrice(snapshot.symbol())
                .map(snapshot::markToMarket)
                .orElse(snapshot);
    }
}
