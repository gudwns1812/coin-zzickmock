package coin.coinzzickmock.architecture;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = CoinZzickmockApplication.class,
        properties = "spring.task.scheduling.enabled=false"
)
@ActiveProfiles("test")
class BackendBeanWiringTest {
    @Autowired
    private RewardPointPolicy rewardPointPolicy;

    @Autowired
    private OrderPreviewPolicy orderPreviewPolicy;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private MemberPasswordHasher memberPasswordHasher;

    @Test
    void exposesPoliciesAndSecurityCollaboratorsAsBeans() {
        assertNotNull(rewardPointPolicy);
        assertNotNull(orderPreviewPolicy);
        assertNotNull(passwordEncoder);

        String hashed = memberPasswordHasher.hash("demo-password");
        assertTrue(memberPasswordHasher.matches("demo-password", hashed));
    }
}
