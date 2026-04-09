package stock.stockzzickmock.core.application.stock.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stock.stockzzickmock.storage.db.stock.entity.StockEntity;
import stock.stockzzickmock.storage.db.stock.repository.StockJpaRepository;
import stock.stockzzickmock.storage.redis.stock.publisher.StockActiveSetPublisher;

@ExtendWith(MockitoExtension.class)
class StockCommandHandlerTest {

    @Mock
    private StockJpaRepository stockJpaRepository;

    @Mock
    private StockActiveSetPublisher stockActiveSetPublisher;

    @InjectMocks
    private StockCommandHandler stockCommandHandler;

    @Test
    void increasesSearchCountWhenStockExists() {
        StockEntity stockEntity = StockEntity.builder()
                .id(1L)
                .stockCode("005930")
                .name("삼성전자")
                .stockSearchCount(10)
                .build();
        when(stockJpaRepository.findByStockCode("005930"))
                .thenReturn(Optional.of(stockEntity));

        stockCommandHandler.increaseSearchCount("005930");

        ArgumentCaptor<StockEntity> captor = ArgumentCaptor.forClass(StockEntity.class);
        verify(stockJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getStockCode()).isEqualTo("005930");
        assertThat(captor.getValue().getStockSearchCount()).isEqualTo(11);
    }

    @Test
    void doesNotSaveWhenStockDoesNotExist() {
        when(stockJpaRepository.findByStockCode("404"))
                .thenReturn(Optional.empty());

        stockCommandHandler.increaseSearchCount("404");

        verify(stockJpaRepository, never()).save(any());
    }

    @Test
    void publishesActiveStockSetSnapshot() {
        stockCommandHandler.publishActiveStockSet("portfolio", List.of("005930", "000660"));

        verify(stockActiveSetPublisher).publish("portfolio", List.of("005930", "000660"));
    }
}
