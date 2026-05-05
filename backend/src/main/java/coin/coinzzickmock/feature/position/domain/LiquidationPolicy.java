package coin.coinzzickmock.feature.position.domain;

import coin.coinzzickmock.common.trading.LiquidationFormula;
import java.util.Comparator;
import java.util.List;

public class LiquidationPolicy {
    public static final double MAINTENANCE_MARGIN_RATE = LiquidationFormula.MAINTENANCE_MARGIN_RATE;

    private static final double MINIMUM_RISK_RATIO_EQUITY = 0d;
    private static final double NO_MAINTENANCE_REQUIREMENT = 0d;

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
        List<PositionSnapshot> crossPositions = crossPositions(positions);
        if (!hasTargetSymbol(crossPositions, targetSymbol)) {
            return CrossLiquidationEstimate.unavailable();
        }

        CrossLiquidationEquationTerms terms = equationTerms(
                walletBalance,
                isolatedInitialMargin(positions),
                crossPositions,
                targetSymbol
        );
        Double boundary = terms.solveBoundary();
        if (boundary == null) {
            return CrossLiquidationEstimate.unavailable();
        }

        return hasOnlyTargetSymbol(crossPositions, targetSymbol)
                ? CrossLiquidationEstimate.exact(boundary)
                : CrossLiquidationEstimate.estimated(boundary);
    }

    private List<PositionSnapshot> crossPositions(List<PositionSnapshot> positions) {
        return positions.stream()
                .filter(PositionSnapshot::isCrossMargin)
                .toList();
    }

    private boolean hasTargetSymbol(List<PositionSnapshot> positions, String targetSymbol) {
        return positions.stream()
                .anyMatch(position -> isTargetSymbol(position, targetSymbol));
    }

    private boolean hasOnlyTargetSymbol(List<PositionSnapshot> positions, String targetSymbol) {
        return positions.stream()
                .allMatch(position -> isTargetSymbol(position, targetSymbol));
    }

    private boolean isTargetSymbol(PositionSnapshot position, String targetSymbol) {
        return position.symbol().equalsIgnoreCase(targetSymbol);
    }

    private double isolatedInitialMargin(List<PositionSnapshot> positions) {
        return positions.stream()
                .filter(position -> !position.isCrossMargin())
                .mapToDouble(PositionSnapshot::initialMargin)
                .sum();
    }

    private CrossLiquidationEquationTerms equationTerms(
            double walletBalance,
            double isolatedInitialMargin,
            List<PositionSnapshot> crossPositions,
            String targetSymbol
    ) {
        CrossLiquidationEquationTerms terms = CrossLiquidationEquationTerms.startingWith(
                walletBalance,
                isolatedInitialMargin
        );
        for (PositionSnapshot position : crossPositions) {
            if (isTargetSymbol(position, targetSymbol)) {
                terms = terms.includeTarget(position);
            } else {
                terms = terms.includeFixedOther(position);
            }
        }
        return terms;
    }

    private CrossPositionRisk toCrossRisk(PositionSnapshot position) {
        double initialMargin = position.initialMargin();
        double unrealizedPnl = position.unrealizedPnl(position.markPrice());
        double equity = Math.max(MINIMUM_RISK_RATIO_EQUITY, initialMargin + unrealizedPnl);
        double maintenanceRequirement = LiquidationFormula.maintenanceRequirement(
                position.markPrice(),
                position.quantity()
        );
        double riskRatio = maintenanceRequirement == NO_MAINTENANCE_REQUIREMENT
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

    private record CrossLiquidationEquationTerms(
            double equityConstant,
            double pnlSlope,
            double maintenanceConstant,
            double maintenanceSlope
    ) {
        private static final double EMPTY_PNL_SLOPE = 0d;
        private static final double EMPTY_MAINTENANCE_REQUIREMENT = 0d;
        private static final double EMPTY_MAINTENANCE_SLOPE = 0d;

        private static CrossLiquidationEquationTerms startingWith(
                double walletBalance,
                double isolatedInitialMargin
        ) {
            return new CrossLiquidationEquationTerms(
                    walletBalance - isolatedInitialMargin,
                    EMPTY_PNL_SLOPE,
                    EMPTY_MAINTENANCE_REQUIREMENT,
                    EMPTY_MAINTENANCE_SLOPE
            );
        }

        private CrossLiquidationEquationTerms includeTarget(PositionSnapshot position) {
            double quantity = position.quantity();
            if (position.identity().isLong()) {
                return new CrossLiquidationEquationTerms(
                        equityConstant - position.entryPrice() * quantity,
                        pnlSlope + quantity,
                        maintenanceConstant,
                        maintenanceSlope + quantity * MAINTENANCE_MARGIN_RATE
                );
            }
            return new CrossLiquidationEquationTerms(
                    equityConstant + position.entryPrice() * quantity,
                    pnlSlope - quantity,
                    maintenanceConstant,
                    maintenanceSlope + quantity * MAINTENANCE_MARGIN_RATE
            );
        }

        private CrossLiquidationEquationTerms includeFixedOther(PositionSnapshot position) {
            return new CrossLiquidationEquationTerms(
                    equityConstant + position.unrealizedPnl(position.markPrice()),
                    pnlSlope,
                    maintenanceConstant + LiquidationFormula.maintenanceRequirement(
                            position.markPrice(),
                            position.quantity()
                    ),
                    maintenanceSlope
            );
        }

        private Double solveBoundary() {
            return LiquidationFormula.solveLinearBoundary(
                    equityConstant,
                    pnlSlope,
                    maintenanceConstant,
                    maintenanceSlope
            );
        }
    }
}
