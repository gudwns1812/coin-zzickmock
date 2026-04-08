package stock.stockzzickmock.core.api.stock.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.market.MarketIndices;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndicesResponseDto {

    private String prev;

    private String sign;

    @JsonProperty("prev_rate")
    private String prevRate;

    private List<IndicesItemResponseDto> indices;

    public static IndicesResponseDto from(MarketIndices indices) {
        return new IndicesResponseDto(
                indices.getPrev(),
                indices.getSign(),
                indices.getPrevRate(),
                indices.getIndices().stream()
                        .map(IndicesItemResponseDto::from)
                        .toList()
        );
    }
}
