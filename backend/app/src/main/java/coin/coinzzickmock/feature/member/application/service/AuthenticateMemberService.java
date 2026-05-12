package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberIdentityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticateMemberService {
    private final MemberCredentialRepository memberCredentialRepository;
    private final MemberPasswordHasher memberPasswordHasher;

    @Transactional(readOnly = true)
    public MemberProfileResult authenticate(String account, String rawPassword) {
        String normalizedAccount = MemberIdentityRules.normalizeAccount(account);
        String requiredPassword = MemberIdentityRules.requirePasswordInput(rawPassword);

        MemberCredential memberCredential = memberCredentialRepository.findActiveByAccount(normalizedAccount)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_CREDENTIALS));

        if (!memberPasswordHasher.matches(requiredPassword, memberCredential.passwordHash())) {
            throw new CoreException(ErrorCode.INVALID_CREDENTIALS);
        }

        return MemberProfileResult.from(memberCredential);
    }
}
