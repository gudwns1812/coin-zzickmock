package coin.coinzzickmock.feature.position.infrastructure.persistence;

import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PositionPersistenceRepository implements PositionRepository {
    private final OpenPositionEntityRepository openPositionEntityRepository;
    private final JPAQueryFactory jpaQueryFactory;
    private final PathBuilder<OpenPositionEntity> position = new PathBuilder<>(OpenPositionEntity.class, "position");

    @Override
    @Transactional(readOnly = true)
    public List<PositionSnapshot> findOpenPositions(String memberId) {
        return jpaQueryFactory.selectFrom(position)
                .where(position.getString("memberId").eq(memberId))
                .orderBy(position.getString("symbol").asc())
                .fetch()
                .stream()
                .map(OpenPositionEntity::toDomain)
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
        OpenPositionEntity entity = jpaQueryFactory.selectFrom(position)
                .where(
                        position.getString("memberId").eq(memberId),
                        position.getString("symbol").eq(symbol),
                        position.getString("positionSide").eq(positionSide),
                        position.getString("marginMode").eq(marginMode)
                )
                .fetchOne();
        return Optional.ofNullable(entity).map(OpenPositionEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
        return jpaQueryFactory.selectFrom(position)
                .where(position.getString("symbol").eq(symbol))
                .orderBy(
                        position.getString("memberId").asc(),
                        position.getString("positionSide").asc(),
                        position.getString("marginMode").asc()
                )
                .fetch()
                .stream()
                .map(entity -> new OpenPositionCandidate(entity.memberId(), entity.toDomain()))
                .toList();
    }

    @Override
    @Transactional
    public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
        OpenPositionEntity entity = jpaQueryFactory.selectFrom(position)
                .where(
                        position.getString("memberId").eq(memberId),
                        position.getString("symbol").eq(positionSnapshot.symbol()),
                        position.getString("positionSide").eq(positionSnapshot.positionSide()),
                        position.getString("marginMode").eq(positionSnapshot.marginMode())
                )
                .fetchOne();

        if (entity == null) {
            entity = OpenPositionEntity.from(memberId, positionSnapshot);
        } else {
            entity.apply(positionSnapshot);
        }

        return openPositionEntityRepository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public boolean deleteIfOpen(String memberId, String symbol, String positionSide, String marginMode) {
        return jpaQueryFactory.delete(position)
                .where(
                        position.getString("memberId").eq(memberId),
                        position.getString("symbol").eq(symbol),
                        position.getString("positionSide").eq(positionSide),
                        position.getString("marginMode").eq(marginMode)
                )
                .execute() > 0;
    }

    @Override
    @Transactional
    public void delete(String memberId, String symbol, String positionSide, String marginMode) {
        deleteIfOpen(memberId, symbol, positionSide, marginMode);
    }
}
