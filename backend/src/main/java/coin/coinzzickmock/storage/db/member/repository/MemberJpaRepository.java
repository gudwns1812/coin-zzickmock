package coin.coinzzickmock.storage.db.member.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import coin.coinzzickmock.storage.db.member.entity.MemberEntity;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, String> {

    boolean existsByAccount(String account);

    Optional<MemberEntity> findByAccount(String account);
}
