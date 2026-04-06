package stock.stockzzickmock.extern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import stock.stockzzickmock.core.domain.stock.Stock;
import stock.stockzzickmock.core.domain.stock.StockHistory;
import stock.stockzzickmock.storage.db.stock.StockHistoryRepository;
import stock.stockzzickmock.storage.db.stock.StockRepository;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        stockHistoryRepository.deleteAll();
        stockRepository.deleteAll();
        valueOperations = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(valueOperations.get(anyString())).thenReturn(null);

        stockRepository.save(
                Stock.builder()
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
                Stock.builder()
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
                StockHistory.builder()
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
