import MarketsLanding from "@/components/router/(main)/markets/MarketsLanding";
import { getFuturesMarkets } from "@/lib/futures-api";
import { MARKET_SNAPSHOTS } from "@/lib/markets";

export default async function MarketsPage() {
  const markets = await getFuturesMarkets();
  const [btcMarket = MARKET_SNAPSHOTS.BTCUSDT, ethMarket = MARKET_SNAPSHOTS.ETHUSDT] =
    markets;

  return <MarketsLanding markets={[btcMarket, ethMarket]} />;
}
