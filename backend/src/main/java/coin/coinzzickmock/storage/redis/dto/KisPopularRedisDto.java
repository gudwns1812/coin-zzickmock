package coin.coinzzickmock.storage.redis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import coin.coinzzickmock.core.domain.market.PopularStock;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KisPopularRedisDto {

    @JsonProperty("hts_kor_isnm")
    private String stockName;

    @JsonProperty("mksc_shrn_iscd")
    private String stockCode;

    @JsonProperty("data_rank")
    private String rank;

    @JsonProperty("stck_prpr")
    private String price;

    @JsonProperty("prdy_vrss_sign")
    private String sign;

    @JsonProperty("prdy_vrss")
    private String changeAmount;

    @JsonProperty("prdy_ctrt")
    private String changeRate;

    private String stockImage;

    public PopularStock toDomain() {
        return PopularStock.builder()
                .stockName(stockName)
                .stockCode(stockCode)
                .rank(rank)
                .price(price)
                .sign(sign)
                .changeAmount(changeAmount)
                .changeRate(changeRate)
                .stockImage(stockImage)
                .build();
    }
}
