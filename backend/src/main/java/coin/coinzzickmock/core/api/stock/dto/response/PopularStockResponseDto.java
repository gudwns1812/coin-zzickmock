package coin.coinzzickmock.core.api.stock.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import coin.coinzzickmock.core.application.stock.result.PopularStockResult;

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

    public static PopularStockResponseDto from(PopularStockResult dto) {
        return new PopularStockResponseDto(
                dto.stockName(),
                dto.stockCode(),
                dto.rank(),
                dto.price(),
                dto.sign(),
                dto.changeAmount(),
                dto.changeRate(),
                dto.stockImage()
        );
    }
}
