# Bitget Tickers Channel

이 문서는 Bitget 공개 WebSocket ticker 채널을 실시간 시세 공급원으로 검토할 때 읽는 참고 노트다.
구체적인 subscribe 인자와 응답 스키마는 구현 시점에 공식 Bitget 문서 원문을 다시 대조한다.

## 언제 먼저 읽는가

- REST polling 기반 최신가 조회를 WebSocket 기반으로 대체하려 할 때
- 상세 페이지의 최신 체결가, mark price, funding 관련 스냅샷을 한 번에 갱신하려 할 때
- 심볼 목록과 상세 페이지가 같은 시세 원천을 공유해야 할 때

## 이 저장소에서 확인할 포인트

- 현재 `BitgetMarketDataGateway`가 REST 응답으로 읽는 필드와 ticker 채널 필드가 일치하는지
- SSE 브로드캐스트 전에 백엔드 캐시를 갱신하는 흐름으로 연결할지
- 구독 수가 늘어도 심볼별 중복 연결 없이 fan-out할 수 있는지
- WebSocket 장애 시 REST polling fallback을 유지할지

## 함께 볼 문서

- [bitget-quickstart.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/bitget-quickstart.md)
- [best-practice-guide.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/best-practice-guide.md)
- [MarketRealtimeService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/MarketRealtimeService.java)
