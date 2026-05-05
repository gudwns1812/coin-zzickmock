package coin.coinzzickmock.feature.position.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record CrossLiquidationAssessment(
        double walletBalance,
        double isolatedInitialMargin,
        double totalUnrealizedPnl,
        double crossEquity,
        double maintenanceRequirement,
        boolean breached,
        List<CrossPositionRisk> rankedRisks
) {
    public CrossLiquidationAssessment {
        rankedRisks = List.copyOf(rankedRisks);
    }

    public Optional<CrossPositionRisk> liquidationCandidate() {
        return rankedRisks.stream()
                .min(Comparator.comparingDouble(CrossPositionRisk::riskRatio)
                        .thenComparing((left, right) -> Double.compare(
                                right.absoluteUnrealizedLoss(),
                                left.absoluteUnrealizedLoss()
                        ))
                        .thenComparing(risk -> risk.position().stableKey()));
    }
}
