"use client";

import MarketDetailRouteShell from "@/components/futures/MarketDetailRouteShell";
import { SUPPORTED_MARKET_SYMBOLS, type MarketSymbol } from "@/lib/markets";
import { usePathname } from "next/navigation";

export default function MarketDetailLoading() {
  return <MarketDetailRouteShell symbol={resolveMarketSymbolFromPathname()} />;
}

function resolveMarketSymbolFromPathname(): MarketSymbol {
  const pathname = usePathname();
  const symbol = pathname.split("/").filter(Boolean).at(-1);

  if (isMarketSymbol(symbol)) {
    return symbol;
  }

  return "BTCUSDT";
}

function isMarketSymbol(value: string | undefined): value is MarketSymbol {
  return SUPPORTED_MARKET_SYMBOLS.includes(value as MarketSymbol);
}
