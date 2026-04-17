package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckMemberAvailabilityService {
    private final MemberCredentialRepository memberCredentialRepository;

    public CheckMemberAvailabilityService(MemberCredentialRepository memberCredentialRepository) {
        this.memberCredentialRepository = memberCredentialRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(String memberId) {
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "아이디는 필수입니다.");
        }
        return !memberCredentialRepository.existsByMemberId(memberId.trim());
    }
}
