package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionCloseOutcome;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.grant.RewardPointGrantProcessor;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.providers.Providers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClosePositionService {
    private static final double TAKER_FEE_RATE = 0.0005;

    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;
    private final Providers providers;
    private final RewardPointGrantProcessor rewardPointGrantProcessor;

    @Transactional
    public ClosePositionResult close(
            String memberId,
            String symbol,
            String positionSide,
            String marginMode,
            double quantity
    ) {
        PositionSnapshot position = positionRepository.findOpenPosition(memberId, symbol, positionSide, marginMode)
                .orElseThrow(() -> new CoreException(ErrorCode.POSITION_NOT_FOUND));

        MarketSnapshot market = loadMarket(symbol);
        PositionCloseOutcome closeOutcome = position.close(quantity, market.markPrice(), market.lastPrice(), TAKER_FEE_RATE);

        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountRepository.save(account.settlePositionClose(
                closeOutcome.realizedPnl(),
                closeOutcome.closeFee(),
                closeOutcome.releasedMargin()
        ));

        if (closeOutcome.remainingPosition() == null) {
            positionRepository.delete(memberId, symbol, positionSide, marginMode);
        } else {
            positionRepository.save(memberId, closeOutcome.remainingPosition());
        }

        RewardPointResult rewardPointResult = rewardPointGrantProcessor.grant(
                new GrantProfitPointCommand(memberId, Math.max(closeOutcome.netRealizedPnl(), 0))
        );

        return new ClosePositionResult(
                symbol,
                closeOutcome.closedQuantity(),
                closeOutcome.netRealizedPnl(),
                rewardPointResult.rewardPoint()
        );
    }

    private MarketSnapshot loadMarket(String symbol) {
        MarketSnapshot snapshot = providers.connector().marketDataGateway().loadMarket(symbol);
        if (snapshot == null) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다: " + symbol);
        }
        return snapshot;
    }
}
