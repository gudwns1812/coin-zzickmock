import MarketsLandingRealtimeView from "@/components/router/(main)/markets/MarketsLandingRealtimeView";
import {
  getFuturesAccountSummary,
  getFuturesMarketsResult,
  getFuturesPositions,
  getFuturesReward,
} from "@/lib/futures-api";
import {
  formatSignedUsd,
  formatUsd,
  MARKET_SNAPSHOTS,
} from "@/lib/markets";

const DEMO_STARTING_BALANCE = 100_000;

export default async function MarketsPage() {
  const [marketsResult, account, positions, reward] = await Promise.all([
    getFuturesMarketsResult(),
    getFuturesAccountSummary(),
    getFuturesPositions(),
    getFuturesReward(),
  ]);
  const { markets, isFallback } = marketsResult;
  const [btcMarket = MARKET_SNAPSHOTS.BTCUSDT, ethMarket = MARKET_SNAPSHOTS.ETHUSDT] =
    markets;
  const todayProfit = positions.reduce(
    (sum, position) => sum + position.unrealizedPnl,
    0
  );
  const totalAsset = account.walletBalance + todayProfit;
  const totalProfit = totalAsset - DEMO_STARTING_BALANCE;
  const summaryCards = [
    {
      title: "총 자산",
      value: formatUsd(totalAsset),
      support: `가용 잔고 ${formatUsd(account.available)}`,
      icon: "wallet" as const,
      tone: "accent" as const,
    },
    {
      title: "총 수익",
      value: formatSignedUsd(totalProfit),
      support: `누적 포인트 ${reward.rewardPoint}P`,
      icon: "trophy" as const,
      tone: totalProfit >= 0 ? ("positive" as const) : ("negative" as const),
    },
    {
      title: "오늘 수익",
      value: formatSignedUsd(todayProfit),
      support:
        positions.length > 0
          ? `열린 포지션 ${positions.length}건 기준`
          : "열린 포지션 없음",
      icon: "trend" as const,
      tone: todayProfit >= 0 ? ("positive" as const) : ("negative" as const),
    },
  ];

  return (
    <div className="mx-auto w-full max-w-[1200px]">
      <MarketsLandingRealtimeView
        initialMarkets={[btcMarket, ethMarket]}
        isMarketDataDegraded={isFallback}
        summaryCards={summaryCards}
      />
    </div>
  );
}
