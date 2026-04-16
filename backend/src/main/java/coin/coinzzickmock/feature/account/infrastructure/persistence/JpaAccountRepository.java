package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class JpaAccountRepository implements AccountRepository {
    private final TradingAccountSpringDataRepository tradingAccountSpringDataRepository;

    public JpaAccountRepository(TradingAccountSpringDataRepository tradingAccountSpringDataRepository) {
        this.tradingAccountSpringDataRepository = tradingAccountSpringDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TradingAccount> findByMemberId(String memberId) {
        return tradingAccountSpringDataRepository.findById(memberId)
                .map(TradingAccountJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public TradingAccount save(TradingAccount account) {
        TradingAccountJpaEntity entity = tradingAccountSpringDataRepository.findById(account.memberId())
                .map(existing -> {
                    existing.apply(existing.memberEmail(), account);
                    return existing;
                })
                .orElseGet(() -> TradingAccountJpaEntity.from(defaultEmail(account.memberId()), account));
        return tradingAccountSpringDataRepository.save(entity).toDomain();
    }

    private String defaultEmail(String memberId) {
        if ("demo-member".equals(memberId)) {
            return "demo@coinzzickmock.dev";
        }
        return memberId + "@coinzzickmock.local";
    }
}
