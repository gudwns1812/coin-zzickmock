package coin.coinzzickmock.feature.position.infrastructure.persistence;

import static coin.coinzzickmock.feature.position.infrastructure.persistence.QOpenPositionEntity.openPositionEntity;

import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class PositionPersistenceRepository implements PositionRepository {
    private final OpenPositionEntityRepository openPositionEntityRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    @Transactional(readOnly = true)
    public List<PositionSnapshot> findOpenPositions(Long memberId) {
        return jpaQueryFactory.selectFrom(openPositionEntity)
                .where(openPositionEntity.memberId.eq(memberId))
                .orderBy(openPositionEntity.symbol.asc())
                .fetch()
                .stream()
                .map(OpenPositionEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PositionSnapshot> findOpenPosition(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        OpenPositionEntity entity = jpaQueryFactory.selectFrom(openPositionEntity)
                .where(
                        openPositionEntity.memberId.eq(memberId),
                        openPositionEntity.symbol.eq(symbol),
                        openPositionEntity.positionSide.eq(positionSide),
                        openPositionEntity.marginMode.eq(marginMode)
                )
                .fetchOne();
        return Optional.ofNullable(entity).map(OpenPositionEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PositionSnapshot> findOpenPosition(Long memberId, String symbol, String positionSide) {
        OpenPositionEntity entity = jpaQueryFactory.selectFrom(openPositionEntity)
                .where(
                        openPositionEntity.memberId.eq(memberId),
                        openPositionEntity.symbol.eq(symbol),
                        openPositionEntity.positionSide.eq(positionSide)
                )
                .orderBy(openPositionEntity.marginMode.asc())
                .fetchFirst();
        return Optional.ofNullable(entity).map(OpenPositionEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
        return jpaQueryFactory.selectFrom(openPositionEntity)
                .where(openPositionEntity.symbol.eq(symbol))
                .orderBy(
                        openPositionEntity.memberId.asc(),
                        openPositionEntity.positionSide.asc(),
                        openPositionEntity.marginMode.asc()
                )
                .fetch()
                .stream()
                .map(entity -> new OpenPositionCandidate(entity.memberId(), entity.toDomain()))
                .toList();
    }

    @Override
    @Transactional
    public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
        OpenPositionEntity entity = jpaQueryFactory.selectFrom(openPositionEntity)
                .where(
                        openPositionEntity.memberId.eq(memberId),
                        openPositionEntity.symbol.eq(positionSnapshot.symbol()),
                        openPositionEntity.positionSide.eq(positionSnapshot.positionSide()),
                        openPositionEntity.marginMode.eq(positionSnapshot.marginMode())
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
            Long memberId,
            PositionSnapshot expectedPosition,
            PositionSnapshot nextPosition
    ) {
        PositionSnapshot versioned = nextPosition.withVersion(expectedPosition.version() + 1);
        long affectedRows = applyPositionUpdate(
                jpaQueryFactory.update(openPositionEntity)
                        .where(
                                openPositionEntity.memberId.eq(memberId),
                                openPositionEntity.symbol.eq(expectedPosition.symbol()),
                                openPositionEntity.positionSide.eq(expectedPosition.positionSide()),
                                openPositionEntity.marginMode.eq(expectedPosition.marginMode()),
                                openPositionEntity.version.eq(expectedPosition.version())
                        ),
                versioned
        ).execute();

        if (affectedRows > 0) {
            return PositionMutationResult.updated(affectedRows, versioned);
        }

        return findOpenPosition(memberId, expectedPosition.symbol(), expectedPosition.positionSide(),
                expectedPosition.marginMode())
                .map(PositionMutationResult::staleVersion)
                .orElseGet(PositionMutationResult::notFound);
    }

    @Override
    @Transactional
    public PositionMutationResult deleteWithVersion(Long memberId, PositionSnapshot expectedPosition) {
        long affectedRows = jpaQueryFactory.delete(openPositionEntity)
                .where(
                        openPositionEntity.memberId.eq(memberId),
                        openPositionEntity.symbol.eq(expectedPosition.symbol()),
                        openPositionEntity.positionSide.eq(expectedPosition.positionSide()),
                        openPositionEntity.marginMode.eq(expectedPosition.marginMode()),
                        openPositionEntity.version.eq(expectedPosition.version())
                )
                .execute();

        if (affectedRows > 0) {
            return PositionMutationResult.deleted(affectedRows);
        }

        return findOpenPosition(memberId, expectedPosition.symbol(), expectedPosition.positionSide(),
                expectedPosition.marginMode())
                .map(PositionMutationResult::staleVersion)
                .orElseGet(PositionMutationResult::notFound);
    }

    @Override
    @Transactional
    public boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode) {
        return jpaQueryFactory.delete(openPositionEntity)
                .where(
                        openPositionEntity.memberId.eq(memberId),
                        openPositionEntity.symbol.eq(symbol),
                        openPositionEntity.positionSide.eq(positionSide),
                        openPositionEntity.marginMode.eq(marginMode)
                )
                .execute() > 0;
    }

    @Override
    @Transactional
    public void delete(Long memberId, String symbol, String positionSide, String marginMode) {
        deleteIfOpen(memberId, symbol, positionSide, marginMode);
    }

    private JPAUpdateClause applyPositionUpdate(JPAUpdateClause update, PositionSnapshot positionSnapshot) {
        return update
                .set(openPositionEntity.symbol, positionSnapshot.symbol())
                .set(openPositionEntity.positionSide, positionSnapshot.positionSide())
                .set(openPositionEntity.marginMode, positionSnapshot.marginMode())
                .set(openPositionEntity.leverage, positionSnapshot.leverage())
                .set(openPositionEntity.quantity, decimal(positionSnapshot.quantity()))
                .set(openPositionEntity.entryPrice, decimal(positionSnapshot.entryPrice()))
                .set(openPositionEntity.markPrice, decimal(positionSnapshot.markPrice()))
                .set(openPositionEntity.liquidationPrice,
                        positionSnapshot.liquidationPrice() == null ? null
                                : decimal(positionSnapshot.liquidationPrice()))
                .set(openPositionEntity.unrealizedPnl, decimal(positionSnapshot.unrealizedPnl()))
                .set(openPositionEntity.openedAt, positionSnapshot.openedAt())
                .set(openPositionEntity.originalQuantity, decimal(positionSnapshot.originalQuantity()))
                .set(openPositionEntity.accumulatedClosedQuantity,
                        decimal(positionSnapshot.accumulatedClosedQuantity()))
                .set(openPositionEntity.accumulatedExitNotional, decimal(positionSnapshot.accumulatedExitNotional()))
                .set(openPositionEntity.accumulatedRealizedPnl, decimal(positionSnapshot.accumulatedRealizedPnl()))
                .set(openPositionEntity.accumulatedOpenFee, decimal(positionSnapshot.accumulatedOpenFee()))
                .set(openPositionEntity.accumulatedCloseFee, decimal(positionSnapshot.accumulatedCloseFee()))
                .set(openPositionEntity.accumulatedFundingCost, decimal(positionSnapshot.accumulatedFundingCost()))
                .set(openPositionEntity.takeProfitPrice,
                        positionSnapshot.takeProfitPrice() == null ? null : decimal(positionSnapshot.takeProfitPrice()))
                .set(openPositionEntity.stopLossPrice,
                        positionSnapshot.stopLossPrice() == null ? null : decimal(positionSnapshot.stopLossPrice()))
                .set(openPositionEntity.version, positionSnapshot.version());
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
