package coin.coinzzickmock.feature.position.domain;

import coin.coinzzickmock.common.trading.LiquidationFormula;
import java.util.Comparator;
import java.util.List;

public class LiquidationPolicy {
    public static final double MAINTENANCE_MARGIN_RATE = LiquidationFormula.MAINTENANCE_MARGIN_RATE;

    public IsolatedLiquidationAssessment assessIsolated(PositionSnapshot position, double markPrice) {
        double initialMargin = position.initialMargin();
        double unrealizedPnl = position.unrealizedPnl(markPrice);
        double notional = position.notional(markPrice);
        double equity = initialMargin + unrealizedPnl;
        double maintenanceRequirement = LiquidationFormula.maintenanceRequirement(markPrice, position.quantity());
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

    public CrossLiquidationAssessment assessCross(double walletBalance, List<PositionSnapshot> positions) {
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
        double isolatedInitialMargin = positions.stream()
                .filter(position -> !position.isCrossMargin())
                .mapToDouble(PositionSnapshot::initialMargin)
                .sum();
        double crossEquity = walletBalance - isolatedInitialMargin + totalUnrealizedPnl;

        return new CrossLiquidationAssessment(
                walletBalance,
                isolatedInitialMargin,
                totalUnrealizedPnl,
                crossEquity,
                maintenanceRequirement,
                crossEquity <= maintenanceRequirement,
                rankedRisks
        );
    }

    public CrossLiquidationEstimate estimateCrossLiquidationPrice(
            double walletBalance,
            List<PositionSnapshot> positions,
            String targetSymbol
    ) {
        List<PositionSnapshot> crossPositions = positions.stream()
                .filter(PositionSnapshot::isCrossMargin)
                .toList();
        List<PositionSnapshot> targetPositions = crossPositions.stream()
                .filter(position -> position.symbol().equalsIgnoreCase(targetSymbol))
                .toList();
        if (targetPositions.isEmpty()) {
            return CrossLiquidationEstimate.unavailable();
        }

        double isolatedInitialMargin = positions.stream()
                .filter(position -> !position.isCrossMargin())
                .mapToDouble(PositionSnapshot::initialMargin)
                .sum();
        double baseEquity = walletBalance - isolatedInitialMargin;
        double baseMaintenance = 0d;
        double pnlSlope = 0d;
        double maintenanceSlope = 0d;

        for (PositionSnapshot position : crossPositions) {
            if (position.symbol().equalsIgnoreCase(targetSymbol)) {
                double quantity = position.quantity();
                if (position.identity().isLong()) {
                    pnlSlope += quantity;
                    baseEquity -= position.entryPrice() * quantity;
                } else {
                    pnlSlope -= quantity;
                    baseEquity += position.entryPrice() * quantity;
                }
                maintenanceSlope += quantity * MAINTENANCE_MARGIN_RATE;
                continue;
            }

            baseEquity += position.unrealizedPnl(position.markPrice());
            baseMaintenance += LiquidationFormula.maintenanceRequirement(position.markPrice(), position.quantity());
        }

        Double boundary = LiquidationFormula.solveLinearBoundary(
                baseEquity,
                pnlSlope,
                baseMaintenance,
                maintenanceSlope
        );
        if (boundary == null) {
            return CrossLiquidationEstimate.unavailable();
        }

        boolean exact = crossPositions.stream()
                .allMatch(position -> position.symbol().equalsIgnoreCase(targetSymbol));
        return exact
                ? CrossLiquidationEstimate.exact(boundary)
                : CrossLiquidationEstimate.estimated(boundary);
    }

    private CrossPositionRisk toCrossRisk(PositionSnapshot position) {
        double initialMargin = position.initialMargin();
        double unrealizedPnl = position.unrealizedPnl(position.markPrice());
        double equity = Math.max(0d, initialMargin + unrealizedPnl);
        double maintenanceRequirement = LiquidationFormula.maintenanceRequirement(
                position.markPrice(),
                position.quantity()
        );
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
