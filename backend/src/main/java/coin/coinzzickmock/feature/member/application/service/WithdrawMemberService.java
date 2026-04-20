package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberDataCleaner;
import coin.coinzzickmock.feature.member.domain.MemberIdentityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawMemberService {
    private final MemberDataCleaner memberDataCleaner;

    @Transactional
    public void withdraw(String actorMemberId, String requestedMemberId) {
        String normalizedActorMemberId = MemberIdentityRules.normalizeMemberId(actorMemberId);
        String normalizedRequestedMemberId = MemberIdentityRules.normalizeMemberId(requestedMemberId);
        if (!normalizedActorMemberId.equals(normalizedRequestedMemberId)) {
            throw new CoreException(ErrorCode.FORBIDDEN, "본인 계정만 탈퇴할 수 있습니다.");
        }

        memberDataCleaner.deleteAllByMemberId(normalizedActorMemberId);
    }
}
