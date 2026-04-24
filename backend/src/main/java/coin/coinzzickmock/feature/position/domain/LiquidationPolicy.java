package coin.coinzzickmock.feature.position.domain;

import java.util.Comparator;
import java.util.List;

public class LiquidationPolicy {
    public static final double MAINTENANCE_MARGIN_RATE = 0.005d;

    public IsolatedLiquidationAssessment assessIsolated(PositionSnapshot position, double markPrice) {
        double initialMargin = position.initialMargin();
        double unrealizedPnl = position.unrealizedPnl(markPrice);
        double notional = position.notional(markPrice);
        double equity = initialMargin + unrealizedPnl;
        double maintenanceRequirement = notional * MAINTENANCE_MARGIN_RATE;
        return new IsolatedLiquidationAssessment(
                position,
                markPrice,
                notional,
                initialMargin,
                unrealizedPnl,
                equity,
                maintenanceRequirement,
                equity <= maintenanceRequirement
        );
    }

    public CrossLiquidationAssessment assessCross(double availableMargin, List<PositionSnapshot> positions) {
        List<CrossPositionRisk> rankedRisks = positions.stream()
                .filter(PositionSnapshot::isCrossMargin)
                .map(this::toCrossRisk)
                .sorted(Comparator.comparingDouble(CrossPositionRisk::riskRatio)
                        .thenComparing((left, right) -> Double.compare(
                                right.absoluteUnrealizedLoss(),
                                left.absoluteUnrealizedLoss()
                        ))
                        .thenComparing(risk -> risk.position().stableKey()))
                .toList();

        double totalUnrealizedPnl = rankedRisks.stream()
                .mapToDouble(CrossPositionRisk::unrealizedPnl)
                .sum();
        double maintenanceRequirement = rankedRisks.stream()
                .mapToDouble(CrossPositionRisk::maintenanceRequirement)
                .sum();
        double crossEquity = availableMargin + totalUnrealizedPnl;

        return new CrossLiquidationAssessment(
                availableMargin,
                totalUnrealizedPnl,
                crossEquity,
                maintenanceRequirement,
                crossEquity <= maintenanceRequirement,
                rankedRisks
        );
    }

    private CrossPositionRisk toCrossRisk(PositionSnapshot position) {
        double initialMargin = position.initialMargin();
        double unrealizedPnl = position.unrealizedPnl(position.markPrice());
        double equity = Math.max(0d, initialMargin + unrealizedPnl);
        double maintenanceRequirement = position.notional(position.markPrice()) * MAINTENANCE_MARGIN_RATE;
        double riskRatio = maintenanceRequirement == 0
                ? Double.POSITIVE_INFINITY
                : equity / maintenanceRequirement;
        return new CrossPositionRisk(
                position,
                initialMargin,
                unrealizedPnl,
                equity,
                maintenanceRequirement,
                riskRatio
        );
    }
}
