package coin.coinzzickmock.core.api.stock.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import coin.coinzzickmock.core.application.stock.result.MarketIndicesResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndicesResponseDto {

    private String prev;

    private String sign;

    @JsonProperty("prev_rate")
    private String prevRate;

    private List<IndicesItemResponseDto> indices;

    public static IndicesResponseDto from(MarketIndicesResult indices) {
        return new IndicesResponseDto(
                indices.prev(),
                indices.sign(),
                indices.prevRate(),
                indices.indices().stream()
                        .map(IndicesItemResponseDto::from)
                        .toList()
        );
    }
}
