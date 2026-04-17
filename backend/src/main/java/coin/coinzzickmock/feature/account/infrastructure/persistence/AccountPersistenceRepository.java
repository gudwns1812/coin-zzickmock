package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountPersistenceRepository implements AccountRepository {
    private final TradingAccountEntityRepository tradingAccountEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TradingAccount> findByMemberId(String memberId) {
        return tradingAccountEntityRepository.findById(memberId)
                .map(TradingAccountEntity::toDomain);
    }

    @Override
    @Transactional
    public TradingAccount save(TradingAccount account) {
        TradingAccountEntity entity = tradingAccountEntityRepository.findById(account.memberId())
                .map(existing -> {
                    existing.apply(account);
                    return existing;
                })
                .orElseGet(() -> TradingAccountEntity.from(account));
        return tradingAccountEntityRepository.save(entity).toDomain();
    }
}
