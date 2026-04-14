package coin.coinzzickmock.core.application.stock.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import coin.coinzzickmock.core.application.stock.implement.result.CategoryStocksPage;
import coin.coinzzickmock.core.domain.stock.Stock;
import coin.coinzzickmock.core.domain.stock.StockHistory;
import coin.coinzzickmock.storage.db.stock.entity.StockEntity;
import coin.coinzzickmock.storage.db.stock.entity.StockHistoryEntity;
import coin.coinzzickmock.storage.db.stock.repository.StockHistoryJpaRepository;
import coin.coinzzickmock.storage.db.stock.repository.StockJpaRepository;
import coin.coinzzickmock.storage.redis.dto.StockDto;
import coin.coinzzickmock.support.error.CoreException;
import coin.coinzzickmock.support.error.StockErrorType;

@ExtendWith(MockitoExtension.class)
class StockLoaderTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StockJpaRepository stockJpaRepository;

    @Mock
    private StockHistoryJpaRepository stockHistoryJpaRepository;

    private StockLoader stockLoader;

    @BeforeEach
    void setUp() {
        stockLoader = new StockLoader(
                redisTemplate,
                objectMapper,
                stockJpaRepository,
                stockHistoryJpaRepository
        );
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void returnsRedisStockInfoWhenPresent() {
        Object redisValue = Map.of("stockCode", "005930");
        StockDto stockDto = new StockDto(
                "005930",
                "삼성전자",
                "전기·전자",
                "70000",
                null,
                null,
                null,
                "KOSPI",
                "1000",
                "2",
                "1.45",
                "1000",
                "90000000",
                "image"
        );
        when(valueOperations.get("STOCK:005930")).thenReturn(redisValue);
        when(objectMapper.convertValue(redisValue, StockDto.class)).thenReturn(stockDto);

        Stock result = stockLoader.loadInfo("005930");

        assertThat(result.getStockCode()).isEqualTo("005930");
        verify(stockJpaRepository, never()).findByStockCode(anyString());
    }

    @Test
    void fallsBackToDatabaseWhenRedisMisses() {
        when(valueOperations.get("STOCK:005930")).thenReturn(null);
        when(stockJpaRepository.findByStockCode("005930")).thenReturn(Optional.of(
                StockEntity.builder()
                        .stockCode("005930")
                        .name("삼성전자")
                        .category("전기·전자")
                        .price("70000")
                        .marketName("KOSPI")
                        .changeAmount("1000")
                        .sign("2")
                        .changeRate("1.45")
                        .volume("1000")
                        .volumeValue("90000000")
                        .stockImage("image")
                        .build()
        ));

        Stock result = stockLoader.loadInfo("005930");

        assertThat(result.getStockCode()).isEqualTo("005930");
        assertThat(result.getName()).isEqualTo("삼성전자");
    }

    @Test
    void throwsWhenStockDoesNotExistAnywhere() {
        when(valueOperations.get("STOCK:404")).thenReturn(null);
        when(stockJpaRepository.findByStockCode("404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockLoader.loadInfo("404"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(StockErrorType.STOCK_NOT_FOUND);
    }

    @Test
    void loadsStockHistoryByCodeAndType() {
        when(stockHistoryJpaRepository.findByStockCodeAndTypeOrderByDateAsc("005930", "D"))
                .thenReturn(List.of(
                        StockHistoryEntity.builder()
                                .stockCode("005930")
                                .date(LocalDate.of(2025, 1, 1))
                                .type("D")
                                .open("69000")
                                .high("70000")
                                .low("68000")
                                .close("69500")
                                .volume("1000")
                                .volumeAmount("70000000")
                                .prevPrice(500)
                                .build()
                ));

        List<StockHistory> result = stockLoader.loadHistory("005930", "D");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockCode()).isEqualTo("005930");
    }

    @Test
    void loadsCategories() {
        when(stockJpaRepository.findCategoryAll()).thenReturn(List.of("전기·전자", "서비스"));

        List<String> categories = stockLoader.loadCategories();

        assertThat(categories).containsExactly("전기·전자", "서비스");
    }

    @Test
    void loadsStocksByCategory() {
        when(stockJpaRepository.findAllByCategory("전기·전자"))
                .thenReturn(List.of(
                        StockEntity.builder().stockCode("005930").name("삼성전자").build(),
                        StockEntity.builder().stockCode("000660").name("SK하이닉스").build()
                ));

        List<Stock> stocks = stockLoader.loadByCategory("전기·전자");

        assertThat(stocks).extracting(Stock::getStockCode)
                .containsExactly("005930", "000660");
    }

    @Test
    void returnsSortedAndSlicedCategoryPage() {
        when(stockJpaRepository.findAllByCategory("전기·전자"))
                .thenReturn(List.of(
                        stockEntity("A", "1000"),
                        stockEntity("B", "900"),
                        stockEntity("C", "800"),
                        stockEntity("D", "700"),
                        stockEntity("E", "600"),
                        stockEntity("F", "500"),
                        stockEntity("G", "400"),
                        stockEntity("H", "300"),
                        stockEntity("I", "200"),
                        stockEntity("J", "100"),
                        stockEntity("K", "50")
                ));

        CategoryStocksPage page = stockLoader.loadCategoryPage("전기·전자", 2);

        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.stocks()).hasSize(1);
        assertThat(page.stocks().get(0).getStockCode()).isEqualTo("K");
    }

    @Test
    void returnsEmptyStocksWhenCategoryPageIsOutOfRange() {
        when(stockJpaRepository.findAllByCategory("전기·전자"))
                .thenReturn(List.of(stockEntity("A", "1000")));

        CategoryStocksPage page = stockLoader.loadCategoryPage("전기·전자", 3);

        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.stocks()).isEmpty();
    }

    private StockEntity stockEntity(String code, String volumeValue) {
        return StockEntity.builder()
                .stockCode(code)
                .name("종목-" + code)
                .volumeValue(volumeValue)
                .build();
    }
}
