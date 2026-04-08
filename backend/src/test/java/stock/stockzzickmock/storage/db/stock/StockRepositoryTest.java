package stock.stockzzickmock.storage.db.stock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import stock.stockzzickmock.storage.db.stock.entity.StockEntity;
import stock.stockzzickmock.storage.db.stock.repository.StockJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StockRepositoryTest {

    @Autowired
    private StockJpaRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
        stockRepository.save(
                StockEntity.builder()
                        .stockCode("005930")
                        .name("삼성전자")
                        .price("70000")
                        .marketName("KOSPI")
                        .category("전기·전자")
                        .stockSearchCount(10)
                        .build()
        );
        stockRepository.save(
                StockEntity.builder()
                        .stockCode("000660")
                        .name("SK하이닉스")
                        .price("180000")
                        .marketName("KOSPI")
                        .category("전기·전자")
                        .stockSearchCount(8)
                        .build()
        );
        stockRepository.save(
                StockEntity.builder()
                        .stockCode("035420")
                        .name("NAVER")
                        .price("200000")
                        .marketName("KOSPI")
                        .category("IT 서비스")
                        .stockSearchCount(3)
                        .build()
        );
    }

    @Test
    void searchByNameReturnsMatchingStocks() {
        var results = stockRepository.searchByName("삼성", PageRequest.of(0, 5));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStockCode()).isEqualTo("005930");
    }

    @Test
    void searchByStockCodeReturnsMatchingStocks() {
        var results = stockRepository.searchByStockCode("005", PageRequest.of(0, 5));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("삼성전자");
    }

    @Test
    void findTop6OrdersBySearchCountDescending() {
        var results = stockRepository.findTop6ByOrderByStockSearchCountDesc();

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getStockCode()).isEqualTo("005930");
        assertThat(results.get(1).getStockCode()).isEqualTo("000660");
    }
}
