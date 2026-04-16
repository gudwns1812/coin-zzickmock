package coin.coinzzickmock.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "coin.coinzzickmock")
public class CoinZzickmockApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinZzickmockApplication.class, args);
    }
}
