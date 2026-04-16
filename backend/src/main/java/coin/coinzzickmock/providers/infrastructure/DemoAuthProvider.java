package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import org.springframework.stereotype.Component;

@Component
public class DemoAuthProvider implements AuthProvider {
    private static final Actor DEMO_ACTOR = new Actor(
            "demo-member",
            "demo@coinzzickmock.dev",
            "Demo Trader"
    );

    @Override
    public Actor currentActor() {
        return DEMO_ACTOR;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }
}
