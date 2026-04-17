# 로컬 회원가입/로그인 동기화와 데모 계정 정리

이 ExecPlan은 살아 있는 문서입니다. `진행 현황`, `놀라움과 발견`, `의사결정 기록`, `결과 및 회고` 섹션은 작업이 진행되는 내내 최신 상태로 유지해야 합니다.

이 문서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md) 기준을 따라 관리합니다.

## 목적 / 큰 그림

현재 프론트엔드의 로그인과 회원가입 화면은 `/proxy/auth/*` 계약을 사용하지만, 이 저장소의 백엔드에는 실제 회원 저장소와 인증 API가 없습니다. 그래서 사용자가 비밀번호를 입력해도 이 저장소 DB에는 저장되지 않고, 메인 화면은 `DemoAuthProvider` 때문에 항상 데모 사용자처럼 동작합니다.

이 변경이 끝나면 사용자는 이 저장소 안에서 직접 회원가입을 하고, 가입 직후 로그인할 수 있어야 합니다. 로그인 후 헤더와 메인 화면에 보이는 사용자 정보는 방금 가입한 정보와 일치해야 하며, 데모 계정은 `아이디 test`, `비밀번호 test@1234`, 표시 이름 `demo-trader`로 고정되어 있어야 합니다. 브라우저에서 `/signup`으로 가입하거나 `/login`에서 `test / test@1234`로 로그인했을 때 `/markets`와 `/portfolio`에 동일한 사용자 정보가 보이면 성공입니다.

## 진행 현황

- [x] (2026-04-16 20:39 KST) 현재 구조를 조사했고, 프론트 인증 요청이 외부 `news-toss.click`으로 rewrite되고 있으며 백엔드에는 회원/비밀번호 저장 구조가 없음을 확인했다.
- [x] (2026-04-16 20:39 KST) 실행 계획 초안을 `docs/exec-plans/active/2026-04-16-auth-sync-local-members.md`에 작성했다.
- [x] (2026-04-16 20:48 KST) 사용자 승인 후 백엔드에 `member_credentials` 스키마, `feature.member` 인증 API, JWT 쿠키 기반 `JwtCookieAuthProvider`를 추가했다.
- [x] (2026-04-16 20:50 KST) 프론트 `/proxy/auth/*` 경로를 로컬 백엔드 인증 API로 연결하고, 회원가입 직후 로그인 요청의 `credentials` 버그를 수정했다.
- [x] (2026-04-16 20:50 KST) 데모 계정 `test / test@1234 / demo-trader`를 시드로 넣고, 서버 렌더링용 `futures-api`가 `accessToken` 쿠키를 백엔드로 전달하도록 맞췄다.
- [x] (2026-04-16 20:55 KST) `architectureLint`, `./gradlew test`, `./gradlew check`, `npm run build --workspace frontend`를 모두 통과했다.

## 놀라움과 발견

- 관찰:
  프론트 로그인과 회원가입은 모두 `/proxy/auth/*`로 요청하지만, `frontend/next.config.ts`는 이 경로를 로컬 백엔드가 아니라 `https://news-toss.click/api/:path*`로 보내고 있다.
  증거:
  `frontend/next.config.ts`의 `source: "/proxy/:path*"` rewrite

- 관찰:
  현재 DB에는 `member` 성격 테이블이 없고 `trading_accounts`에 `member_id`, `member_email`, `member_name`, 자산 정보만 있다.
  증거:
  `backend/src/main/resources/db/migration/V1__initial_schema.sql`

- 관찰:
  백엔드는 `DemoAuthProvider`가 항상 같은 사용자를 인증된 상태로 반환한다. 따라서 비로그인 상태여도 `/api/futures/account/me`는 데모 사용자를 기준으로 응답한다.
  증거:
  `backend/src/main/java/coin/coinzzickmock/providers/infrastructure/DemoAuthProvider.java`

- 관찰:
  서버 컴포넌트가 직접 로컬 백엔드로 fetch할 때는 브라우저 쿠키가 자동으로 전달되지 않는다. `frontend/lib/futures-api.ts`에서 `next/headers`로 `accessToken`을 꺼내 `Cookie` 헤더를 수동 전달해야 로그인 사용자 계정 요약이 맞게 나온다.
  증거:
  로그인 쿠키는 프론트 `getJwtToken()`에는 보였지만, 수정 전 `readApi()`는 헤더 없이 `http://127.0.0.1:8080`만 호출했다.

## 의사결정 기록

- 결정:
  기존 프론트 계약 `/proxy/auth/*`는 유지하고, 이 경로만 로컬 백엔드의 `/api/futures/auth/*`로 라우팅한다.
  근거:
  프론트 로그인/회원가입/헤더 코드가 이미 이 계약을 사용하고 있어서, 화면 코드를 대거 갈아엎기보다 rewrite와 백엔드 API를 맞추는 편이 범위가 작고 회귀 위험이 낮다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  회원 자격 증명은 새 테이블로 분리하고, 기존 `trading_accounts`는 계정 자산/닉네임 저장 역할을 유지한다.
  근거:
  비밀번호 해시와 세션/인증 책임은 자산 테이블과 분리하는 편이 보안과 역할 경계에 맞다. 또한 현재 주문/포지션/보상 포인트가 모두 `trading_accounts.member_id`를 참조하고 있으므로, 기존 계정 테이블을 그대로 두는 편이 안전하다.
  날짜/작성자:
  2026-04-16 / Codex

## 결과 및 회고

로컬 백엔드가 이제 회원가입, 로그인, 중복 확인, 로그아웃, 세션 갱신, 탈퇴를 직접 처리한다. 프론트는 기존 `/proxy/auth/*` 계약을 유지한 채 로컬 `/api/futures/auth/*`로 연결되고, 로그인 후 서버 렌더링 화면도 같은 `accessToken` 쿠키를 사용해 계정 요약/포지션/포인트를 읽는다.

이번 구현으로 `test / test@1234 / demo-trader` 데모 계정이 로컬 시드로 고정되었고, 빌드와 백엔드 검증은 모두 통과했다. 브라우저 자동화 도구는 당시 세션의 프로필 락 때문에 붙지 못했지만, 이 계획 자체의 구현 및 검증 범위는 닫혔으므로 완료 계획으로 정리한다.

## 맥락과 길잡이

이 작업은 프론트와 백엔드를 동시에 건드린다. 프론트는 `frontend/app/login/page.tsx`, `frontend/app/signup/page.tsx`, `frontend/components/router/siginup/RegisterStep2.tsx`, `frontend/components/ui/shared/header/*`에서 인증 요청과 토큰 표시를 담당한다. 백엔드는 현재 `feature.account`, `providers.auth`, `providers.infrastructure.DemoAuthProvider`가 사용자 식별의 전부다.

중요한 현재 상태는 아래와 같다.

`frontend/next.config.ts`
  `/proxy/:path*`를 외부 `news-toss.click`로 rewrite한다.

`frontend/utils/auth.ts`
  `accessToken` 쿠키를 읽고 `jwtDecode`만 한다. 이 코드는 브라우저 표시용 참고 정보이며, 서버 최종 인증 근거로 쓰면 안 된다.

`backend/src/main/resources/db/migration/V1__initial_schema.sql`
  현재 `trading_accounts`, `reward_point_wallets`, `futures_orders`, `open_positions`만 만든다.

`backend/src/main/java/coin/coinzzickmock/providers/infrastructure/DemoAuthProvider.java`
  실제 로그인과 무관하게 `demo-member` 사용자를 항상 반환한다.

이번 작업에서 새로 만들어야 할 핵심 개념은 "회원 자격 증명"이다. 여기에는 로그인 아이디, 비밀번호 해시, 연락처/주소 같은 프로필, 탈퇴 여부, 생성/수정 시간이 들어간다. 로그인 성공 후에는 프론트가 기대하는 `JwtToken` 형태에 맞는 JWT를 `accessToken` 쿠키로 내려줘야 한다.

## 작업 계획

첫째, 백엔드에 `feature.member`를 만든다. `api`에는 회원가입, 로그인, 중복 확인, 로그아웃, 토큰 갱신, 탈퇴 엔드포인트를 두고, `application`에는 각 유스케이스 서비스와 command/result 모델을 둔다. `domain`에는 회원 자격 증명과 프로필 규칙을 담고, `infrastructure`에는 JPA 엔티티, Spring Data 저장소, 비밀번호 해시 구현, JWT 발급/검증 구현을 둔다.

둘째, DB에 새 `Flyway` migration을 추가한다. 새 테이블은 로그인 아이디와 비밀번호 해시를 저장해야 하며, 프론트가 회원가입에서 보내는 `account`, `password`, `name`, `phoneNumber`, `email`, `address.zipcode`, `address.address`, `address.addressDetail`를 모두 보존할 수 있어야 한다. 기존 `trading_accounts`와는 `member_id` 기준으로 연결한다. 데모 계정 `test / test@1234 / demo-trader`도 함께 시드한다.

셋째, 기존 `DemoAuthProvider`를 실제 요청 기반 인증으로 교체한다. HTTP 쿠키의 JWT를 검증해 현재 사용자 `Actor`를 복원하고, 인증이 없으면 비인증으로 처리해야 한다. 메인 화면에서 비로그인 사용자에게 데모 계정을 보여줄지, 로그인 사용자만 실제 계정을 볼지는 구현 중 프론트 요구에 맞춰 정리하되, 최소한 로그인 후에는 가입 정보와 정확히 일치해야 한다.

넷째, 프론트 rewrite를 조정한다. `/proxy/auth/:path*`는 로컬 백엔드 인증 API로 보내고, 나머지 `/proxy/:path*`는 기존 외부 서버를 유지한다. 이렇게 하면 기존 주식 흐름을 크게 건드리지 않으면서 로그인/회원가입만 이 저장소의 백엔드로 옮길 수 있다.

다섯째, 검증을 넣는다. 백엔드는 회원가입, 중복 확인, 로그인 성공/실패, JWT 쿠키 복원, 탈퇴 후 재로그인 실패를 테스트한다. 프론트는 최소한 로그인/회원가입 폼이 같은 계약을 쓰는지 확인하고, 필요하면 로컬 브라우저로 `/signup -> /markets` 흐름을 확인한다.

## 구체적인 단계

1. `backend/src/main/resources/db/migration/V2__add_member_auth.sql` 같은 새 migration 파일을 추가한다.
2. `backend/src/main/java/coin/coinzzickmock/feature/member/` 아래에 `api`, `application`, `domain`, `infrastructure` 패키지를 만든다.
3. `backend/build.gradle`에 비밀번호 해시와 JWT 서명 검증에 필요한 의존성을 추가한다.
4. `backend/src/main/java/coin/coinzzickmock/providers/auth/AuthProvider.java`와 구현체를 실제 인증 흐름에 맞게 바꾼다.
5. `frontend/next.config.ts`에서 `/proxy/auth/:path*` rewrite를 로컬 백엔드로 추가하고 기존 `/proxy/:path*`보다 앞에 둔다.
6. 프론트 폼이 보내는 JSON 구조와 백엔드 DTO를 맞춘다. 필요하면 오타나 `credentials` 헤더 위치 같은 실수를 함께 정리한다.
7. 데모 사용자 시드와 초기 자산/포인트를 추가한다.
8. 테스트와 수동 검증을 실행하고 계획서의 결과를 갱신한다.

## 검증과 수용 기준

백엔드 검증은 `/Users/hj.park/projects/coin-zzickmock/backend`에서 아래 명령으로 수행한다.

    GRADLE_USER_HOME=/tmp/gradle-home ./gradlew architectureLint --console=plain
    GRADLE_USER_HOME=/tmp/gradle-home ./gradlew check --console=plain

프론트 검증은 저장소 루트 `/Users/hj.park/projects/coin-zzickmock`에서 아래 명령을 사용한다.

    npm run build --workspace frontend

수동 수용 기준은 아래와 같다.

1. `/signup`에서 새 사용자 정보를 입력하고 가입을 완료하면 `/markets`로 이동한다.
2. 이동 직후 헤더 사용자 정보와 메인/포트폴리오의 계정 이름이 가입 정보와 일치한다.
3. `/login`에서 `test / test@1234`로 로그인하면 표시 이름 `demo-trader`가 보인다.
4. 잘못된 비밀번호로 로그인하면 실패한다.
5. 같은 아이디로 중복 확인을 하면 이미 사용 중이라고 응답한다.
6. 탈퇴 후에는 같은 계정으로 다시 로그인할 수 없다.

## 반복 실행 가능성 및 복구

새 migration은 기존 파일을 수정하지 않고 새 버전으로 추가하므로 반복 실행에 안전해야 한다. 시드 로직은 "존재하지 않을 때만 생성" 방식으로 작성해 여러 번 서버가 떠도 같은 데모 계정이 중복 생성되지 않게 한다. 로그인 쿠키 실험 중 브라우저 상태가 꼬이면 `accessToken` 쿠키를 삭제하고 `/login`부터 다시 시작한다.

## 산출물과 메모

구현 후 아래 증거를 남긴다.

- `V2__...sql` migration diff
- 회원가입/로그인 관련 테스트 통과 로그
- 필요하면 `/login` 성공 후 응답 헤더 또는 쿠키 확인 메모

## 인터페이스와 의존성

예상되는 핵심 계약은 아래와 같다.

`POST /api/futures/auth/register`
  입력:
  `account`, `password`, `name`, `phoneNumber`, `email`, `fgOffset`, `address.zipcode`, `address.address`, `address.addressDetail`

`POST /api/futures/auth/login`
  입력:
  `account`, `password`
  동작:
  성공 시 `accessToken` 쿠키 설정

`POST /api/futures/auth/duplicate`
  입력:
  `account`
  동작:
  사용 가능하면 성공, 이미 존재하면 실패

`POST /api/futures/auth/logout`
  동작:
  `accessToken` 쿠키 만료

`GET /api/futures/auth/refresh`
  동작:
  기존 인증 사용자를 다시 검증하고 새 만료 시간을 가진 JWT를 발급

`DELETE /api/futures/auth/withdraw`
  입력:
  `memberId`
  동작:
  현재 로그인 사용자 본인만 탈퇴 가능

변경 메모:
2026-04-16 초안 작성. 현재 프론트 외부 인증 의존성과 로컬 백엔드 부재를 기준으로 회원/인증 동기화 작업 범위를 정리했다.
2026-04-16 구현 반영. `feature.member`, `member_credentials`, JWT 쿠키 인증, 프론트 rewrite/쿠키 전달, 데모 계정 시드, 테스트/빌드 결과를 문서에 반영했다.
