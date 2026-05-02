package coin.coinzzickmock.feature.leaderboard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.port.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
@Transactional
class LeaderboardProjectionPersistenceRepositoryTest {
    @Autowired
    private LeaderboardProjectionRepository leaderboardProjectionRepository;

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findAllExcludesWithdrawnMembers() {
        MemberCredential active = saveMemberWithAccount("leaderboard-active");
        MemberCredential withdrawn = saveMemberWithAccount("leaderboard-withdrawn");
        memberCredentialRepository.save(withdrawn.withdraw(Instant.parse("2026-05-02T00:00:00Z")));

        assertThat(leaderboardProjectionRepository.findAll())
                .extracting(entry -> entry.memberId())
                .contains(active.memberId())
                .doesNotContain(withdrawn.memberId());
    }

    @Test
    void findByMemberIdReturnsEmptyForWithdrawnMember() {
        MemberCredential withdrawn = saveMemberWithAccount("leaderboard-single-withdrawn");
        memberCredentialRepository.save(withdrawn.withdraw(Instant.parse("2026-05-02T00:00:00Z")));

        assertThat(leaderboardProjectionRepository.findByMemberId(withdrawn.memberId())).isEmpty();
    }

    private MemberCredential saveMemberWithAccount(String account) {
        MemberCredential member = memberCredentialRepository.save(MemberCredential.register(
                account,
                "hashed-password",
                "Member " + account,
                "Nick " + account.substring(0, Math.min(account.length(), 20)),
                account + "@coinzzickmock.dev",
                "010-0000-0000",
                "00000",
                "Seoul",
                "101",
                0
        ));
        accountRepository.save(new TradingAccount(
                member.memberId(),
                member.memberEmail(),
                member.memberName(),
                120_000,
                120_000
        ));
        return member;
    }
}
