# Branch And PR Rules

이 문서는 저장소의 브랜치 생성과 PR naming 규칙을 고정한다. 외부 GitHub skill, 자동화 도구, 에이전트 기본값보다 이 저장소 규칙이 항상 우선한다.

## Branch Name Policy

브랜치는 반드시 아래 형식을 사용한다.

```text
<type>/<kebab-case-summary>
```

허용하는 `<type>`은 다음 목록으로 제한한다.

- `feat`: 새 사용자 기능이나 제품 동작 추가
- `fix`: 버그 수정 또는 회귀 수정
- `refactor`: 동작 변경 없는 구조 개선
- `docs`: 문서만 변경
- `test`: 테스트 추가 또는 테스트 구조 수정
- `chore`: 운영성 낮은 잡무성 변경
- `ci`: CI, workflow, hook, automation 변경
- `perf`: 성능 개선
- `style`: 포맷, 스타일, 비동작 변경
- `build`: 빌드/패키징/의존성 경계 변경
- `revert`: 이전 변경 되돌림

예시:

```text
feat/limit-order-entry
fix/login-token-refresh
refactor/market-cache-boundary
docs/branch-name-policy
ci/branch-name-check
```

## Hard Bans

- `codex/*`, `codex-*` 같은 자동화/에이전트 접두사는 금지한다.
- 외부 skill이 `codex/{description}` 같은 기본 브랜치명을 제안해도 그대로 쓰면 안 된다.
- `market-realtime-event-split`처럼 `<type>/` prefix가 없는 브랜치는 금지한다.
- 공백, 대문자, 한글 slug는 브랜치명에 쓰지 않는다. 설명은 PR 제목/본문에 쓴다.

## PR Title Policy

PR 제목도 작업 성격을 드러내는 conventional prefix를 사용한다.

```text
feat: 지정가 주문 입력 추가
fix: 로그인 토큰 갱신 오류 수정
refactor: 시장 캐시 경계 정리
docs: 브랜치 네이밍 규칙 고정
```

PR 제목에 `[codex]` 같은 자동화 접두사를 붙이지 않는다.

## Enforcement

로컬 또는 CI에서 같은 검사 스크립트를 사용한다.

```bash
npm run check:branch -- feat/example-branch
```

인자를 생략하면 현재 git 브랜치를 검사한다.

```bash
npm run check:branch
```

GitHub Actions CI는 PR과 보호 브랜치 push마다 `Branch Name Policy` job을 먼저 실행한다. 이 job이 실패하면 frontend/backend 검증 job도 진행하지 않는다.
