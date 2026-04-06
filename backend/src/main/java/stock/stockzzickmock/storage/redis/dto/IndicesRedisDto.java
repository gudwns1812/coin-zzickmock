package stock.stockzzickmock.storage.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndicesRedisDto {

    private String prev;

    private String sign;

    private String prev_rate;

    private List<KisIndicesRedisDto> indices;

}
