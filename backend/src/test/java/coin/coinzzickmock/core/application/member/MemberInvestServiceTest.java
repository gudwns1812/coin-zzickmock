package coin.coinzzickmock.core.application.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import coin.coinzzickmock.storage.db.Address;
import coin.coinzzickmock.storage.db.member.entity.MemberEntity;
import coin.coinzzickmock.storage.db.member.repository.MemberJpaRepository;
import coin.coinzzickmock.support.error.AuthErrorType;
import coin.coinzzickmock.support.error.CoreException;

@ExtendWith(MockitoExtension.class)
class MemberInvestServiceTest {

    @Mock
    private MemberJpaRepository memberJpaRepository;

    @InjectMocks
    private MemberInvestService memberInvestService;

    @Test
    void rejectsDifferentMemberOwnership() {
        assertThatThrownBy(() -> memberInvestService.updateInvest("member-1", "member-2", 4))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(AuthErrorType.MEMBER_ACCESS_DENIED);
    }

    @Test
    void updatesInvestScoreForOwnedMember() {
        MemberEntity memberEntity = MemberEntity.builder()
                .memberId("member-1")
                .account("tester")
                .passwordHash("encoded-password")
                .name("테스터")
                .email("tester@example.com")
                .phoneNumber("010-1234-5678")
                .address(Address.builder()
                        .zipCode("12345")
                        .address("서울시 강남구")
                        .addressDetail("101동")
                        .build())
                .invest(0)
                .refreshTokenVersion(0L)
                .build();

        when(memberJpaRepository.findById("member-1")).thenReturn(Optional.of(memberEntity));

        var result = memberInvestService.updateInvest("member-1", "member-1", 4);

        assertThat(result.getInvest()).isEqualTo(4);
        assertThat(memberEntity.getInvest()).isEqualTo(4);
    }
}
