import MarketsLandingRealtimeView from "@/components/router/(main)/markets/MarketsLandingRealtimeView";
import { getFuturesMarketsResult } from "@/lib/futures-api";

export default async function MarketsPage() {
  const { markets, isFallback } = await getFuturesMarketsResult();
  const [btcMarket, ethMarket] = markets;

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
