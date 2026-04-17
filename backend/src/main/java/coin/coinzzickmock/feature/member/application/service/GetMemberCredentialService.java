package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMemberCredentialService {
    private final MemberCredentialRepository memberCredentialRepository;

    @Transactional(readOnly = true)
    public MemberCredential get(String memberId) {
        return memberCredentialRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
