package coin.coinzzickmock.feature.position.infrastructure.persistence;

import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAUpdateClause;
import com.querydsl.jpa.impl.JPAQueryFactory;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public PositionMutationResult updateWithVersion(
            String memberId,
            PositionSnapshot expectedPosition,
            PositionSnapshot nextPosition
    ) {
        PositionSnapshot versioned = nextPosition.withVersion(expectedPosition.version() + 1);
        long affectedRows = applyPositionUpdate(
                jpaQueryFactory.update(position)
                        .where(
                                position.getString("memberId").eq(memberId),
                                position.getString("symbol").eq(expectedPosition.symbol()),
                                position.getString("positionSide").eq(expectedPosition.positionSide()),
                                position.getString("marginMode").eq(expectedPosition.marginMode()),
                                position.getNumber("version", Long.class).eq(expectedPosition.version())
                        ),
                versioned
        ).execute();

        if (affectedRows > 0) {
            return PositionMutationResult.updated(affectedRows, versioned);
        }

        return findOpenPosition(memberId, expectedPosition.symbol(), expectedPosition.positionSide(), expectedPosition.marginMode())
                .map(PositionMutationResult::staleVersion)
                .orElseGet(PositionMutationResult::notFound);
    }

    @Override
    @Transactional
    public PositionMutationResult deleteWithVersion(String memberId, PositionSnapshot expectedPosition) {
        long affectedRows = jpaQueryFactory.delete(position)
                .where(
                        position.getString("memberId").eq(memberId),
                        position.getString("symbol").eq(expectedPosition.symbol()),
                        position.getString("positionSide").eq(expectedPosition.positionSide()),
                        position.getString("marginMode").eq(expectedPosition.marginMode()),
                        position.getNumber("version", Long.class).eq(expectedPosition.version())
                )
                .execute();

        if (affectedRows > 0) {
            return PositionMutationResult.deleted(affectedRows);
        }

        return findOpenPosition(memberId, expectedPosition.symbol(), expectedPosition.positionSide(), expectedPosition.marginMode())
                .map(PositionMutationResult::staleVersion)
                .orElseGet(PositionMutationResult::notFound);
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

    private JPAUpdateClause applyPositionUpdate(JPAUpdateClause update, PositionSnapshot positionSnapshot) {
        return update
                .set(position.getString("symbol"), positionSnapshot.symbol())
                .set(position.getString("positionSide"), positionSnapshot.positionSide())
                .set(position.getString("marginMode"), positionSnapshot.marginMode())
                .set(position.getNumber("leverage", Integer.class), positionSnapshot.leverage())
                .set(position.get("quantity", BigDecimal.class), decimal(positionSnapshot.quantity()))
                .set(position.get("entryPrice", BigDecimal.class), decimal(positionSnapshot.entryPrice()))
                .set(position.get("markPrice", BigDecimal.class), decimal(positionSnapshot.markPrice()))
                .set(position.get("liquidationPrice", BigDecimal.class),
                        positionSnapshot.liquidationPrice() == null ? null : decimal(positionSnapshot.liquidationPrice()))
                .set(position.get("unrealizedPnl", BigDecimal.class), decimal(positionSnapshot.unrealizedPnl()))
                .set(position.getDateTime("openedAt", java.time.Instant.class), positionSnapshot.openedAt())
                .set(position.get("originalQuantity", BigDecimal.class), decimal(positionSnapshot.originalQuantity()))
                .set(position.get("accumulatedClosedQuantity", BigDecimal.class), decimal(positionSnapshot.accumulatedClosedQuantity()))
                .set(position.get("accumulatedExitNotional", BigDecimal.class), decimal(positionSnapshot.accumulatedExitNotional()))
                .set(position.get("accumulatedRealizedPnl", BigDecimal.class), decimal(positionSnapshot.accumulatedRealizedPnl()))
                .set(position.get("accumulatedOpenFee", BigDecimal.class), decimal(positionSnapshot.accumulatedOpenFee()))
                .set(position.get("accumulatedCloseFee", BigDecimal.class), decimal(positionSnapshot.accumulatedCloseFee()))
                .set(position.get("accumulatedFundingCost", BigDecimal.class), decimal(positionSnapshot.accumulatedFundingCost()))
                .set(position.getNumber("version", Long.class), positionSnapshot.version());
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
