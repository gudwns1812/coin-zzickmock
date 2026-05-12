package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyActiveUserSummaryEntityRepository
        extends JpaRepository<DailyActiveUserSummaryEntity, LocalDate> {
}
