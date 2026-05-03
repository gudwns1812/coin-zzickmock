package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<TradingAccount> findByMemberId(Long memberId) {
        return tradingAccountEntityRepository.findById(memberId)
                .map(TradingAccountEntity::toDomain);
    }

    @Override
    @Transactional
    public TradingAccount create(TradingAccount account) {
        TradingAccountEntity entity = TradingAccountEntity.from(account);
        entityManager.persist(entity);
        // Flush here to prove provisioning is insert-only for manually assigned account ids.
        entityManager.flush();
        return entity.toDomain();
    }

    @Override
    @Transactional
    public AccountMutationResult updateWithVersion(
            TradingAccount expectedAccount,
            TradingAccount nextAccount,
            WalletHistorySource source
    ) {
        if (source == null) {
            throw new IllegalArgumentException("WalletHistorySource is required for account mutations.");
        }

        Optional<TradingAccountEntity> current = tradingAccountEntityRepository.findById(expectedAccount.memberId());
        if (current.isEmpty()) {
            return AccountMutationResult.notFound();
        }

        TradingAccountEntity entity = current.orElseThrow();
        if (entity.version() != expectedAccount.version()) {
            return AccountMutationResult.staleVersion(entity.toDomain());
        }

        TradingAccount versioned = nextAccount.withVersion(expectedAccount.version() + 1);
        entity.apply(versioned);
        walletHistoryPersistenceRepository.saveIfAbsent(versioned, source, Instant.now());
        return AccountMutationResult.updated(1, versioned);
    }
}
