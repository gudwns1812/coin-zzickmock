package stock.stockzzickmock.core.domain.market;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MarketIndices {

    private final String prev;
    private final String sign;
    private final String prevRate;
    private final List<MarketIndexSnapshot> indices;
}
