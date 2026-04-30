package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountPersistenceRepository implements AccountRepository {
    private final TradingAccountEntityRepository tradingAccountEntityRepository;
    private final WalletHistoryPersistenceRepository walletHistoryPersistenceRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TradingAccount> findByMemberId(Long memberId) {
        return tradingAccountEntityRepository.findById(memberId)
                .map(TradingAccountEntity::toDomain);
    }

    @Override
    @Transactional
    public TradingAccount save(TradingAccount account) {
        return save(account, null);
    }

    @Override
    @Transactional
    public TradingAccount save(TradingAccount account, WalletHistorySource source) {
        TradingAccountEntity entity = tradingAccountEntityRepository.findById(account.memberId())
                .map(existing -> {
                    existing.apply(account);
                    return existing;
                })
                .orElseGet(() -> TradingAccountEntity.from(account));
        TradingAccount saved = tradingAccountEntityRepository.save(entity).toDomain();
        if (source != null) {
            walletHistoryPersistenceRepository.saveIfAbsent(saved, source, Instant.now());
        }
        return saved;
    }
}
