import MarketDetailRealtimeView from "@/components/futures/MarketDetailRealtimeView";
import {
  getFuturesAccountSummary,
  getFuturesMarket,
  getFuturesOpenOrders,
  getFuturesOrderHistory,
  getFuturesPositionHistory,
  getFuturesPositions,
  isSupportedFuturesSymbol,
} from "@/lib/futures-api";
import { notFound } from "next/navigation";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ symbol: string }>;
}) {
  const { symbol } = await params;

  if (!isSupportedFuturesSymbol(symbol)) {
    return {
      title: "잘못된 심볼",
    };
  }

  return {
    title: `${symbol} 마켓`,
    description: `${symbol} 선물 마켓의 시세와 주문 입력을 확인하는 페이지`,
  };
}

export default async function MarketDetailPage({
  params,
}: {
  params: Promise<{ symbol: string }>;
}) {
  const { symbol } = await params;

  if (!isSupportedFuturesSymbol(symbol)) {
    notFound();
  }

  const [market, positions, openOrders, orderHistory, positionHistory] =
    await Promise.all([
    getFuturesMarket(symbol),
    getFuturesPositions(),
    getFuturesOpenOrders(),
    getFuturesOrderHistory(),
    getFuturesPositionHistory(),
  ]);
  const accountSummary = await getFuturesAccountSummary();

  return (
    <MarketDetailRealtimeView
      initialMarket={market}
      isAuthenticated={false}
      key={market.symbol}
      accountSummary={accountSummary}
      chartOpenOrders={openOrders.filter((order) => order.symbol === symbol)}
      chartPositions={positions.filter((position) => position.symbol === symbol)}
      openOrders={openOrders}
      positions={positions}
      positionHistory={positionHistory}
      orderHistory={orderHistory}
    />
  );
}
