package coin.coinzzickmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {
        "coin.coinzzickmock.feature.push",
        "coin.coinzzickmock.common.web.security",
        "coin.coinzzickmock.providers.infrastructure"
})
@EnableScheduling
public class PushServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PushServerApplication.class, args);
    }
}
