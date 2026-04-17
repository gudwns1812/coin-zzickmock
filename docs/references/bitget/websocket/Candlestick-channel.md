# Bitget Candlestick Channel

이 문서는 Bitget 공개 WebSocket의 캔들 채널을 구현하거나 검토할 때 빠르게 읽는 참고 메모다.
정확한 subscribe payload, 필드 이름, 응답 예시는 공식 Bitget 문서 원문과 함께 확인한다.

## 언제 먼저 읽는가

- 과거 가격 수집이 아니라 실시간 캔들 스트림이 필요할 때
- `1m`, `5m`, `15m`, `1h` 같은 주기별 차트 갱신 구조를 설계할 때
- REST 기반 차트 조회를 WebSocket 기반 보강 흐름으로 확장할 때

## 이 저장소에서 확인할 포인트

- 지원 심볼명과 Bitget 채널의 상품 코드 표기가 현재 `market_symbols.symbol`과 바로 연결되는지
- 프론트 즉시 갱신용인지, 백엔드 수집/롤업용인지에 따라 소비 위치를 분리할지
- 봉 확정 전 업데이트와 봉 마감 이벤트를 같은 upsert 규칙으로 처리할지
- 재연결 시 마지막 `open_time` 기준으로 중복 캔들을 어떻게 무해하게 흡수할지

## 함께 볼 문서

- [bitget-quickstart.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/bitget-quickstart.md)
- [best-practice-guide.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/best-practice-guide.md)
- [coin-futures-candle-timeframe-spec.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-candle-timeframe-spec.md)
