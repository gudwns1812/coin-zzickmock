package coin.coinzzickmock.core.application.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import coin.coinzzickmock.core.domain.member.Member;
import coin.coinzzickmock.storage.db.member.entity.MemberEntity;
import coin.coinzzickmock.storage.db.member.repository.MemberJpaRepository;
import coin.coinzzickmock.support.error.AuthErrorType;
import coin.coinzzickmock.support.error.CoreException;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberInvestService {

    private final MemberJpaRepository memberJpaRepository;

    public Member updateInvest(String authenticatedMemberId, String requestMemberId, int investScore) {
        validateMemberOwnership(authenticatedMemberId, requestMemberId);

        MemberEntity member = memberJpaRepository.findById(authenticatedMemberId)
                .orElseThrow(() -> new CoreException(AuthErrorType.MEMBER_NOT_FOUND));
        member.updateInvest(investScore);
        return member.toDomain();
    }

    private void validateMemberOwnership(String authenticatedMemberId, String requestMemberId) {
        if (!authenticatedMemberId.equals(requestMemberId)) {
            throw new CoreException(AuthErrorType.MEMBER_ACCESS_DENIED);
        }
    }
}
