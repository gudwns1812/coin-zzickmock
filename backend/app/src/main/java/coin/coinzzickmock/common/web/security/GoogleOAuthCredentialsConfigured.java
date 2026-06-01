package coin.coinzzickmock.common.web.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

final class GoogleOAuthCredentialsConfigured implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return GoogleOAuthCredentials.areConfigured(context.getEnvironment());
    }
}
