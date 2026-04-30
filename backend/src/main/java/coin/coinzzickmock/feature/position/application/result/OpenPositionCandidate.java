package coin.coinzzickmock.feature.position.application.result;

import coin.coinzzickmock.feature.position.domain.PositionSnapshot;

public record OpenPositionCandidate(
        Long memberId,
        PositionSnapshot position
) {
    public String symbol() {
        return position.symbol();
    }

    public String positionSide() {
        return position.positionSide();
    }

    public String marginMode() {
        return position.marginMode();
    }
}
