package stock.stockzzickmock.core.application.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.stockzzickmock.core.application.stock.implement.MarketLoader;
import stock.stockzzickmock.core.domain.market.MarketIndices;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndicesService {

    private final MarketLoader marketLoader;

    public MarketIndices getIndicesInfo(String market) {
        return marketLoader.loadIndices(market);
    }
}
