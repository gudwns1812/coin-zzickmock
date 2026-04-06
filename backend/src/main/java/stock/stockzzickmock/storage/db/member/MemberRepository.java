package stock.stockzzickmock.storage.db.member;

import org.springframework.data.jpa.repository.JpaRepository;
import stock.stockzzickmock.core.domain.member.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {
    boolean existsByAccount(String account);

    Optional<Member> findByAccount(String account);
}
