package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import java.util.List;
import java.util.Optional;

public abstract class TestAccountRepository implements AccountRepository {
    @Override
    public Optional<TradingAccount> findByMemberId(Long memberId) {
        throw new UnsupportedOperationException("findByMemberId is not implemented for this test fake");
    }

    @Override
    public Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
        throw new UnsupportedOperationException("findByMemberIdForUpdate is not implemented for this test fake");
    }

    @Override
    public List<TradingAccount> findAll() {
        throw new UnsupportedOperationException("findAll is not implemented for this test fake");
    }

    @Override
    public TradingAccount create(TradingAccount account) {
        throw new UnsupportedOperationException("create is not implemented for this test fake");
    }

    @Override
    public AccountMutationResult updateWithVersion(TradingAccount expectedAccount, TradingAccount nextAccount) {
        throw new UnsupportedOperationException("updateWithVersion is not implemented for this test fake");
    }
}
