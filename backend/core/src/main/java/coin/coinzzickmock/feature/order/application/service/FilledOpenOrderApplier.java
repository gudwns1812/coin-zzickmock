package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.position.application.event.PositionOpenedEvent;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.realtime.OpenPositionBookWriter;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilledOpenOrderApplier {
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final OpenPositionBookWriter openPositionBookWriter;


    public void apply(FilledOpenOrder order) {
        PositionSnapshot existing = positionRepository.findOpenPosition(
                order.memberId(),
                order.symbol(),
                order.positionSide()
        ).orElse(null);

        if (existing != null && !existing.marginMode().equalsIgnoreCase(order.marginMode())) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }

        int effectiveLeverage = existing == null
                ? symbolMarginLeverage(order).orElse(order.leverage())
                : existing.leverage();
        double initialMargin = order.reservedInitialMargin(effectiveLeverage);
        TradingAccount updatedAccount = reserveAccountMargin(order.memberId(), order.estimatedFee(), initialMargin);
        afterCommitEventPublisher.publish(WalletBalanceChangedEvent.from(updatedAccount));

        if (existing == null) {
            PositionSnapshot saved = positionRepository.save(order.memberId(), PositionSnapshot.open(
                    order.symbol(),
                    order.positionSide(),
                    order.marginMode(),
                    effectiveLeverage,
                    order.quantity(),
                    order.executionPrice(),
                    order.markPrice(),
                    order.estimatedFee()
            ));
            openPositionBookWriter.addAfterCommit(order.memberId(), saved);
            afterCommitEventPublisher.publish(new PositionOpenedEvent(order.memberId(), order.symbol()));
            return;
        }

        PositionMutationResult mutationResult = positionRepository.updateWithVersion(
                order.memberId(),
                existing,
                existing.increase(
                        effectiveLeverage,
                        order.quantity(),
                        order.executionPrice(),
                        order.markPrice(),
                        order.estimatedFee()
                )
        );
        validatePositionMutation(mutationResult);
        openPositionBookWriter.replaceAfterCommit(order.memberId(), mutationResult.updatedSnapshot());
    }

    private TradingAccount reserveAccountMargin(Long memberId, double estimatedFee, double initialMargin) {
        TradingAccount account = accountRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        // Order traceability stays in order/position history; wallet_history is a KST daily account snapshot.
        return validateAccountMutation(accountRepository.updateWithVersion(
                account,
                account.reserveForFilledOrder(estimatedFee, initialMargin)
        ));
    }

    private Optional<Integer> symbolMarginLeverage(FilledOpenOrder order) {
        return positionRepository.findOpenPositions(order.memberId())
                .stream()
                .filter(position -> position.symbol().equalsIgnoreCase(order.symbol()))
                .filter(position -> position.marginMode().equalsIgnoreCase(order.marginMode()))
                .map(PositionSnapshot::leverage)
                .findFirst();
    }

    private TradingAccount validateAccountMutation(AccountMutationResult mutationResult) {
        if (mutationResult.succeeded()) {
            return mutationResult.updatedAccount();
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
            Double reservedInitialMargin
    ) {
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
                    null
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
