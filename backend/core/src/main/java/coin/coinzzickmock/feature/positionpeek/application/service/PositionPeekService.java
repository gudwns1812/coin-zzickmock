package coin.coinzzickmock.feature.positionpeek.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.positionpeek.application.repository.PositionPeekSnapshotRepository;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekPublicPositionResult;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekSnapshotRecord;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekSnapshotResult;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekStatusResult;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekTargetResult;
import coin.coinzzickmock.feature.reward.application.repository.RewardItemBalanceRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.domain.RewardItemBalance;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PositionPeekService {
    private final PositionPeekTargetTokenCodec targetTokenService;
    private final LeaderboardProjectionRepository leaderboardProjectionRepository;
    private final RewardShopItemRepository rewardShopItemRepository;
    private final RewardItemBalanceRepository rewardItemBalanceRepository;
    private final PublicPositionSnapshotReader publicPositionSnapshotReader;
    private final PositionPeekSnapshotRepository positionPeekSnapshotRepository;
    private final PositionPeekItemBalanceReader itemBalanceService;

    @Transactional(readOnly = true)
    public PositionPeekStatusResult latest(Long viewerMemberId, String targetToken) {
        ResolvedTarget target = resolveTarget(targetToken);
        PositionPeekSnapshotResult latest = positionPeekSnapshotRepository
                .findLatestByViewerMemberIdAndTargetMemberId(viewerMemberId, target.memberId())
                .map(record -> PositionPeekSnapshotResult.from(record, null))
                .orElse(null);
        return PositionPeekStatusResult.from(
                target.result(),
                latest,
                itemBalanceService.getRemainingCount(viewerMemberId)
        );
    }

    @Transactional
    public PositionPeekStatusResult consume(Long viewerMemberId, String targetToken) {
        ResolvedTarget target = resolveTarget(targetToken);
        RewardShopItem item = rewardShopItemRepository.findByCode(PositionPeekItemBalanceReader.POSITION_PEEK_ITEM_CODE)
                .filter(RewardShopItem::positionPeek)
                .orElseThrow(this::invalid);
        RewardItemBalance consumed = rewardItemBalanceRepository
                .findByMemberIdAndShopItemIdForUpdate(viewerMemberId, item.id())
                .orElse(RewardItemBalance.empty(viewerMemberId, item.id()))
                .consumeOne();
        RewardItemBalance savedBalance = rewardItemBalanceRepository.save(consumed);
        List<PositionPeekPublicPositionResult> positions = publicPositionSnapshotReader.read(target.memberId());
        PositionPeekSnapshotRecord savedSnapshot = positionPeekSnapshotRepository.save(new PositionPeekSnapshotRecord(
                null,
                UUID.randomUUID().toString(),
                viewerMemberId,
                target.memberId(),
                targetTokenService.fingerprint(targetToken),
                target.result().nickname(),
                target.result().rank(),
                target.leaderboardMode(),
                Instant.now(),
                positions
        ));
        return PositionPeekStatusResult.from(
                target.result(),
                PositionPeekSnapshotResult.from(savedSnapshot, savedBalance.remainingQuantity()),
                savedBalance.remainingQuantity()
        );
    }

    @Transactional(readOnly = true)
    public PositionPeekSnapshotResult getSnapshot(Long viewerMemberId, String peekId) {
        return positionPeekSnapshotRepository.findByPeekIdAndViewerMemberId(peekId, viewerMemberId)
                .map(record -> PositionPeekSnapshotResult.from(record, null))
                .orElseThrow(this::invalid);
    }

    private ResolvedTarget resolveTarget(String targetToken) {
        PositionPeekTargetTokenCodec.TargetTokenPayload payload = targetTokenService.validate(targetToken);
        LeaderboardEntry entry = leaderboardProjectionRepository.findByMemberId(payload.targetMemberId())
                .orElseThrow(this::invalid);
        return new ResolvedTarget(
                entry.memberId(),
                PositionPeekTargetResult.from(entry, payload.rank(), targetToken),
                payload.leaderboardMode()
        );
    }

    private CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }

    private record ResolvedTarget(
            Long memberId,
            PositionPeekTargetResult result,
            String leaderboardMode
    ) {
    }
}
