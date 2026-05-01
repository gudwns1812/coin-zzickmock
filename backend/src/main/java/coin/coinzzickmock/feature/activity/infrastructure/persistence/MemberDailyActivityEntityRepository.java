package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberDailyActivityEntityRepository
        extends JpaRepository<MemberDailyActivityEntity, MemberDailyActivityId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select activity from MemberDailyActivityEntity activity where activity.id = :id")
    Optional<MemberDailyActivityEntity> findByIdForUpdate(@Param("id") MemberDailyActivityId id);

    long countByIdActivityDate(LocalDate activityDate);
}
