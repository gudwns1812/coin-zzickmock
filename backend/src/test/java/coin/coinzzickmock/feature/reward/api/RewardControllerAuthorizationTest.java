package coin.coinzzickmock.feature.reward.api;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.domain.MemberRole;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewardControllerAuthorizationTest {
    @Test
    void adminApisRejectNonAdminActorBeforeCallingServices() {
        RewardController controller = new RewardController(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                providers(new Actor(1L, "demo", "demo@coinzzickmock.dev", "Demo", MemberRole.USER))
        );

        CoreException thrown = assertThrows(
                CoreException.class,
                () -> controller.adminRedemptions("PENDING")
        );

        assertEquals(ErrorCode.FORBIDDEN, thrown.errorCode());
    }

    @Test
    void adminShopItemApisRejectNonAdminActorBeforeCallingServices() {
        RewardController controller = new RewardController(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                providers(new Actor(1L, "demo", "demo@coinzzickmock.dev", "Demo", MemberRole.USER))
        );

        CoreException thrown = assertThrows(
                CoreException.class,
                controller::adminShopItems
        );

        assertEquals(ErrorCode.FORBIDDEN, thrown.errorCode());
    }

    @Test
    void adminShopItemWritesRejectMissingBodyBeforeCallingServices() {
        RewardController controller = new RewardController(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                providers(new Actor(99L, "admin", "admin@coinzzickmock.dev", "Admin", MemberRole.ADMIN))
        );

        CoreException thrown = assertThrows(
                CoreException.class,
                () -> controller.createAdminShopItem(null)
        );

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
    }

    private Providers providers(Actor actor) {
        return new Providers() {
            @Override
            public AuthProvider auth() {
                return new AuthProvider() {
                    @Override
                    public Actor currentActor() {
                        return actor;
                    }

                    @Override
                    public boolean isAuthenticated() {
                        return true;
                    }
                };
            }

            @Override
            public ConnectorProvider connector() {
                return null;
            }

            @Override
            public TelemetryProvider telemetry() {
                return null;
            }

            @Override
            public FeatureFlagProvider featureFlags() {
                return null;
            }
        };
    }
}
