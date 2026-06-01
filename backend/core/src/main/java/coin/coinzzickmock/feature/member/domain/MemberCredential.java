package coin.coinzzickmock.feature.member.domain;

import java.time.Instant;
import java.util.Objects;

public record MemberCredential(
        Long memberId,
        String account,
        String passwordHash,
        String memberName,
        String nickname,
        String memberEmail,
        String phoneNumber,
        int investScore,
        MemberRole role,
        Instant withdrawnAt
) {
    public MemberCredential(
            Long memberId,
            String account,
            String passwordHash,
            String memberName,
            String nickname,
            String memberEmail,
            String phoneNumber,
            int investScore,
            MemberRole role
    ) {
        this(
                memberId,
                account,
                passwordHash,
                memberName,
                nickname,
                memberEmail,
                phoneNumber,
                investScore,
                role,
                null
        );
    }

    public static MemberCredential register(
            String account,
            String passwordHash,
            String memberName,
            String nickname,
            String memberEmail,
            String phoneNumber,
            int investScore
    ) {
        return new MemberCredential(
                null,
                MemberIdentityRules.normalizeAccount(account),
                MemberIdentityRules.validateRequired(passwordHash),
                MemberIdentityRules.normalizeRequired(memberName),
                MemberIdentityRules.normalizeNickname(nickname),
                MemberIdentityRules.normalizeRequired(memberEmail),
                MemberIdentityRules.normalizeRequired(phoneNumber),
                investScore,
                MemberRole.USER,
                null
        );
    }

    public static MemberCredential registerGoogleOnly(
            String memberName,
            String nickname,
            String memberEmail,
            String phoneNumber,
            int investScore
    ) {
        return new MemberCredential(
                null,
                null,
                null,
                MemberIdentityRules.normalizeRequired(memberName),
                MemberIdentityRules.normalizeNickname(nickname),
                MemberIdentityRules.normalizeRequired(memberEmail),
                MemberIdentityRules.normalizeRequired(phoneNumber),
                investScore,
                MemberRole.USER,
                null
        );
    }

    public MemberCredential withMemberId(Long memberId) {
        return new MemberCredential(
                memberId,
                account,
                passwordHash,
                memberName,
                nickname,
                memberEmail,
                phoneNumber,
                investScore,
                role,
                withdrawnAt
        );
    }

    public MemberCredential asAdmin() {
        return withRole(MemberRole.ADMIN);
    }

    public MemberCredential withRole(MemberRole role) {
        return new MemberCredential(
                memberId,
                account,
                passwordHash,
                memberName,
                nickname,
                memberEmail,
                phoneNumber,
                investScore,
                role == null ? MemberRole.USER : role,
                withdrawnAt
        );
    }

    public boolean withdrawn() {
        return withdrawnAt != null;
    }

    public MemberCredential withdraw(Instant withdrawnAt) {
        return new MemberCredential(
                memberId,
                account,
                passwordHash,
                memberName,
                nickname,
                memberEmail,
                phoneNumber,
                investScore,
                role,
                Objects.requireNonNull(withdrawnAt, "withdrawnAt")
        );
    }
}
