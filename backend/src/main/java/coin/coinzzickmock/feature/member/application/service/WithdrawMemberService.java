package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.feature.member.application.repository.MemberDataCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawMemberService {
    private final MemberDataCleaner memberDataCleaner;

    @Transactional
    public void withdraw(String memberId) {
        memberDataCleaner.deleteAllByMemberId(memberId);
    }
}
