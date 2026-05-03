package coin.coinzzickmock.feature.member.application.event;

public record MemberRegisteredEvent(
        Long memberId,
        String account,
        String memberName,
        String memberEmail
) {
}
