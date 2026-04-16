package coin.coinzzickmock.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "coin.coinzzickmock")
@EntityScan(basePackages = "coin.coinzzickmock")
@EnableJpaRepositories(basePackages = "coin.coinzzickmock")
public class CoinZzickmockApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinZzickmockApplication.class, args);
    }
}
