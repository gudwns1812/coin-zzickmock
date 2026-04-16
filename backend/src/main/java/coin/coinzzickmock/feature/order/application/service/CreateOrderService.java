package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.BadRequestException;
import coin.coinzzickmock.common.error.NotFoundException;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.CreateOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.providers.Providers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateOrderService {
    private final Providers providers;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;

    public CreateOrderService(
            Providers providers,
            OrderRepository orderRepository,
            AccountRepository accountRepository,
            PositionRepository positionRepository
    ) {
        this.providers = providers;
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public OrderPreview preview(CreateOrderCommand command) {
        MarketSnapshot market = loadMarket(command.symbol());
        double entryPrice = "LIMIT".equalsIgnoreCase(command.orderType()) && command.limitPrice() != null
                ? command.limitPrice()
                : market.lastPrice();
        boolean executable = "MARKET".equalsIgnoreCase(command.orderType())
                || ("LONG".equalsIgnoreCase(command.positionSide()) && market.lastPrice() <= entryPrice)
                || ("SHORT".equalsIgnoreCase(command.positionSide()) && market.lastPrice() >= entryPrice);
        String feeType = executable ? "TAKER" : "MAKER";
        double feeRate = executable ? 0.0005 : 0.00015;
        double estimatedFee = entryPrice * command.quantity() * feeRate;
        double estimatedInitialMargin = entryPrice * command.quantity() / command.leverage();
        double liquidationGap = entryPrice / command.leverage();
        double estimatedLiquidationPrice = "LONG".equalsIgnoreCase(command.positionSide())
                ? entryPrice - liquidationGap
                : entryPrice + liquidationGap;
        return new OrderPreview(feeType, estimatedFee, estimatedInitialMargin, estimatedLiquidationPrice);
    }

    @Transactional
    public CreateOrderResult execute(CreateOrderCommand command) {
        OrderPreview preview = preview(command);
        MarketSnapshot marketSnapshot = loadMarket(command.symbol());
        String orderId = UUID.randomUUID().toString();
        boolean executable = isExecutable(command, marketSnapshot.lastPrice());
        String status = executable ? "FILLED" : "PENDING";

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

        if (executable) {
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
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        double requiredMargin = preview.estimatedInitialMargin() + preview.estimatedFee();
        if (account.availableMargin() < requiredMargin) {
            throw new BadRequestException(ErrorCode.INSUFFICIENT_AVAILABLE_MARGIN);
        }

        accountRepository.save(new TradingAccount(
                account.memberId(),
                account.memberName(),
                account.walletBalance() - preview.estimatedFee(),
                account.availableMargin() - requiredMargin
        ));

        PositionSnapshot existing = positionRepository.findOpenPosition(
                command.memberId(),
                command.symbol(),
                command.positionSide(),
                command.marginMode()
        ).orElse(null);

        if (existing == null) {
            positionRepository.save(command.memberId(), new PositionSnapshot(
                    command.symbol(),
                    command.positionSide(),
                    command.marginMode(),
                    command.leverage(),
                    command.quantity(),
                    marketSnapshot.lastPrice(),
                    marketSnapshot.markPrice(),
                    preview.estimatedLiquidationPrice(),
                    0
            ));
            return;
        }

        double totalQuantity = existing.quantity() + command.quantity();
        double weightedEntryPrice =
                ((existing.entryPrice() * existing.quantity()) + (marketSnapshot.lastPrice() * command.quantity()))
                        / totalQuantity;

        positionRepository.save(command.memberId(), new PositionSnapshot(
                existing.symbol(),
                existing.positionSide(),
                existing.marginMode(),
                command.leverage(),
                totalQuantity,
                weightedEntryPrice,
                marketSnapshot.markPrice(),
                preview.estimatedLiquidationPrice(),
                0
        ));
    }

    private boolean isExecutable(CreateOrderCommand command, double lastPrice) {
        if ("MARKET".equalsIgnoreCase(command.orderType())) {
            return true;
        }

        double limitPrice = command.limitPrice() == null ? lastPrice : command.limitPrice();
        if ("LONG".equalsIgnoreCase(command.positionSide())) {
            return lastPrice <= limitPrice;
        }
        return lastPrice >= limitPrice;
    }

    private MarketSnapshot loadMarket(String symbol) {
        MarketSnapshot snapshot = providers.connector().marketDataGateway().loadMarket(symbol);
        if (snapshot == null) {
            throw new NotFoundException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다: " + symbol);
        }
        return snapshot;
    }
}
