package stock.stockzzickmock.core.api.stock.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.core.domain.market.MarketIndexSnapshot;

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

    public static IndicesItemResponseDto from(MarketIndexSnapshot snapshot) {
        return new IndicesItemResponseDto(
                snapshot.getDate(),
                snapshot.getCurrentPrice(),
                snapshot.getHighPrice(),
                snapshot.getLowPrice(),
                snapshot.getAccumulatedVolume(),
                snapshot.getAccumulatedVolumePrice()
        );
    }
}
