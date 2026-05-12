package coin.coinzzickmock.feature.position.application.query;

import java.util.List;

public interface OpenPositionSymbolsReader {
    List<String> openSymbols(Long memberId);
}
