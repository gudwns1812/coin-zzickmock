package coin.coinzzickmock.feature.activity.infrastructure.persistence;

import coin.coinzzickmock.feature.activity.application.repository.DailyActiveUserSummaryRepository;
import coin.coinzzickmock.feature.activity.domain.DailyActiveUserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DailyActiveUserSummaryPersistenceRepository implements DailyActiveUserSummaryRepository {
    private final DailyActiveUserSummaryEntityRepository entityRepository;

    @Override
    public DailyActiveUserSummary save(DailyActiveUserSummary summary) {
        DailyActiveUserSummaryEntity entity = entityRepository.findById(summary.activityDate())
                .map(existing -> {
                    existing.apply(summary);
                    return existing;
                })
                .orElseGet(() -> DailyActiveUserSummaryEntity.from(summary));
        return entityRepository.saveAndFlush(entity).toDomain();
    }
}
