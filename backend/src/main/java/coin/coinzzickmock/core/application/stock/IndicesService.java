package coin.coinzzickmock.core.application.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import coin.coinzzickmock.core.application.stock.implement.MarketLoader;
import coin.coinzzickmock.core.application.stock.result.MarketIndicesResult;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndicesService {

    private final MarketLoader marketLoader;

    public MarketIndicesResult getIndicesInfo(String market) {
        return MarketIndicesResult.from(marketLoader.loadIndices(market));
    }
}
