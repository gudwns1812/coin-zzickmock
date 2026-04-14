package coin.coinzzickmock.core.application.stock.implement;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import coin.coinzzickmock.core.domain.stock.Stock;
import coin.coinzzickmock.storage.db.stock.entity.StockEntity;
import coin.coinzzickmock.storage.db.stock.repository.StockJpaRepository;
import coin.coinzzickmock.storage.redis.stock.publisher.StockActiveSetPublisher;

@Component
@RequiredArgsConstructor
public class StockCommandHandler {

    private final StockJpaRepository stockJpaRepository;
    private final StockActiveSetPublisher stockActiveSetPublisher;

    @Transactional
    public void increaseSearchCount(String stockCode) {
        stockJpaRepository.findByStockCode(stockCode)
                .map(StockEntity::toDomain)
                .ifPresent(stock -> {
                    stock.incrementStockSearchCount();
                    stockJpaRepository.save(toEntity(stock));
                });
    }

    public void publishActiveStockSet(String source, List<String> stockCodes) {
        stockActiveSetPublisher.publish(source, stockCodes);
    }

    private StockEntity toEntity(Stock stock) {
        return StockEntity.builder()
                .id(stock.getId())
                .stockCode(stock.getStockCode())
                .name(stock.getName())
                .price(stock.getPrice())
                .openPrice(stock.getOpenPrice())
                .highPrice(stock.getHighPrice())
                .lowPrice(stock.getLowPrice())
                .marketName(stock.getMarketName())
                .stockImage(stock.getStockImage())
                .changeAmount(stock.getChangeAmount())
                .sign(stock.getSign())
                .changeRate(stock.getChangeRate())
                .volume(stock.getVolume())
                .volumeValue(stock.getVolumeValue())
                .stockSearchCount(stock.getStockSearchCount())
                .category(stock.getCategory())
                .build();
    }
}
