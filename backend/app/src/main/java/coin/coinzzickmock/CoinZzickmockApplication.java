package coin.coinzzickmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(
        basePackages = "coin.coinzzickmock",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = {
                                "coin\\.coinzzickmock\\.common\\.web\\.Sse.*",
                                "coin\\.coinzzickmock\\.feature\\.order\\.web\\.TradingExecutionSseBroker",
                                "coin\\.coinzzickmock\\.feature\\.market\\.web\\.MarketStreamBroker",
                                "coin\\.coinzzickmock\\.feature\\.market\\.web\\.MarketRealtimeSseBroker",
                                "coin\\.coinzzickmock\\.feature\\.market\\.web\\.MarketCandleRealtimeSseBroker",
                                "coin\\.coinzzickmock\\.feature\\.market\\.web\\.MarketStreamRegistry",
                                "coin\\.coinzzickmock\\.feature\\.market\\.web\\.MarketSseStreamRouter",
                                "coin\\.coinzzickmock\\.feature\\.market\\.web\\.MarketSummaryStreamStrategy",
                                "coin\\.coinzzickmock\\.feature\\.market\\.web\\.MarketCandleStreamStrategy",
                                "coin\\.coinzzickmock\\.feature\\.market\\.web\\.UnifiedMarketStreamStrategy"
                        }
                )
        }
)
@EnableAsync
@EnableCaching
@EnableScheduling
public class CoinZzickmockApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinZzickmockApplication.class, args);
    }
}
