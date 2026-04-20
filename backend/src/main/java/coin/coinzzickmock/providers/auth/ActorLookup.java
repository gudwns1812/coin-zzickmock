package coin.coinzzickmock.providers.auth;

import java.util.Optional;

public interface ActorLookup {
    Optional<Actor> findByMemberId(String memberId);
}
