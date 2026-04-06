package stock.stockzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.storage.redis.dto.KisPopularRedisDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PopularStockResponseDto {

    private String stockName;

    private String stockCode;

    private String rank;

    private String price;

    private String sign;

    private String changeAmount;

    private String changeRate;

    private String stockImage;

    public static PopularStockResponseDto from(KisPopularRedisDto dto) {
        return new PopularStockResponseDto(
                dto.getStockName(),
                dto.getStockCode(),
                dto.getRank(),
                dto.getPrice(),
                dto.getSign(),
                dto.getChangeAmount(),
                dto.getChangeRate(),
                dto.getStockImage()
        );
    }
}
