package coin.coinzzickmock.extern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import coin.coinzzickmock.storage.db.stock.entity.StockEntity;
import coin.coinzzickmock.storage.db.stock.entity.StockHistoryEntity;
import coin.coinzzickmock.storage.db.stock.repository.StockHistoryJpaRepository;
import coin.coinzzickmock.storage.db.stock.repository.StockJpaRepository;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private StockHistoryJpaRepository stockHistoryRepository;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOperations;
    private StreamOperations<String, Object, Object> streamOperations;

    @BeforeEach
    void setUp() {
        stockHistoryRepository.deleteAll();
        stockRepository.deleteAll();
        valueOperations = Mockito.mock(ValueOperations.class);
        streamOperations = Mockito.mock(StreamOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        Mockito.when(valueOperations.get(any())).thenReturn(null);
        Mockito.when(streamOperations.add(any())).thenReturn(RecordId.of("1-0"));

        stockRepository.save(
                StockEntity.builder()
                        .stockCode("005930")
                        .name("삼성전자")
                        .price("70000")
                        .openPrice("69500")
                        .highPrice("70500")
                        .lowPrice("69000")
                        .marketName("KOSPI")
                        .stockImage("https://example.com/samsung.png")
                        .changeAmount("1000")
                        .sign("2")
                        .changeRate("1.45")
                        .volume("1000")
                        .volumeValue("90000000")
                        .stockSearchCount(10)
                        .category("전기·전자")
                        .build()
        );
        stockRepository.save(
                StockEntity.builder()
                        .stockCode("000660")
                        .name("SK하이닉스")
                        .price("180000")
                        .openPrice("178000")
                        .highPrice("181000")
                        .lowPrice("177000")
                        .marketName("KOSPI")
                        .stockImage("https://example.com/sk.png")
                        .changeAmount("2000")
                        .sign("2")
                        .changeRate("1.12")
                        .volume("2000")
                        .volumeValue("80000000")
                        .stockSearchCount(8)
                        .category("전기·전자")
                        .build()
        );
        stockHistoryRepository.save(
                StockHistoryEntity.builder()
                        .stockCode("005930")
                        .date(LocalDate.of(2025, 1, 1))
                        .type("D")
                        .open("69000")
                        .high("71000")
                        .low("68000")
                        .close("70000")
                        .volume("1000")
                        .volumeAmount("70000000")
                        .prevPrice(500)
                        .build()
        );
    }

    @AfterEach
    void tearDown() {
        stockHistoryRepository.deleteAll();
        stockRepository.deleteAll();
    }

    @Test
    void searchEndpointReturnsStocks() throws Exception {
        mockMvc.perform(get("/api/v2/stocks/search")
                        .param("keyword", "삼성")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].stockCode").value("005930"));
    }

    @Test
    void searchSelectionEndpointIncreasesCountOnly() throws Exception {
        mockMvc.perform(post("/api/v2/stocks/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stockCode": "005930"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        assertThat(stockRepository.findByStockCode("005930"))
                .isPresent()
                .get()
                .extracting(StockEntity::getStockSearchCount)
                .isEqualTo(11);
        Mockito.verify(streamOperations, Mockito.never()).add(any());
    }

    @Test
    void searchSelectionEndpointRejectsBlankStockCode() throws Exception {
        mockMvc.perform(post("/api/v2/stocks/search")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "stockCode": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("FAIL"));

        assertThat(stockRepository.findByStockCode("005930"))
                .isPresent()
                .get()
                .extracting(StockEntity::getStockSearchCount)
                .isEqualTo(10);
        Mockito.verify(streamOperations, Mockito.never()).add(any());
    }

    @Test
    void activeSetsEndpointPublishesSnapshot() throws Exception {
        mockMvc.perform(post("/api/v2/stocks/active-sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "portfolio",
                                  "stockCodes": ["005930", "000660"]
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        Mockito.verify(streamOperations).add(any());
    }

    @Test
    void activeSetsEndpointAcceptsEmptyStockCodes() throws Exception {
        mockMvc.perform(post("/api/v2/stocks/active-sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "recent-view",
                                  "stockCodes": []
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        Mockito.verify(streamOperations).add(any());
    }

    @Test
    void activeSetsEndpointRejectsBlankSource() throws Exception {
        mockMvc.perform(post("/api/v2/stocks/active-sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": " ",
                                  "stockCodes": ["005930"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("FAIL"));

        Mockito.verify(streamOperations, Mockito.never()).add(any());
    }

    @Test
    void categoryEndpointReturnsCategories() throws Exception {
        mockMvc.perform(get("/api/v2/stocks/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("전기·전자"));
    }

    @Test
    void categoryStockEndpointReturnsPagedStocks() throws Exception {
        mockMvc.perform(get("/api/v2/stocks/category/전기·전자")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.stocks[0].stockCode").value("005930"));
    }

    @Test
    void stockInfoEndpointReturnsStockData() throws Exception {
        mockMvc.perform(get("/api/v2/stocks/info/005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.stockName").value("삼성전자"));
    }

    @Test
    void stockHistoryEndpointReturnsPeriodData() throws Exception {
        mockMvc.perform(get("/api/v2/stocks/005930")
                        .param("period", "D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockCode").value("005930"))
                .andExpect(jsonPath("$.data[0].open").value("69000"));
    }
}
