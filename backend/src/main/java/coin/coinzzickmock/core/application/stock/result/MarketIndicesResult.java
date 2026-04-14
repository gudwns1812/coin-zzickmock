package coin.coinzzickmock.core.application.stock.result;

import java.util.List;
import coin.coinzzickmock.core.domain.market.MarketIndices;

public record MarketIndicesResult(
        String prev,
        String sign,
        String prevRate,
        List<MarketIndexResult> indices
) {

    public static MarketIndicesResult from(MarketIndices indices) {
        return new MarketIndicesResult(
                indices.getPrev(),
                indices.getSign(),
                indices.getPrevRate(),
                indices.getIndices().stream()
                        .map(MarketIndexResult::from)
                        .toList()
        );
    }
}
