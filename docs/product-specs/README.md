# Product Specs

이 디렉터리는 제품 요구사항과 기능 명세 문서를 두는 곳이다.

## What Goes Here

- 기능 요구사항
- 사용자 시나리오
- 수용 기준
- 화면/도메인 단위 명세

## How To Use

- 새 기능을 시작하기 전, 관련 명세가 있으면 먼저 읽는다.
- 명세가 없는데 구현을 시작해야 하면, 최소한의 초안 명세를 먼저 만든다.
- 구현이 명세를 바꾸면 명세 문서도 같이 갱신한다.
- 손익, 마진, 청산가, 펀딩, 포인트, 랭킹, 차트 파생값처럼 계산 공식이나 판단 기준이 추가/변경되면 구현과 같은 작업에서 제품 명세 또는 설계 문서에 반드시 기록한다. 이후 구현자는 공식이 헷갈릴 때 코드 추측이 아니라 문서 원문을 기준으로 삼아야 한다.

## Current Specs

- [coin-futures-platform-mvp.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-platform-mvp.md)
  코인 선물 모의투자 플랫폼의 첫 제품 설계 초안. 목표, MVP 범위, 도메인 모델, 시스템 구조, 프론트 재사용 전략을 담는다.
- [coin-futures-screen-spec.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-screen-spec.md)
  회원가입, 마켓, 심볼 상세, 포트폴리오, 상점까지 MVP 화면을 더 작은 블록으로 나눈 화면 명세.
- [coin-futures-simulation-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-simulation-rules.md)
  주문 체결, 수수료, 손익, 마진, 펀딩비, 포인트 적립 규칙을 고정하는 계산 명세.
- [coin-futures-candle-timeframe-spec.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-candle-timeframe-spec.md)
  과거 가격 차트가 지원해야 하는 기간 목록과, 현재 DB의 `1m`/`1h` 저장 구조에서 어떤 기간을 어떻게 파생해야 하는지 정리한 명세.

## Recommended Files

- `README.md`
  이 디렉터리 입구 문서
- `<feature-name>.md`
  개별 기능 명세
- `index.md` 또는 별도 목록 문서
  명세가 많아질 때 사용
