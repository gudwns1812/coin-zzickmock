package coin.coinzzickmock.feature.position.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenPositionEntityRepository extends JpaRepository<OpenPositionEntity, Long> {
    void deleteAllByMemberId(String memberId);
}
