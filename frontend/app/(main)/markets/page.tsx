import MarketsLandingRealtimeView from "@/components/router/(main)/markets/MarketsLandingRealtimeView";
import { getFuturesMarketsResult } from "@/lib/futures-api";
import { MARKET_SNAPSHOTS } from "@/lib/markets";

export default async function MarketsPage() {
  const { markets, isFallback } = await getFuturesMarketsResult();
  const [btcMarket = MARKET_SNAPSHOTS.BTCUSDT, ethMarket = MARKET_SNAPSHOTS.ETHUSDT] =
    markets;

  return (
    <div className="mx-auto w-full max-w-[1200px]">
      <MarketsLandingRealtimeView
        initialMarkets={[btcMarket, ethMarket]}
        isMarketDataDegraded={isFallback}
        isAuthenticated={false}
      />
    </div>
  );
}
