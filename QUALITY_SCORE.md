# QUALITY_SCORE.md

## Purpose

이 문서는 에이전트 루프의 검증 단계와 리뷰 단계를 위한 품질 게이트 기준서다.
목표는 두 가지다.

- `multi-angle-review` 방식의 독립 리뷰를 점수화해서 종료 조건으로 쓴다.
- 점수가 임계값 미만이면 같은 작업 루프를 다시 돌며 수정하게 만든다.

이 문서는 구현 규칙 자체를 대체하지 않는다.
품질 판단이 필요할 때는 이 문서를 사용하고, 실제 설계/보안/프론트/백엔드 기준은 각 원문 문서를 따른다.

관련 기준 문서:

- [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
- [ARCHITECTURE.md](/Users/hj.park/projects/coin-zzickmock/ARCHITECTURE.md)
- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
- [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)

## When To Use

아래 상황에서는 이 문서를 루프의 마지막 단계에 적용한다.

- 코드 수정 직후
- 리팩터링 직후
- 큰 문서 기준을 새로 만들거나 바꾼 직후
- PR 전 self-review
- 에이전트가 "끝난 것 같은데 정말 끝났는지" 판단해야 할 때

기본 검토 대상:

- 사용자가 지정한 diff
- 지정이 없으면 현재 working tree 변경분

## Review Panel

리뷰는 `multi-angle-review` 방식으로 독립적으로 수행한다.
각 각도는 서로의 결론을 보지 않은 상태에서 현재 스냅샷만 검토한다.

필수 각도:

1. `readability-reviewer`
2. `performance-reviewer`
3. `security-reviewer`
4. `test-reviewer`
5. `architecture-reviewer`

강한 규칙:

- 한 리뷰어의 결과를 다른 리뷰어 프롬프트에 넣지 않는다.
- 수정 의도나 기대 답안을 리뷰어에게 미리 주지 않는다.
- 합성은 모든 각도 리뷰가 끝난 뒤에만 한다.

## Recommended Reviewer Prompt

각 reviewer에는 아래 형태의 짧은 프롬프트를 권장한다.

```text
Review the current target using the `<angle>` angle.
Inspect only the current snapshot and do not rely on prior reviews.
Follow the repository guidance relevant to this target.
Return:
- findings ordered by severity
- concrete evidence with file references
- residual risks if no issues are found
- one angle score from 0 to 100 using QUALITY_SCORE.md
```

합성 단계에서는 reviewer별 점수와 finding을 모은 뒤, 이 문서의 penalty와 hard gate를 적용한다.

## Review Inputs

리뷰 단계에 들어가기 전에 아래 입력을 정리한다.

- review target
- 관련 기준 문서
- 실행한 테스트와 결과
- 알려진 제약사항

최소 기준 문서 선택:

- 프론트 변경: [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
- 백엔드 변경: [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- 보안 관련 변경: [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)
- 구조 변경: [ARCHITECTURE.md](/Users/hj.park/projects/coin-zzickmock/ARCHITECTURE.md)

## Scoring Model

총점은 100점 만점이다.
각 각도 점수를 가중 평균한 뒤, unresolved finding penalty와 hard gate를 적용한다.

### Angle Weights

- Readability: 15
- Performance: 20
- Security: 25
- Test Quality: 20
- Architecture: 20

총합은 100이다.

### Per-angle Base Score

각 리뷰어는 자신의 각도에서 0~100점으로 평가한다.

권장 해석:

- 95~100: 즉시 merge 가능한 수준, 눈에 띄는 리스크 없음
- 90~94: 사소한 개선점은 있으나 루프 재진입까지는 불필요
- 80~89: 개선 필요성이 있으나 구조적으로는 안전
- 70~79: 다시 손봐야 할 가능성이 높음
- 0~69: 현재 상태로는 종료하면 안 됨

### Severity Penalty

합성 단계에서 unresolved finding에 대해 아래 penalty를 추가 적용한다.

- Critical: -30
- High: -20
- Medium: -10
- Low: -3

같은 문제를 여러 각도가 지적해도 penalty는 한 번만 적용한다.
단, 중복 지적은 confidence 상승 근거로 기록한다.

### Hard Gates

아래 조건 중 하나라도 만족하면 총점과 무관하게 루프를 종료하면 안 된다.

- unresolved `Critical` finding이 1개 이상 있음
- unresolved `High` severity security finding이 1개 이상 있음
- unresolved `High` severity architecture finding이 1개 이상 있음
- 핵심 테스트가 실패했음
- 요구된 테스트를 아예 실행하지 못했고, 그 이유가 정당화되지 않음
- 변경이 명세와 충돌하는데 해결되지 않았음

즉, 숫자만 높다고 통과시키지 않는다.

## Exit Thresholds

### Pass

아래를 모두 만족하면 에이전트 루프를 종료해도 된다.

- final score `>= 85`
- unresolved `Critical` 없음
- unresolved `High` 없음
- 실행 가능한 핵심 테스트 통과
- 관련 기준 문서와 명백히 충돌하지 않음

### Fix And Re-run

아래 중 하나면 수정 후 같은 루프를 다시 돈다.

- final score `70~84`
- unresolved `Medium`이 남아 있음
- 테스트는 통과했지만 구조/보안/가독성 리스크가 반복 지적됨

### Major Rework

아래 중 하나면 부분 수정보다 설계 재검토를 먼저 한다.

- final score `< 70`
- hard gate 위반
- 첫 수정 이후 점수 개선이 거의 없고 같은 핵심 문제가 반복됨

## Loop Algorithm

에이전트 루프는 아래 순서를 따른다.

1. 구현 또는 수정
2. 가능한 테스트 실행
3. `multi-angle-review` 방식으로 독립 리뷰 수행
4. angle score와 finding을 합성
5. final score 계산
6. hard gate 확인
7. 결과 분기

분기 규칙:

- `Pass`: 루프 종료
- `Fix And Re-run`: finding을 severity 순으로 해결하고 다시 2단계부터 수행
- `Major Rework`: 원인 분석 후 설계/구조 자체를 먼저 수정하고 다시 2단계부터 수행

## Final Score Formula

권장 계산식:

```text
weighted_score =
  readability * 0.15 +
  performance * 0.20 +
  security * 0.25 +
  test * 0.20 +
  architecture * 0.20

final_score = max(0, weighted_score - unresolved_penalty_total)
```

예시:

- readability 90
- performance 84
- security 92
- test 80
- architecture 88

그러면:

```text
weighted_score = 87.4
```

여기서 unresolved medium 1개와 low 2개가 남아 있으면:

```text
final_score = 87.4 - 10 - 3 - 3 = 71.4
```

이 경우 `Fix And Re-run`이다.

## Required Review Output

리뷰 단계의 최종 출력에는 아래 항목이 반드시 있어야 한다.

- review target
- scope summary
- angle score 5개
- merged findings
- unresolved findings
- executed tests
- final score
- pass / fix-and-re-run / major-rework 판정

권장 출력 형식:

```text
Review Target: <diff or files>
Scope: <short summary>

Angle Scores
- Readability: 88
- Performance: 82
- Security: 91
- Test: 79
- Architecture: 85

Merged Findings
- High | Security | ...
- Medium | Test | ...

Executed Tests
- ./gradlew clean test : passed

Final Score: 81
Decision: Fix And Re-run
```

## Fix Prioritization

루프 재진입 시에는 아래 순서로 수정한다.

1. hard gate 위반
2. Critical
3. High
4. Medium
5. Low
6. 문서 표현/미세한 정리

강한 규칙:

- 점수만 올리기 위한 cosmetic fix에 먼저 시간을 쓰지 않는다.
- 보안/구조/테스트 문제를 남긴 채 가독성 점수만 높이는 식의 대응을 금지한다.

## Angle-specific Rubric

### Readability

본다:

- 이름이 역할을 드러내는가
- 파일 책임이 명확한가
- 불필요한 복잡도나 중복이 있는가
- 문서와 코드가 서로 찾기 쉬운가

감점 예:

- 의미 없는 추상화
- 너무 큰 파일
- 역할이 섞인 클래스
- 링크는 있는데 실제 탐색 흐름이 불명확한 문서

### Performance

본다:

- 불필요한 반복 계산
- 과도한 렌더링/쿼리/IO
- hot path에 큰 비용이 숨어 있는가
- 문서 기준이라면 루프 비용이나 검증 비용이 과도하지 않은가

### Security

본다:

- 민감 정보 노출
- 인증/인가 경계 누락
- 외부 입력 신뢰
- 리뷰 루프에서 보안 hard gate가 빠져 있지 않은가

### Test Quality

본다:

- 변경을 보호하는 테스트가 있는가
- 테스트 누락이 명백한가
- 테스트 결과를 품질 판단에 반영했는가
- "테스트를 못 돌림"이 무비판적으로 통과되지 않는가

### Architecture

본다:

- 기존 기준 문서와 충돌하는가
- 레이어/경계/책임 분리가 유지되는가
- 루프 규칙이 예측 가능하고 재현 가능한가
- 프로젝트 인덱스와 기준 문서가 서로 맞물리는가

## Maximum Iterations

무한 루프를 막기 위해 같은 작업에 대한 품질 루프는 기본 3회까지 권장한다.

- 1회차: 문제 발견
- 2회차: 주요 수정
- 3회차: 수렴 확인

3회차 이후에도 hard gate나 동일 high-severity finding이 반복되면, 단순 수정 루프가 아니라 설계 재정의나 범위 재협상이 필요하다고 판단한다.

## Special Rules For Documentation-only Changes

문서만 바뀐 작업이라고 품질 루프를 생략하지 않는다.
다만 아래처럼 해석을 조정한다.

- 테스트 점수는 "실행 테스트"보다 "검증 가능성"과 "오해 방지력"을 더 본다.
- architecture, readability 비중 판단이 더 중요해진다.
- 문서가 실제 작업 루프를 더 안전하게 만드는지 본다.

문서 작업의 pass 최소 조건:

- 탐색 경로가 명확함
- 원문과 인덱스 역할이 섞이지 않음
- 종료 조건과 재시도 조건이 모호하지 않음

## Default Agent Policy

에이전트는 기본적으로 아래 정책을 따른다.

- review 없이 종료하지 않는다
- score 없이 "괜찮아 보인다"로 종료하지 않는다
- hard gate 위반 상태에서 점수만으로 종료하지 않는다
- threshold 미달이면 수정 후 다시 review한다

기본 정책 요약:

- `final_score >= 85` and no hard gate violation: 종료
- `final_score < 85`: 수정 후 재검토
- any hard gate violation: 즉시 재작업
