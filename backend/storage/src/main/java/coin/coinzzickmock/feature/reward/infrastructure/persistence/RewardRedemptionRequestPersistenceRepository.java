package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardRedemptionRequestRepository;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RewardRedemptionRequestPersistenceRepository implements RewardRedemptionRequestRepository {
    private final RewardRedemptionRequestEntityRepository redemptionRequestEntityRepository;
    private final RewardShopItemEntityRepository shopItemEntityRepository;

    @Override
    @Transactional
    public RewardRedemptionRequest save(RewardRedemptionRequest request) {
        RewardShopItemEntity shopItem = shopItemEntityRepository.findById(request.shopItemId()).orElseThrow();
        RewardRedemptionRequestEntity entity = redemptionRequestEntityRepository.findByRequestId(request.requestId())
                .map(existing -> {
                    existing.apply(request, shopItem);
                    return existing;
                })
                .orElseGet(() -> RewardRedemptionRequestEntity.from(request, shopItem));
        return redemptionRequestEntityRepository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RewardRedemptionRequest> findByRequestId(String requestId) {
        return redemptionRequestEntityRepository.findByRequestId(requestId)
                .map(RewardRedemptionRequestEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<RewardRedemptionRequest> findByRequestIdForUpdate(String requestId) {
        return redemptionRequestEntityRepository.findWithLockingByRequestId(requestId)
                .map(RewardRedemptionRequestEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardRedemptionRequest> findByMemberId(Long memberId) {
        return redemptionRequestEntityRepository.findByMemberIdOrderByRequestedAtDesc(memberId).stream()
                .map(RewardRedemptionRequestEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardRedemptionRequest> findByStatus(RewardRedemptionStatus status) {
        return redemptionRequestEntityRepository.findByStatusOrderByRequestedAtDesc(status.name()).stream()
                .map(RewardRedemptionRequestEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public int claimPendingAsApproved(String requestId, Long adminMemberId, String adminMemo, Instant approvedAt) {
        return redemptionRequestEntityRepository.findWithLockingByRequestId(requestId)
                .filter(RewardRedemptionRequestEntity::isPending)
                .map(request -> {
                    request.approvePending(adminMemberId, adminMemo, approvedAt);
                    return 1;
                })
                .orElse(0);
    }

    @Override
    @Transactional
    public int claimPendingAsRejected(String requestId, Long adminMemberId, String adminMemo, Instant rejectedAt) {
        return redemptionRequestEntityRepository.findWithLockingByRequestId(requestId)
                .filter(RewardRedemptionRequestEntity::isPending)
                .map(request -> {
                    request.rejectPending(adminMemberId, adminMemo, rejectedAt);
                    return 1;
                })
                .orElse(0);
    }

    @Override
    @Transactional
    public int claimPendingAsCancelled(String requestId, Long memberId, Instant cancelledAt) {
        return redemptionRequestEntityRepository.findWithLockingByRequestId(requestId)
                .filter(RewardRedemptionRequestEntity::isPending)
                .filter(request -> request.getMemberId().equals(memberId))
                .map(request -> {
                    request.cancelPending(cancelledAt);
                    return 1;
                })
                .orElse(0);
    }
}
