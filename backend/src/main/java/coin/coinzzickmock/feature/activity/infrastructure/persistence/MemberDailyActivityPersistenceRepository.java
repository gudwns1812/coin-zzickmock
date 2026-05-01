package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import coin.coinzzickmock.feature.activity.application.repository.MemberDailyActivityRepository;
import coin.coinzzickmock.feature.activity.domain.MemberDailyActivity;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberDailyActivityPersistenceRepository implements MemberDailyActivityRepository {
    private final MemberDailyActivityEntityRepository entityRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<MemberDailyActivity> findByDateAndMemberIdForUpdate(LocalDate activityDate, Long memberId) {
        return entityRepository.findByIdForUpdate(new MemberDailyActivityId(activityDate, memberId))
                .map(MemberDailyActivityEntity::toDomain);
    }

    @Override
    public MemberDailyActivity save(MemberDailyActivity activity) {
        MemberDailyActivityId id = new MemberDailyActivityId(activity.activityDate(), activity.memberId());
        MemberDailyActivityEntity entity = entityRepository.findById(id)
                .map(existing -> {
                    existing.apply(activity);
                    return existing;
                })
                .orElseGet(() -> MemberDailyActivityEntity.from(activity));
        try {
            return entityRepository.saveAndFlush(entity).toDomain();
        } catch (DataIntegrityViolationException exception) {
            entityManager.clear();
            throw exception;
        }
    }

    @Override
    public long countByDate(LocalDate activityDate) {
        return entityRepository.countByIdActivityDate(activityDate);
    }
}
