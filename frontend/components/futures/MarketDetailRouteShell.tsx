"use client";

import MarketDetailRealtimeView from "@/components/futures/MarketDetailRealtimeView";
import { MARKET_SNAPSHOTS, type MarketSymbol } from "@/lib/markets";

export default function MarketDetailRouteShell({
  symbol,
}: {
  symbol: MarketSymbol;
}) {
  const market = MARKET_SNAPSHOTS[symbol];

  return (
    <MarketDetailRealtimeView
      accountSummary={null}
      chartOpenOrders={[]}
      chartPositions={[]}
      initialMarket={market}
      isAuthenticated={false}
      isInitialMarketDataDegraded
      key={market.symbol}
      openOrders={[]}
      orderHistory={[]}
      positionHistory={[]}
      positions={[]}
    />
  );
}
