# Bitget Reference Map

## Purpose

This document routes Bitget-related external adapter work to the repository reference documents.

## Required References

Start with:

- [../../../docs/references/README.md](/Users/hj.park/projects/coin-zzickmock/docs/references/README.md)
- [../../../docs/references/bitget/bitget-quickstart.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/bitget-quickstart.md)
- [../../../docs/references/bitget/best-practice-guide.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/best-practice-guide.md)

For market metadata:

- [../../../docs/references/bitget/bitget-GetInstruments.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/bitget-GetInstruments.md)
- [../../../docs/references/bitget/bitget-GetCurrentFundingRate.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/bitget-GetCurrentFundingRate.md)

For candles and websocket:

- [../../../docs/references/bitget/Get-History-Candle-Data.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/Get-History-Candle-Data.md)
- [../../../docs/references/bitget/websocket/Tickers-channel.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/websocket/Tickers-channel.md)
- [../../../docs/references/bitget/websocket/Candlestick-channel.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/websocket/Candlestick-channel.md)
- [../../../docs/references/bitget/websocket/PublicTrade-channel.md](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/websocket/PublicTrade-channel.md)

## Rule

Bitget raw DTOs and provider-specific reconnect/runtime behavior stay in `backend/external`.
`app` translates external updates into stream/storage-facing calls when leaf modules must cooperate.
