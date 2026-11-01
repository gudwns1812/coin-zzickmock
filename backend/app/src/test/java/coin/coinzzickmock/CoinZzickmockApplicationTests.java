package coin.coinzzickmock;

import coin.coinzzickmock.testsupport.IntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@IntegrationTest
@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class CoinZzickmockApplicationTests {
}
