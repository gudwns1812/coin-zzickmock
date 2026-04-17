package coin.coinzzickmock.feature.member.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterMemberService {
    private static final double INITIAL_WALLET_BALANCE = 100_000d;
    private static final double INITIAL_AVAILABLE_MARGIN = 100_000d;

    private final MemberCredentialRepository memberCredentialRepository;
    private final AccountRepository accountRepository;
    private final MemberPasswordHasher memberPasswordHasher;

    @Transactional
    public void register(
            String memberId,
            String rawPassword,
            String memberName,
            String memberEmail,
            String phoneNumber,
            String zipCode,
            String address,
            String addressDetail
    ) {
        String normalizedMemberId = normalizeRequired(memberId, "아이디");
        String normalizedPassword = normalizePassword(rawPassword);
        String normalizedMemberName = normalizeRequired(memberName, "이름");
        String normalizedMemberEmail = normalizeRequired(memberEmail, "이메일");
        String normalizedPhoneNumber = normalizeRequired(phoneNumber, "휴대폰 번호");
        String normalizedZipCode = normalizeRequired(zipCode, "우편번호");
        String normalizedAddress = normalizeRequired(address, "주소");
        String normalizedAddressDetail = addressDetail == null ? "" : addressDetail.trim();

        if (memberCredentialRepository.existsByMemberId(normalizedMemberId)) {
            throw new CoreException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        accountRepository.save(new TradingAccount(
                normalizedMemberId,
                normalizedMemberEmail,
                normalizedMemberName,
                INITIAL_WALLET_BALANCE,
                INITIAL_AVAILABLE_MARGIN
        ));

        memberCredentialRepository.save(new MemberCredential(
                normalizedMemberId,
                memberPasswordHasher.hash(normalizedPassword),
                normalizedMemberName,
                normalizedMemberEmail,
                normalizedPhoneNumber,
                normalizedZipCode,
                normalizedAddress,
                normalizedAddressDetail,
                0
        ));
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }

    private String normalizePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "비밀번호는 필수입니다.");
        }
        if (rawPassword.length() < 8 || rawPassword.length() > 20) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "비밀번호는 8자 이상 20자 이하여야 합니다.");
        }
        return rawPassword;
    }
}
