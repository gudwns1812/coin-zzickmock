package coin.coinzzickmock.storage.redis.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FxRedisDto {

    private String changePrice;

    private String changeSign;

    private String changeRate;

    private String prevPrice;

    private String highPrice;

    private String lowPrice;

    private String openPrice;

    private String currentPrice;

    private List<KisFxPastInfoDto> pastInfo;

}
