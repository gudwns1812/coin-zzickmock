package coin.coinzzickmock.feature.account.infrastructure.persistence;

import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
    private final JPAQueryFactory jpaQueryFactory;
    private final PathBuilder<TradingAccountEntity> account = new PathBuilder<>(TradingAccountEntity.class, "account");
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
        entityManager.flush();
        tradingAccountEntityRepository.findById(expectedAccount.memberId())
                .ifPresent(entityManager::detach);

        TradingAccount versioned = nextAccount.withVersion(expectedAccount.version() + 1);
        long affectedRows = jpaQueryFactory.update(account)
                .where(
                        account.getNumber("memberId", Long.class).eq(expectedAccount.memberId()),
                        account.getNumber("version", Long.class).eq(expectedAccount.version())
                )
                .set(account.getString("memberEmail"), versioned.memberEmail())
                .set(account.getString("memberName"), versioned.memberName())
                .set(account.get("walletBalance", java.math.BigDecimal.class), java.math.BigDecimal.valueOf(versioned.walletBalance()))
                .set(account.get("availableMargin", java.math.BigDecimal.class), java.math.BigDecimal.valueOf(versioned.availableMargin()))
                .set(account.getNumber("version", Long.class), versioned.version())
                .execute();

        if (affectedRows > 0) {
            if (source != null) {
                walletHistoryPersistenceRepository.saveIfAbsent(versioned, source, Instant.now());
            }
            return AccountMutationResult.updated(affectedRows, versioned);
        }

        return findByMemberId(expectedAccount.memberId())
                .map(AccountMutationResult::staleVersion)
                .orElseGet(AccountMutationResult::notFound);
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
