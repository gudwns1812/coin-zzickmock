package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberIdentityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckMemberAvailabilityService {
    private final MemberCredentialRepository memberCredentialRepository;

    @Transactional(readOnly = true)
    public boolean isAvailable(String account) {
        return !memberCredentialRepository.existsByAccount(MemberIdentityRules.normalizeAccount(account));
    }
}
