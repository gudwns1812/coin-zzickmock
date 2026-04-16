package coin.coinzzickmock.feature.position.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenPositionSpringDataRepository extends JpaRepository<OpenPositionEntity, Long> {
}
