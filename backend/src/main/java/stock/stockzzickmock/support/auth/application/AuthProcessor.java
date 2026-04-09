package stock.stockzzickmock.support.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stock.stockzzickmock.core.domain.member.Member;
import stock.stockzzickmock.storage.db.member.entity.MemberEntity;
import stock.stockzzickmock.storage.db.member.repository.MemberJpaRepository;
import stock.stockzzickmock.support.error.AuthErrorType;
import stock.stockzzickmock.support.error.CoreException;

@Component
@RequiredArgsConstructor
@Transactional
public class AuthProcessor {

    private final MemberJpaRepository memberJpaRepository;

    public Member updateInvest(String memberId, int investScore) {
        MemberEntity member = getMember(memberId);

        member.updateInvest(investScore);

        return member.toDomain();
    }

    public Member updateRefreshVersion(String memberId, Long version) {
        MemberEntity member = getMember(memberId);

        member.updateRefreshVersion(version);

        return member.toDomain();
    }

    @NonNull
    private MemberEntity getMember(String memberId) {
        return memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new CoreException(AuthErrorType.MEMBER_NOT_FOUND));
    }
}
