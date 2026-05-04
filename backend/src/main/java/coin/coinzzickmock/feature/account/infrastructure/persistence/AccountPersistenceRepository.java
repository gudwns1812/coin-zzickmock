package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import java.util.List;
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
    public Optional<TradingAccount> findByMemberId(Long memberId) {
        return tradingAccountEntityRepository.findById(memberId)
                .map(TradingAccountEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
        return tradingAccountEntityRepository.findWithLockingByMemberId(memberId)
                .map(TradingAccountEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradingAccount> findAll() {
        return tradingAccountEntityRepository.findAll().stream()
                .map(TradingAccountEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public TradingAccount create(TradingAccount account) {
        TradingAccountEntity entity = tradingAccountEntityRepository.saveAndFlush(TradingAccountEntity.from(account));
        return entity.toDomain();
    }

    @Override
    @Transactional
    public AccountMutationResult updateWithVersion(
            TradingAccount expectedAccount,
            TradingAccount nextAccount
    ) {
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
        return AccountMutationResult.updated(1, versioned);
    }
}
