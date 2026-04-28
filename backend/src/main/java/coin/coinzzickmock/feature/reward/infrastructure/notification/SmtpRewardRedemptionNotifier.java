package coin.coinzzickmock.feature.reward.infrastructure.notification;

import coin.coinzzickmock.feature.reward.application.event.RewardRedemptionCreatedEvent;
import coin.coinzzickmock.feature.reward.application.notification.RewardRedemptionNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmtpRewardRedemptionNotifier implements RewardRedemptionNotifier {
    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final String adminEmail;
    private final String mailHost;

    public SmtpRewardRedemptionNotifier(
            ObjectProvider<JavaMailSender> javaMailSenderProvider,
            @Value("${coin.reward.notification.admin-email:gudwns1812@naver.com}") String adminEmail,
            @Value("${spring.mail.host:}") String mailHost
    ) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.adminEmail = adminEmail;
        this.mailHost = mailHost;
    }

    @Override
    public void notifyCreated(RewardRedemptionCreatedEvent event) {
        if (mailHost == null || mailHost.isBlank()) {
            log.warn("Reward redemption email skipped because spring.mail.host is not configured. requestId={} recipient={}",
                    event.requestId(), adminEmail);
            return;
        }
        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            log.warn("Reward redemption email skipped because JavaMailSender is unavailable. requestId={} recipient={}",
                    event.requestId(), adminEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("[coin-zzickmock] 커피 교환권 요청");
        message.setText("""
                새 포인트 상점 교환권 요청이 접수되었습니다.

                요청 ID: %s
                회원 ID: %s
                상품: %s (%s)
                포인트: %d
                휴대폰 번호: %s
                """.formatted(
                event.requestId(),
                event.memberId(),
                event.itemName(),
                event.itemCode(),
                event.pointAmount(),
                event.submittedPhoneNumber()
        ));
        javaMailSender.send(message);
    }
}
