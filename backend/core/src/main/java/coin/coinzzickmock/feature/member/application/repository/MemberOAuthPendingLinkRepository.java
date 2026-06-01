package coin.coinzzickmock.feature.member.application.repository;

import coin.coinzzickmock.feature.member.domain.MemberOAuthPendingLink;
import java.util.Optional;

public interface MemberOAuthPendingLinkRepository {
    MemberOAuthPendingLink create(MemberOAuthPendingLink pendingLink);

    Optional<MemberOAuthPendingLink> findByTokenHash(String tokenHash);

    Optional<MemberOAuthPendingLink> findByTokenHashForUpdate(String tokenHash);

    MemberOAuthPendingLink save(MemberOAuthPendingLink pendingLink);
}
