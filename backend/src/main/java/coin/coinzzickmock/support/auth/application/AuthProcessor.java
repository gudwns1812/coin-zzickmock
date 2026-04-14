package coin.coinzzickmock.support.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import coin.coinzzickmock.core.domain.member.Member;
import coin.coinzzickmock.storage.db.member.entity.MemberEntity;
import coin.coinzzickmock.storage.db.member.repository.MemberJpaRepository;
import coin.coinzzickmock.support.error.AuthErrorType;
import coin.coinzzickmock.support.error.CoreException;

@Component
@RequiredArgsConstructor
@Transactional
public class AuthProcessor {

    private final MemberJpaRepository memberJpaRepository;

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
