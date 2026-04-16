package coin.coinzzickmock.feature.position.infrastructure.persistence;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class JpaPositionRepositoryTest {
    @Autowired
    private PositionRepository positionRepository;

    @Test
    void savesAndLoadsOpenPositionThroughH2() {
        positionRepository.save("demo-member", new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.15,
                100000,
                100250,
                90000.0,
                37.5
        ));

        PositionSnapshot loaded = positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();

        assertEquals(0.15, loaded.quantity(), 0.000001);
        assertEquals(1, positionRepository.findOpenPositions("demo-member").size());
    }
}
