package coin.coinzzickmock.feature.position.application.result;

import coin.coinzzickmock.feature.position.domain.PositionSnapshot;

public record PositionMutationResult(
        Status status,
        long affectedRows,
        PositionSnapshot updatedSnapshot
) {
    public enum Status {
        UPDATED,
        DELETED,
        STALE_VERSION,
        NOT_FOUND
    }

    public static PositionMutationResult updated(long affectedRows, PositionSnapshot updatedSnapshot) {
        return new PositionMutationResult(Status.UPDATED, affectedRows, updatedSnapshot);
    }

    public static PositionMutationResult deleted(long affectedRows) {
        return new PositionMutationResult(Status.DELETED, affectedRows, null);
    }

    public static PositionMutationResult staleVersion(PositionSnapshot currentSnapshot) {
        return new PositionMutationResult(Status.STALE_VERSION, 0, currentSnapshot);
    }

    public static PositionMutationResult notFound() {
        return new PositionMutationResult(Status.NOT_FOUND, 0, null);
    }

    public boolean succeeded() {
        return status == Status.UPDATED || status == Status.DELETED;
    }
}
