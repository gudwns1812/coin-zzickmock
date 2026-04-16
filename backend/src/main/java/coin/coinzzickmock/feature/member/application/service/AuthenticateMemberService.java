package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticateMemberService {
    private final MemberCredentialRepository memberCredentialRepository;
    private final MemberPasswordHasher memberPasswordHasher;

    public AuthenticateMemberService(
            MemberCredentialRepository memberCredentialRepository,
            MemberPasswordHasher memberPasswordHasher
    ) {
        this.memberCredentialRepository = memberCredentialRepository;
        this.memberPasswordHasher = memberPasswordHasher;
    }

    @Transactional(readOnly = true)
    public MemberCredential authenticate(String memberId, String rawPassword) {
        String normalizedMemberId = normalizeRequired(memberId, "아이디");
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "비밀번호는 필수입니다.");
        }

        MemberCredential memberCredential = memberCredentialRepository.findByMemberId(normalizedMemberId)
                .orElseThrow(() -> new CoreException(ErrorCode.INVALID_CREDENTIALS));

        if (!memberPasswordHasher.matches(rawPassword, memberCredential.passwordHash())) {
            throw new CoreException(ErrorCode.INVALID_CREDENTIALS);
        }

        return memberCredential;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }
}
