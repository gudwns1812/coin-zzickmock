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
    public List<RewardRedemptionRequest> findByMemberId(String memberId) {
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
    public int claimPendingAsApproved(String requestId, String adminMemberId, String adminMemo, Instant approvedAt) {
        return redemptionRequestEntityRepository.claimPendingAsApproved(requestId, adminMemberId, adminMemo, approvedAt);
    }

    @Override
    @Transactional
    public int claimPendingAsRejected(String requestId, String adminMemberId, String adminMemo, Instant rejectedAt) {
        return redemptionRequestEntityRepository.claimPendingAsRejected(requestId, adminMemberId, adminMemo, rejectedAt);
    }

    @Override
    @Transactional
    public int claimPendingAsCancelled(String requestId, String memberId, Instant cancelledAt) {
        return redemptionRequestEntityRepository.claimPendingAsCancelled(requestId, memberId, cancelledAt);
    }
}
