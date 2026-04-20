package coin.coinzzickmock.feature.member.infrastructure.config;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class MemberDemoSeedConfiguration {
    private static final String DEMO_MEMBER_ID = "test";
    private static final String DEMO_PASSWORD = "test@1234";
    private static final String DEMO_MEMBER_NAME = "demo-trader";
    private static final String DEMO_MEMBER_EMAIL = "test@coinzzickmock.dev";
    private static final String DEMO_PHONE_NUMBER = "010-1234-5678";
    private static final String DEMO_ZIP_CODE = "06018";
    private static final String DEMO_ADDRESS = "서울 강남구 테헤란로 123";
    private static final String DEMO_ADDRESS_DETAIL = "101호";
    @Bean
    ApplicationRunner demoMemberInitializer(
            AccountRepository accountRepository,
            MemberCredentialRepository memberCredentialRepository,
            MemberPasswordHasher memberPasswordHasher
    ) {
        return args -> {
            if (accountRepository.findByMemberId(DEMO_MEMBER_ID).isEmpty()) {
                accountRepository.save(TradingAccount.openDefault(
                        DEMO_MEMBER_ID,
                        DEMO_MEMBER_EMAIL,
                        DEMO_MEMBER_NAME
                ));
            }

            if (memberCredentialRepository.findByMemberId(DEMO_MEMBER_ID).isEmpty()) {
                memberCredentialRepository.save(MemberCredential.register(
                        DEMO_MEMBER_ID,
                        memberPasswordHasher.hash(DEMO_PASSWORD),
                        DEMO_MEMBER_NAME,
                        DEMO_MEMBER_EMAIL,
                        DEMO_PHONE_NUMBER,
                        DEMO_ZIP_CODE,
                        DEMO_ADDRESS,
                        DEMO_ADDRESS_DETAIL,
                        0
                ));
            }
        };
    }
}
