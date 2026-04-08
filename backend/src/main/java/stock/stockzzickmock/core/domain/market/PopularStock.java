package stock.stockzzickmock.core.domain.market;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PopularStock {

    private final String stockName;
    private final String stockCode;
    private final String rank;
    private final String price;
    private final String sign;
    private final String changeAmount;
    private final String changeRate;
    private final String stockImage;
}
