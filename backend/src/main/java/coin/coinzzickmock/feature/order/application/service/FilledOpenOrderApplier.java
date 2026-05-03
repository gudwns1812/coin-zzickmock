package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilledOpenOrderApplier {
    private static final String DEFAULT_MARGIN_MODE_MISMATCH_MESSAGE = "기존 포지션과 다른 마진 모드로 주문할 수 없습니다.";

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    public void apply(FilledOpenOrder order) {
        PositionSnapshot existing = positionRepository.findOpenPosition(
                order.memberId(),
                order.symbol(),
                order.positionSide()
        ).orElse(null);

        if (existing != null && !existing.marginMode().equalsIgnoreCase(order.marginMode())) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, order.marginModeMismatchMessage());
        }

        int effectiveLeverage = existing == null ? order.leverage() : existing.leverage();
        double initialMargin = order.reservedInitialMargin(effectiveLeverage);
        reserveAccountMargin(order.memberId(), order.orderId(), order.estimatedFee(), initialMargin);
        afterCommitEventPublisher.publish(new WalletBalanceChangedEvent(order.memberId()));

        if (existing == null) {
            positionRepository.save(order.memberId(), PositionSnapshot.open(
                    order.symbol(),
                    order.positionSide(),
                    order.marginMode(),
                    effectiveLeverage,
                    order.quantity(),
                    order.executionPrice(),
                    order.markPrice(),
                    order.estimatedFee()
            ));
            return;
        }

        validatePositionMutation(positionRepository.updateWithVersion(
                order.memberId(),
                existing,
                existing.increase(
                        effectiveLeverage,
                        order.quantity(),
                        order.executionPrice(),
                        order.markPrice(),
                        order.estimatedFee()
                )
        ));
    }

    private void reserveAccountMargin(Long memberId, String orderId, double estimatedFee, double initialMargin) {
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        validateAccountMutation(accountRepository.updateWithVersion(
                account,
                account.reserveForFilledOrder(estimatedFee, initialMargin),
                WalletHistorySource.orderFill(orderId)
        ));
    }

    private void validateAccountMutation(AccountMutationResult mutationResult) {
        if (mutationResult.succeeded()) {
            return;
        }
        if (mutationResult.status() == AccountMutationResult.Status.NOT_FOUND) {
            throw new CoreException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        throw new CoreException(ErrorCode.ACCOUNT_CHANGED);
    }

    private void validatePositionMutation(PositionMutationResult mutationResult) {
        if (mutationResult.succeeded()) {
            return;
        }
        if (mutationResult.status() == PositionMutationResult.Status.NOT_FOUND) {
            throw new CoreException(ErrorCode.POSITION_NOT_FOUND);
        }
        throw new CoreException(ErrorCode.POSITION_CHANGED);
    }

    public record FilledOpenOrder(
            Long memberId,
            String orderId,
            String symbol,
            String positionSide,
            String marginMode,
            int leverage,
            double quantity,
            double executionPrice,
            double markPrice,
            double estimatedFee,
            Double reservedInitialMargin,
            String marginModeMismatchMessage
    ) {
        public FilledOpenOrder {
            if (marginModeMismatchMessage == null || marginModeMismatchMessage.isBlank()) {
                marginModeMismatchMessage = DEFAULT_MARGIN_MODE_MISMATCH_MESSAGE;
            }
        }

        public FilledOpenOrder(
                Long memberId,
                String orderId,
                String symbol,
                String positionSide,
                String marginMode,
                int leverage,
                double quantity,
                double executionPrice,
                double markPrice,
                double estimatedFee
        ) {
            this(
                    memberId,
                    orderId,
                    symbol,
                    positionSide,
                    marginMode,
                    leverage,
                    quantity,
                    executionPrice,
                    markPrice,
                    estimatedFee,
                    null,
                    DEFAULT_MARGIN_MODE_MISMATCH_MESSAGE
            );
        }

        public FilledOpenOrder(
                Long memberId,
                String orderId,
                String symbol,
                String positionSide,
                String marginMode,
                int leverage,
                double quantity,
                double executionPrice,
                double markPrice,
                double estimatedFee,
                String marginModeMismatchMessage
        ) {
            this(
                    memberId,
                    orderId,
                    symbol,
                    positionSide,
                    marginMode,
                    leverage,
                    quantity,
                    executionPrice,
                    markPrice,
                    estimatedFee,
                    null,
                    marginModeMismatchMessage
            );
        }

        private double reservedInitialMargin(int effectiveLeverage) {
            if (reservedInitialMargin != null) {
                return reservedInitialMargin;
            }
            return (executionPrice * quantity) / effectiveLeverage;
        }
    }
}
