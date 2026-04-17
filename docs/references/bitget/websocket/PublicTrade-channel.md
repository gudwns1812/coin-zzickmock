# Bitget Public Trade Channel

이 문서는 Bitget 공개 WebSocket의 체결 스트림을 붙일 때 확인해야 할 체크리스트를 모아 둔 메모다.
정확한 필드 사양과 예시 payload는 구현 시점에 공식 Bitget 문서 원문을 다시 확인한다.

## 언제 먼저 읽는가

- 체결 단위의 최신가 반영이 필요할 때
- ticker보다 더 촘촘한 체결 이벤트를 기반으로 가격 UI를 갱신할 때
- 체결 이벤트를 캔들 집계나 실시간 체결 목록에 재사용하려 할 때

## 이 저장소에서 확인할 포인트

- 상세 화면의 최신 체결가 갱신을 ticker polling/SSE보다 더 낮은 지연으로 바꿀 가치가 있는지
- 거래량, 체결 시각, 매수/매도 방향 같은 필드가 실제 UI 또는 저장소 요구와 맞는지
- 이벤트 순서가 바뀌거나 중복 수신될 때 가격 히스토리 집계가 깨지지 않는지
- 현재 `MarketRealtimeService` 같은 메모리 캐시 계층으로 흡수할지, 별도 ingest 경로를 둘지

## 함께 볼 문서

- [bitget-quickstart.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/bitget-quickstart.md)
- [best-practice-guide.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/best-practice-guide.md)
- [2026-04-17-market-realtime-price-stream.md](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/active/2026-04-17-market-realtime-price-stream.md)
