package coin.coinzzickmock.providers.auth;

import java.util.Optional;

public interface ActorLookup {
    Optional<Actor> findByMemberId(Long memberId);

    Optional<Actor> findByAccount(String account);
}
