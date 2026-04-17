package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.feature.member.application.repository.MemberDataCleaner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawMemberService {
    private final MemberDataCleaner memberDataCleaner;

    public WithdrawMemberService(MemberDataCleaner memberDataCleaner) {
        this.memberDataCleaner = memberDataCleaner;
    }

    @Transactional
    public void withdraw(String memberId) {
        memberDataCleaner.deleteAllByMemberId(memberId);
    }
}
