package stock.stockzzickmock.core.application.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.stockzzickmock.core.api.stock.dto.response.PopularStockResponseDto;
import stock.stockzzickmock.core.application.stock.implement.PopularStockLoader;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularService {

    private final PopularStockLoader popularStockLoader;

    public  List<PopularStockResponseDto> getPopularTop6Stock() {
        return popularStockLoader.load().stream()
                .map(PopularStockResponseDto::from)
                .toList();
    }

}
