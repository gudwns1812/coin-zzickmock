package coin.coinzzickmock.feature.position.infrastructure.persistence;

import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaPositionRepository implements PositionRepository {
    private final OpenPositionSpringDataRepository openPositionSpringDataRepository;
    private final JPAQueryFactory jpaQueryFactory;
    private final PathBuilder<OpenPositionJpaEntity> position = new PathBuilder<>(OpenPositionJpaEntity.class, "position");

    public JpaPositionRepository(
            OpenPositionSpringDataRepository openPositionSpringDataRepository,
            JPAQueryFactory jpaQueryFactory
    ) {
        this.openPositionSpringDataRepository = openPositionSpringDataRepository;
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PositionSnapshot> findOpenPositions(String memberId) {
        return jpaQueryFactory.selectFrom(position)
                .where(position.getString("memberId").eq(memberId))
                .orderBy(position.getString("symbol").asc())
                .fetch()
                .stream()
                .map(OpenPositionJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PositionSnapshot> findOpenPosition(
            String memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        OpenPositionJpaEntity entity = jpaQueryFactory.selectFrom(position)
                .where(
                        position.getString("memberId").eq(memberId),
                        position.getString("symbol").eq(symbol),
                        position.getString("positionSide").eq(positionSide),
                        position.getString("marginMode").eq(marginMode)
                )
                .fetchOne();
        return Optional.ofNullable(entity).map(OpenPositionJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
        OpenPositionJpaEntity entity = jpaQueryFactory.selectFrom(position)
                .where(
                        position.getString("memberId").eq(memberId),
                        position.getString("symbol").eq(positionSnapshot.symbol()),
                        position.getString("positionSide").eq(positionSnapshot.positionSide()),
                        position.getString("marginMode").eq(positionSnapshot.marginMode())
                )
                .fetchOne();

        if (entity == null) {
            entity = OpenPositionJpaEntity.from(memberId, positionSnapshot);
        } else {
            entity.apply(positionSnapshot);
        }

        return openPositionSpringDataRepository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public void delete(String memberId, String symbol, String positionSide, String marginMode) {
        jpaQueryFactory.delete(position)
                .where(
                        position.getString("memberId").eq(memberId),
                        position.getString("symbol").eq(symbol),
                        position.getString("positionSide").eq(positionSide),
                        position.getString("marginMode").eq(marginMode)
                )
                .execute();
    }
}
