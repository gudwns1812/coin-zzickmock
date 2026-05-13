package coin.coinzzickmock.feature.position.domain;

public record CrossPositionRisk(
        PositionSnapshot position,
        double initialMargin,
        double unrealizedPnl,
        double equity,
        double maintenanceRequirement,
        double riskRatio
) {
    public double absoluteUnrealizedLoss() {
        return unrealizedPnl < 0 ? Math.abs(unrealizedPnl) : 0d;
    }
}
