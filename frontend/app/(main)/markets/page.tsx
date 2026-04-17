import MarketsLandingRealtimeView from "@/components/router/(main)/markets/MarketsLandingRealtimeView";
import { getFuturesMarkets } from "@/lib/futures-api";
import { MARKET_SNAPSHOTS } from "@/lib/markets";

export default async function MarketsPage() {
  const markets = await getFuturesMarkets();
  const [btcMarket = MARKET_SNAPSHOTS.BTCUSDT, ethMarket = MARKET_SNAPSHOTS.ETHUSDT] =
    markets;

  return <MarketsLandingRealtimeView initialMarkets={[btcMarket, ethMarket]} />;
}
