package coin.coinzzickmock.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 운영과 같은 MySQL 8.4 컨테이너로 datasource를 바꾼다.
 *
 * <p>컨테이너는 JVM에 하나만 띄우고, 이 설정을 쓰는 스프링 컨텍스트가 모두 같이 쓴다.
 * 컨테이너를 빈으로 등록하면 컨텍스트마다 새로 뜨고 닫히므로 정적 필드로 둔다.
 * 종료는 Testcontainers(Ryuk)가 JVM이 끝날 때 처리한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestContainerConfiguration {
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("coin_zzickmock")
            .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--default-time-zone=+00:00",
                    // 락 대기가 기본값 50초를 기다리면 실패한 동시성 테스트가 그만큼 멈춘다.
                    "--innodb-lock-wait-timeout=5"
            )
            .withUrlParam("serverTimezone", "UTC")
            .withUrlParam("connectionTimeZone", "UTC")
            .withUrlParam("forceConnectionTimeZoneToSession", "true")
            .withUrlParam("characterEncoding", "UTF-8");

    @Bean
    DynamicPropertyRegistrar mySqlDataSourceProperties() {
        return registry -> {
            registry.add("spring.datasource.url", () -> started().getJdbcUrl());
            registry.add("spring.datasource.username", () -> started().getUsername());
            registry.add("spring.datasource.password", () -> started().getPassword());
            registry.add("spring.datasource.driver-class-name", () -> started().getDriverClassName());
        };
    }

    private static MySQLContainer<?> started() {
        MYSQL.start();
        return MYSQL;
    }
}
