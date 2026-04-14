package coin.coinzzickmock.storage.redis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KisFxPastInfoDto {

    @JsonProperty("stck_bsop_date")
    private String date;

    @JsonProperty("ovrs_nmix_prpr")
    private String currentPrice;

    @JsonProperty("ovrs_nmix_oprc")
    private String openPrice;

    @JsonProperty("ovrs_nmix_hgpr")
    private String highPrice;

    @JsonProperty("ovrs_nmix_lwpr")
    private String lowPrice;
}
