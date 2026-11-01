package coin.coinzzickmock.feature.account.application.repository;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.application.dto.AccountMutationResult;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<TradingAccount> findByMemberId(Long memberId);

    Optional<TradingAccount> findByMemberIdForUpdate(Long memberId);

    /**
     * 락 없이 지갑 잔고 값만 읽는다. 계좌 엔티티를 영속성 컨텍스트에 올리지 않으므로,
     * 같은 트랜잭션에서 이후 findByMemberIdForUpdate가 잠근 행의 최신 값을 돌려준다.
     * (findByMemberId로 엔티티를 먼저 읽으면, 그사이 다른 트랜잭션이 계좌를 바꿨을 때
     * 잠금 조회가 version 불일치로 StaleObjectStateException을 던진다.)
     */
    Optional<Double> findWalletBalanceByMemberId(Long memberId);

    List<TradingAccount> findAll();

    TradingAccount create(TradingAccount account);

    AccountMutationResult updateWithVersion(
            TradingAccount expectedAccount,
            TradingAccount nextAccount
    );
}
