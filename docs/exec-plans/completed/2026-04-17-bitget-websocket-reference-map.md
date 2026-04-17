# Bitget WebSocket 참고 문서 인덱스 추가

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
문서 정리 작업이므로 별도 구현 테스트 대신 변경 범위와 링크 정합성 확인을 검증 기준으로 삼는다.

## 목적 / 큰 그림

Bitget 참고 문서는 이미 `docs/references/bitget` 아래에 있었지만, 공개 WebSocket 채널 쪽은 폴더만 생기고 어떤 문서를 먼저 읽어야 하는지가 드러나지 않았다.
이번 작업이 끝나면 에이전트와 개발자가 `AGENTS.md`만 읽어도 WebSocket 참고 폴더로 들어갈 수 있고, 캔들/체결/ticker 문서가 각각 어떤 상황에서 먼저 읽히는지 최소 설명이 붙어 있어야 한다.

## 진행 현황

- [x] (2026-04-17 11:27+09:00) 기존 `AGENTS.md`와 Bitget references 폴더 구조 재확인
- [x] (2026-04-17 11:28+09:00) `AGENTS.md`에 WebSocket 참고 폴더 링크 추가
- [x] (2026-04-17 11:31+09:00) 캔들/체결/ticker 문서에 사용 맥락과 함께 볼 문서 메모 추가

## 결과 및 회고

- `AGENTS.md`에서 Bitget WebSocket 참고 폴더로 바로 진입할 수 있게 됐다.
- `docs/references/bitget/websocket/`의 세 문서는 더 이상 빈 파일이 아니며, 각 채널을 언제 먼저 읽는지와 이 저장소에서 확인할 포인트를 짧게 남긴다.
- 외부 API의 정확한 subscribe payload와 응답 필드는 변경 가능성이 있으므로, 문서에는 구현 관점의 체크리스트만 남기고 원문 스펙은 공식 Bitget 문서를 다시 보도록 했다.

## 검증과 수용 기준

- `AGENTS.md`에서 `docs/references/bitget/websocket` 링크가 보인다.
- `Candlestick-channel.md`, `PublicTrade-channel.md`, `Tickers-channel.md`가 빈 파일이 아니다.
- 각 문서에 "언제 먼저 읽는가" 또는 동등한 사용 맥락 설명이 있다.
