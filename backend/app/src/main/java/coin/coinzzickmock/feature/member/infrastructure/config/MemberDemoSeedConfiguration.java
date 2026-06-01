package coin.coinzzickmock.feature.member.infrastructure.config;

import coin.coinzzickmock.feature.account.application.service.TradingAccountProvisioningService;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberRole;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class MemberDemoSeedConfiguration {
    private static final String DEMO_ACCOUNT = "test";
    private static final String DEMO_PASSWORD = "test@1234";
    private static final String DEMO_MEMBER_NAME = "demo-trader";
    private static final String DEMO_NICKNAME = "demo-trader";
    private static final String DEMO_MEMBER_EMAIL = "test@coinzzickmock.dev";
    private static final String DEMO_PHONE_NUMBER = "010-1234-5678";

    @Bean
    ApplicationRunner demoMemberInitializer(
            TradingAccountProvisioningService tradingAccountProvisioningService,
            MemberCredentialRepository memberCredentialRepository,
            MemberPasswordHasher memberPasswordHasher
    ) {
        return args -> {
            MemberCredential credential = memberCredentialRepository.findByAccountIncludingWithdrawn(DEMO_ACCOUNT)
                    .map(existing -> existing.role().equals(MemberRole.ADMIN)
                            ? existing
                            : memberCredentialRepository.save(existing.asAdmin()))
                    .orElseGet(() -> memberCredentialRepository.create(MemberCredential.register(
                                    DEMO_ACCOUNT,
                                    memberPasswordHasher.hash(DEMO_PASSWORD),
                                    DEMO_MEMBER_NAME,
                                    DEMO_NICKNAME,
                                    DEMO_MEMBER_EMAIL,
                                    DEMO_PHONE_NUMBER,
                                    0
                            ).asAdmin()));

            tradingAccountProvisioningService.openForSeedIfMissing(
                    credential.memberId(),
                    credential.memberEmail(),
                    credential.memberName(),
                    credential.nickname()
            );
        };
    }
}
