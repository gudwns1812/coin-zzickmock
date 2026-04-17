package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
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

        double closeQuantity = Math.min(quantity, position.quantity());
        MarketSnapshot market = loadMarket(symbol);
        double realizedPnl = realizedPnl(position, market.markPrice(), closeQuantity);
        double closeFee = market.lastPrice() * closeQuantity * TAKER_FEE_RATE;
        double releasedMargin = (position.entryPrice() * closeQuantity) / position.leverage();

        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));

        accountRepository.save(new TradingAccount(
                account.memberId(),
                account.memberEmail(),
                account.memberName(),
                account.walletBalance() + realizedPnl - closeFee,
                account.availableMargin() + releasedMargin + realizedPnl - closeFee
        ));

        double remainingQuantity = position.quantity() - closeQuantity;
        if (remainingQuantity <= 0) {
            positionRepository.delete(memberId, symbol, positionSide, marginMode);
        } else {
            positionRepository.save(memberId, new PositionSnapshot(
                    position.symbol(),
                    position.positionSide(),
                    position.marginMode(),
                    position.leverage(),
                    remainingQuantity,
                    position.entryPrice(),
                    market.markPrice(),
                    position.liquidationPrice(),
                    realizedPnl(position, market.markPrice(), remainingQuantity)
            ));
        }

        RewardPointResult rewardPointResult = rewardPointGrantProcessor.grant(
                new GrantProfitPointCommand(memberId, Math.max(realizedPnl - closeFee, 0))
        );

        return new ClosePositionResult(
                symbol,
                closeQuantity,
                realizedPnl - closeFee,
                rewardPointResult.rewardPoint()
        );
    }

    private double realizedPnl(PositionSnapshot position, double markPrice, double quantity) {
        if ("LONG".equalsIgnoreCase(position.positionSide())) {
            return (markPrice - position.entryPrice()) * quantity;
        }
        return (position.entryPrice() - markPrice) * quantity;
    }

    private MarketSnapshot loadMarket(String symbol) {
        MarketSnapshot snapshot = providers.connector().marketDataGateway().loadMarket(symbol);
        if (snapshot == null) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다: " + symbol);
        }
        return snapshot;
    }
}
