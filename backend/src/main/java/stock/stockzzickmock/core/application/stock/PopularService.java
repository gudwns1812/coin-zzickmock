package stock.stockzzickmock.core.application.stock;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.stockzzickmock.core.application.stock.implement.MarketLoader;
import stock.stockzzickmock.core.domain.market.PopularStock;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularService {

    private final MarketLoader marketLoader;

    public List<PopularStock> getPopularTop6Stock() {
        return marketLoader.loadPopularTop6();
    }
}
