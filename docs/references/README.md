# References

이 디렉터리는 조사 메모, 외부 레퍼런스, 비교 자료, ADR 성격의 문서를 두는 곳이다.

## What Goes Here

- 외부 기술 조사
- 라이브러리 선택 비교
- 아키텍처 대안 검토
- ADR 초안 또는 결정 메모

## How To Use

- 구현 기준 문서의 근거가 필요할 때 먼저 이 디렉터리를 본다.
- 장기 기준서에 바로 넣기 애매한 배경 설명은 여기로 내린다.
- 기준이 확정되면 루트 문서나 `docs/design-docs/`에서 이 문서를 링크한다.
- 외부 거래소 연동, 시세 수집, 상품 메타데이터 확인처럼 Bitget 계약을 직접 읽어야 하는 작업이면 이 디렉터리의 Bitget 문서를 먼저 확인한다.

## Current Reference Areas

### Bitget

- [`bitget/bitget-quickstart.md`](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/bitget-quickstart.md)
  REST/WebSocket 엔드포인트, 인증, 기본 연결 절차를 확인할 때 먼저 본다.
- [`bitget/bitget-GetInstruments.md`](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/bitget-GetInstruments.md)
  선물 심볼 목록, 가격 단위, 주문 수량 제한, 상태값 같은 상품 메타데이터를 확인할 때 본다.
- [`bitget/bitget-GetCurrentFundingRate.md`](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/bitget-GetCurrentFundingRate.md)
  현재 펀딩비, 정산 주기, 다음 갱신 시각 같은 funding 데이터를 확인할 때 본다.
- [`bitget/best-practice-guide.md`](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/best-practice-guide.md)
  페이지네이션, WebSocket 운용, 운영 시 주의사항을 확인할 때 본다.

## Recommended Files

- `README.md`
  이 디렉터리 입구 문서
- `adr-xxxx-<topic>.md`
  결정 문서
- `<topic>-research.md`
  조사 문서
