package stock.stockzzickmock.storage.redis.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.market.MarketIndices;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndicesRedisDto {

    private String prev;

    private String sign;

    private String prev_rate;

    private List<KisIndicesRedisDto> indices;

    public MarketIndices toDomain() {
        return MarketIndices.builder()
                .prev(prev)
                .sign(sign)
                .prevRate(prev_rate)
                .indices(indices.stream()
                        .map(KisIndicesRedisDto::toDomain)
                        .toList())
                .build();
    }
}
