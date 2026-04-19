package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.CreateOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.providers.Providers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CreateOrderService {
    private final OrderPreviewPolicy orderPreviewPolicy;
    private final Providers providers;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;

    @Transactional(readOnly = true)
    public OrderPreview preview(CreateOrderCommand command) {
        return preview(command, loadMarket(command.symbol()));
    }

    @Transactional
    public CreateOrderResult execute(CreateOrderCommand command) {
        MarketSnapshot marketSnapshot = loadMarket(command.symbol());
        OrderPreview preview = preview(command, marketSnapshot);
        String orderId = UUID.randomUUID().toString();
        String status = preview.executable() ? "FILLED" : "PENDING";

        FuturesOrder futuresOrder = orderRepository.save(
                command.memberId(),
                new FuturesOrder(
                        orderId,
                        command.symbol(),
                        command.positionSide(),
                        command.orderType(),
                        command.marginMode(),
                        command.leverage(),
                        command.quantity(),
                        command.limitPrice(),
                        status,
                        preview.feeType(),
                        preview.estimatedFee(),
                        marketSnapshot.lastPrice()
                )
        );

        if (preview.executable()) {
            applyFilledOrder(command, marketSnapshot, preview);
        }

        return new CreateOrderResult(
                futuresOrder.orderId(),
                futuresOrder.status(),
                futuresOrder.symbol(),
                futuresOrder.feeType(),
                preview.estimatedFee(),
                preview.estimatedInitialMargin(),
                preview.estimatedLiquidationPrice(),
                futuresOrder.executionPrice()
        );
    }

    private void applyFilledOrder(
            CreateOrderCommand command,
            MarketSnapshot marketSnapshot,
            OrderPreview preview
    ) {
        TradingAccount account = accountRepository.findByMemberId(command.memberId())
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountRepository.save(account.reserveForFilledOrder(preview.estimatedFee(), preview.estimatedInitialMargin()));

        PositionSnapshot existing = positionRepository.findOpenPosition(
                command.memberId(),
                command.symbol(),
                command.positionSide(),
                command.marginMode()
        ).orElse(null);

        if (existing == null) {
            positionRepository.save(command.memberId(), PositionSnapshot.open(
                    command.symbol(),
                    command.positionSide(),
                    command.marginMode(),
                    command.leverage(),
                    command.quantity(),
                    marketSnapshot.lastPrice(),
                    marketSnapshot.markPrice()
            ));
            return;
        }

        positionRepository.save(
                command.memberId(),
                existing.increase(command.leverage(), command.quantity(), marketSnapshot.lastPrice(), marketSnapshot.markPrice())
        );
    }

    private OrderPreview preview(CreateOrderCommand command, MarketSnapshot marketSnapshot) {
        return orderPreviewPolicy.preview(command, marketSnapshot);
    }

    private MarketSnapshot loadMarket(String symbol) {
        MarketSnapshot snapshot = providers.connector().marketDataGateway().loadMarket(symbol);
        if (snapshot == null) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다: " + symbol);
        }
        return snapshot;
    }
}
