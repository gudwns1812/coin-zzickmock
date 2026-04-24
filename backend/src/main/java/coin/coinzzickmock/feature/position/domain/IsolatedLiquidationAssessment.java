package coin.coinzzickmock.feature.position.domain;

public record IsolatedLiquidationAssessment(
        PositionSnapshot position,
        double markPrice,
        double notional,
        double initialMargin,
        double unrealizedPnl,
        double equity,
        double maintenanceRequirement,
        boolean breached
) {
}
