package coin.coinzzickmock.feature.account.application.result;

import coin.coinzzickmock.feature.account.domain.TradingAccount;

public record AccountMutationResult(
        Status status,
        long affectedRows,
        TradingAccount updatedAccount
) {
    public enum Status {
        UPDATED,
        STALE_VERSION,
        NOT_FOUND
    }

    public static AccountMutationResult updated(long affectedRows, TradingAccount updatedAccount) {
        return new AccountMutationResult(Status.UPDATED, affectedRows, updatedAccount);
    }

    public static AccountMutationResult staleVersion(TradingAccount currentAccount) {
        return new AccountMutationResult(Status.STALE_VERSION, 0, currentAccount);
    }

    public static AccountMutationResult notFound() {
        return new AccountMutationResult(Status.NOT_FOUND, 0, null);
    }

    public boolean succeeded() {
        return status == Status.UPDATED;
    }
}
