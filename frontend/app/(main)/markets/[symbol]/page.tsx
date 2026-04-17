import MarketDetailRealtimeView from "@/components/futures/MarketDetailRealtimeView";
import {
  getFuturesMarket,
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

  const [market, positions] = await Promise.all([
    getFuturesMarket(symbol),
    getFuturesPositions(),
  ]);
  const currentPositions = positions.filter((position) => position.symbol === symbol);

  return (
    <MarketDetailRealtimeView
      initialMarket={market}
      currentPositions={currentPositions}
    />
  );
}
