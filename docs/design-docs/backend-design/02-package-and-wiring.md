# Backend Package And Wiring

## Purpose

이 문서는 백엔드의 패키지 형태와 Spring bean 조립 경계를 설명한다.
구조를 어디에 둘지, concrete class 우선 원칙을 어디까지 허용하는지, Spring annotation과 configuration을 어디에 둬야 하는지를 여기서 본다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
- domain 조립 판단이 필요하면 [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)

## Package Shape

현재 패키지 루트는 `coin.coinzzickmock`이므로, 목표 구조는 아래를 기본값으로 한다.

```text
backend/src/main/java/coin/coinzzickmock/
  CoinZzickmockApplication.java
  common/
    api/
    error/
    annotation/
  providers/
    Providers.java
    auth/
    connector/
    telemetry/
    featureflag/
    infrastructure/
      config/
  feature/
    market/
      web/
      application/
        command/
        query/
        realtime/
        result/
        service/
      domain/
        model/
        service/
        policy/
      job/
      infrastructure/
        config/
        persistence/
        connector/
        mapper/
    member/
      web/
      application/
        grant/
      domain/
      job/
      infrastructure/
```

강한 규칙:

- 루트 패키지 바로 아래에는 `*Application` 진입점만 둔다.
- 루트 패키지의 최상위 하위 패키지는 `common`, `providers`, `feature`만 사용한다.
- 기능 코드는 반드시 `feature/<feature-name>/` 아래에 둔다.
- `feature` 바깥에 새 업무용 패키지를 만들지 않는다.
- 최종 feature layer는 `web`, `job`, `application`, `domain`, `infrastructure`만 사용한다.
- HTTP delivery Java package는 `web`이다. Java package 이름과 HTTP URL path는 별개이며 `/api/futures/**` path는 유지한다.
- `support`, `core`, `extern`, `storage`처럼 기술/성격 기준의 광역 패키지는 새로 만들지 않는다.
- `application/usecase`, `application/port`는 기본 골격이 아니다. 실제로 필요한 경우에만 추가한다.

## Concrete Class First

이 저장소의 기본값은 concrete class다.
이 규칙의 목적은 인터페이스를 기계적으로 늘리는 습관을 막는 데 있다.

허용 기준:

- 구현이 하나뿐이고 런타임 경계도 하나라면 concrete class를 그대로 사용한다.
- 인터페이스는 실제 다중 구현, 외부 경계 계약, 명확한 런타임 책임 분리가 있을 때만 만든다.

금지 예:

- 구현체가 하나뿐인데 메서드 전달만 하는 `*Port`
- 컨트롤러 하나만 바라보는 형식적인 `*UseCase`
- 이미 `Providers`나 repository/gateway가 있는데 그 위에 다시 덧씌운 추상화

## Bean Wiring Boundary

이 저장소에서 "기본값은 concrete class"는 "아무 데서나 직접 생성해도 된다"는 뜻이 아니다.
인터페이스를 줄이는 것과 조립 책임을 클래스 내부에 숨기는 것은 다르다.

강한 규칙:

- Spring이 관리하는 협력 객체는 concrete class라도 생성자 주입으로 연결한다.
- 같은 객체를 여러 유스케이스나 인프라 어댑터가 재사용할 수 있다면, 생성 책임은 실제 소유 layer의 config 또는 provider configuration으로 모은다.
- MVC/interceptor/SSE delivery config는 `web`, scheduler/startup/background trigger wiring은 `job`, JPA/Redis/external API/SMTP/JWT/provider implementation wiring은 `infrastructure`가 소유한다.
- domain 조립의 상세 기준은 [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)를 따른다.
- `new`를 써도 되는 경우는 값 객체, 엔티티, 결과 DTO, 컬렉션처럼 한 유스케이스 안에서 즉시 소비되는 짧은 수명 객체다.
- `new`를 피해야 하는 경우는 정책 객체, 암호화기, 파서, 재사용 계산기처럼 장기 협력 객체를 Spring 관리 클래스 내부에서 붙드는 경우다.

예시:

- 허용: `new TradingAccount(...)`, `new RewardPointWallet(...)`
- 허용: `new ConcurrentHashMap<>()` 같은 내부 자료구조
- 금지: `@Service` 안에서 `private final RewardPointPolicy policy = new RewardPointPolicy();`
- 금지: `@Component` 안에서 `private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();`

## Spring Rule

Spring은 조립 도구이지 아키텍처 그 자체가 아니다.
annotation 위치와 configuration 소유권을 명확히 드러내야 한다.

규칙:

- `@RestController`는 `web`에만 둔다.
- `WebMvcConfigurer`, `HandlerInterceptor` 등록, CORS/path pattern, SSE executor처럼 feature의 HTTP/SSE delivery 경계를 설명하는 Spring MVC configuration은 해당 feature의 `web`에 둔다.
- `@Scheduled`, `ApplicationReadyEvent` startup trigger, retry/backfill/background trigger는 `job`에 둔다.
- `job` class는 application service/coordinator 호출만 수행하고 repository/entity/JPA/Redis/SMTP/external SDK를 직접 import하지 않는다.
- `@Configuration`은 owner가 드러나는 layer에 둔다. MVC/SSE config는 `web`, trigger runtime config는 `job`, outbound technology config는 `infrastructure` 또는 `providers/infrastructure`다.
- 앱 시작 시 필요한 seed나 초기화가 feature use case를 실행한다면 `job` trigger로 선언한다. 단순 provider/client lifecycle은 provider implementation runtime에 남을 수 있다.
- `@Entity`, `@Embeddable`은 infrastructure persistence 쪽에 둔다.
- `@Transactional`은 application 유스케이스 경계에서 사용한다.
- `@PostConstruct`, `@Scheduled` 같은 lifecycle hook은 trigger 역할에 집중하고, transactional write orchestration은 별도 application 협력 객체로 위임한다.
- bean에 다른 협력 객체가 얽히는 startup warm-up/backfill은 `@PostConstruct`로 시작하지 않고 `feature/.../job`의 명시적 `@EventListener(ApplicationReadyEvent.class)` 경로를 기본값으로 사용한다.
- `@PostConstruct` lifecycle path가 transactional write orchestration을 직접 수행하거나, 그 목적의 `@Transactional` 협력 객체를 호출하도록 만들지 않는다.
- startup cache warm-up은 provider read -> cache update -> event publish에만 집중한다.
- startup backfill은 DB에 이미 추적되는 symbol/timestamp cursor를 기준으로 빠진 persisted history만 채우고, current snapshot persistence를 같이 맡지 않는다.
- `domain`에는 Spring annotation을 두지 않는다.
- `@RestController`가 application service 대신 형식적인 `*UseCase` 인터페이스만 바라보도록 강제하지 않는다.
- 스프링이 관리하는 클래스에서 final 필드 생성자 주입만 필요할 때는 수동 생성자 대신 Lombok `@RequiredArgsConstructor`를 기본값으로 사용한다.
- 생성자 안에서 값 검증, 정규화, 파생 필드 계산 같은 추가 로직이 있을 때만 수동 생성자를 남긴다.

### Provider Runtime Exception

Provider-level technical runtime은 아래 조건을 모두 만족할 때만 `providers/infrastructure`에 남길 수 있다.

- provider-owned connection/client lifecycle, subscription, reconnect, heartbeat, low-level external stream mechanics를 관리한다.
- feature application service를 business trigger로 호출하지 않는다.
- feature use case 실행이 primary purpose인 scheduler/startup work가 아니다.
- feature `web`/`job` type을 import하지 않는다.

이 조건을 하나라도 만족하지 못하고 feature use case를 실행한다면 `feature/<name>/job` 후보로 분류한다.

## Related Documents

- [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
