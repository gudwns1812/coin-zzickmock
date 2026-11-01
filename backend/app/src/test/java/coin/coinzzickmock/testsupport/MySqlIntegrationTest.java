package coin.coinzzickmock.testsupport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * H2가 아니라 실제 MySQL 8.4에서 돌려야 하는 통합 테스트에 붙인다.
 *
 * <p>H2(MODE=MySQL)는 InnoDB 행 락, 갭 락, 데드락 감지, REPEATABLE READ 스냅샷을 재현하지 못한다.
 * 동시성이나 락 동작을 검증하는 테스트는 이 애너테이션을 쓴다.
 * {@code @SpringBootTest}, {@code @ActiveProfiles("test")}는 다른 통합 테스트처럼 테스트 클래스에 직접 붙인다.
 * Docker가 떠 있어야 하며, 없으면 건너뛰지 않고 실패한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@IntegrationTest
@Import(MySqlTestContainerConfiguration.class)
public @interface MySqlIntegrationTest {
}
