package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.feature.member.application.dto.MemberProfileResult;
import coin.coinzzickmock.feature.member.application.implement.MemberRegistrationProvisioner;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberIdentityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterMemberService {
    private final MemberPasswordHasher memberPasswordHasher;
    private final MemberRegistrationProvisioner memberRegistrationProvisioner;

    public MemberProfileResult register(
            String account,
            String rawPassword,
            String memberName,
            String nickname,
            String memberEmail,
            String phoneNumber
    ) {
        String normalizedAccount = MemberIdentityRules.normalizeAccount(account);
        String normalizedPassword = MemberIdentityRules.validateRawPassword(rawPassword);

        MemberCredential memberCredential = MemberCredential.register(
                normalizedAccount,
                memberPasswordHasher.hash(normalizedPassword),
                memberName,
                nickname,
                memberEmail,
                phoneNumber,
                0
        );
        MemberCredential savedMemberCredential = memberRegistrationProvisioner.provision(memberCredential);
        return MemberProfileResult.from(savedMemberCredential);
    }
}
