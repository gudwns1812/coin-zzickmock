package coin.coinzzickmock.feature.position.application.repository;

import java.util.List;

public interface OpenPositionSymbolsReader {
    List<String> openSymbols(Long memberId);
}
