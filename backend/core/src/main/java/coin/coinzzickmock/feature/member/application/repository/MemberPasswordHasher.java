package coin.coinzzickmock.feature.member.application.repository;

public interface MemberPasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
