package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.event.MemberRegisteredEvent;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.member.domain.MemberIdentityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterMemberService {
    private final MemberCredentialRepository memberCredentialRepository;
    private final MemberPasswordHasher memberPasswordHasher;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public MemberProfileResult register(
            String account,
            String rawPassword,
            String memberName,
            String nickname,
            String memberEmail,
            String phoneNumber,
            String zipCode,
            String address,
            String addressDetail
    ) {
        String normalizedAccount = MemberIdentityRules.normalizeAccount(account);
        String normalizedPassword = MemberIdentityRules.validateRawPassword(rawPassword);

        if (memberCredentialRepository.existsByAccount(normalizedAccount)) {
            throw new CoreException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        MemberCredential memberCredential = MemberCredential.register(
                normalizedAccount,
                memberPasswordHasher.hash(normalizedPassword),
                memberName,
                nickname,
                memberEmail,
                phoneNumber,
                zipCode,
                address,
                addressDetail,
                0
        );
        MemberCredential savedMemberCredential = memberCredentialRepository.save(memberCredential);

        applicationEventPublisher.publishEvent(new MemberRegisteredEvent(
                savedMemberCredential.memberId(),
                savedMemberCredential.account(),
                savedMemberCredential.memberName(),
                savedMemberCredential.memberEmail()
        ));
        return MemberProfileResult.from(savedMemberCredential);
    }
}
