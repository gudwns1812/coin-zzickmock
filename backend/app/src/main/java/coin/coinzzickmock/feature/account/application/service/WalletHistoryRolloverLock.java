package coin.coinzzickmock.feature.account.application.service;

public interface WalletHistoryRolloverLock {
    boolean runIfAcquired(Runnable task);
}
