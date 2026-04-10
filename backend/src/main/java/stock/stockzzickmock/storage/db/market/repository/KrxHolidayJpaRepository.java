package stock.stockzzickmock.storage.db.market.repository;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import stock.stockzzickmock.storage.db.market.entity.KrxHolidayEntity;

public interface KrxHolidayJpaRepository extends JpaRepository<KrxHolidayEntity, Long> {

    boolean existsByMarketCodeAndHolidayYear(String marketCode, Integer holidayYear);

    boolean existsByMarketCodeAndHolidayDate(String marketCode, LocalDate holidayDate);

    void deleteByMarketCodeAndHolidayYear(String marketCode, Integer holidayYear);
}
