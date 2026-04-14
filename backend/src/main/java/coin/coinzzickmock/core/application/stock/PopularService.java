package coin.coinzzickmock.core.application.stock;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import coin.coinzzickmock.core.application.stock.implement.MarketLoader;
import coin.coinzzickmock.core.application.stock.result.PopularStockResult;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularService {

    private final MarketLoader marketLoader;

    public List<PopularStockResult> getPopularTop6Stock() {
        return marketLoader.loadPopularTop6().stream()
                .map(PopularStockResult::from)
                .toList();
    }
}
