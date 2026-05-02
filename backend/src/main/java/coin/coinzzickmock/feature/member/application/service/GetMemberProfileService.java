package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMemberProfileService {
    private final MemberCredentialRepository memberCredentialRepository;

    @Transactional(readOnly = true)
    public MemberProfileResult get(Long memberId) {
        return memberCredentialRepository.findActiveByMemberId(memberId)
                .map(MemberProfileResult::from)
                .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
