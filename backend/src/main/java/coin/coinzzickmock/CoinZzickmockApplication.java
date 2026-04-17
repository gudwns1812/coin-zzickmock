package coin.coinzzickmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoinZzickmockApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinZzickmockApplication.class, args);
    }
}
