package coin.coinzzickmock.feature.member.infrastructure.security;

import coin.coinzzickmock.feature.member.application.repository.MemberPasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BcryptMemberPasswordHasher implements MemberPasswordHasher {
    private final BCryptPasswordEncoder passwordEncoder;


    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
