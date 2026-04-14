package coin.coinzzickmock.core.api.stock.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import coin.coinzzickmock.core.application.stock.result.MarketIndexResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndicesItemResponseDto {

    @JsonProperty("stck_bsop_date")
    private String indiceDate;

    @JsonProperty("bstp_nmix_prpr")
    private String curPrice;

    @JsonProperty("bstp_nmix_hgpr")
    private String highPrice;

    @JsonProperty("bstp_nmix_lwpr")
    private String lowPrice;

    @JsonProperty("acml_vol")
    private String acmlVol;

    @JsonProperty("acml_tr_pbmn")
    private String acmlVolPrice;

    public static IndicesItemResponseDto from(MarketIndexResult snapshot) {
        return new IndicesItemResponseDto(
                snapshot.indiceDate(),
                snapshot.curPrice(),
                snapshot.highPrice(),
                snapshot.lowPrice(),
                snapshot.acmlVol(),
                snapshot.acmlVolPrice()
        );
    }
}
