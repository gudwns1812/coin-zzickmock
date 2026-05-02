package coin.coinzzickmock.providers.auth;

public record Actor(
        Long memberId,
        String account,
        String email,
        String nickname,
        ActorRole role
) {
    public Actor(Long memberId, String account, String email, String nickname) {
        this(memberId, account, email, nickname, ActorRole.USER);
    }

    public boolean admin() {
        return role == ActorRole.ADMIN;
    }
}
