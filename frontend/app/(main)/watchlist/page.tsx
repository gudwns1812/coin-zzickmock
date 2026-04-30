import { getFuturesMarkets } from "@/lib/futures-api";
import { formatPercent, formatUsd } from "@/lib/markets";
import Link from "next/link";

export default async function WatchlistPage() {
  const markets = await getFuturesMarkets();

  return (
    <div className="px-main-2 pb-24 flex flex-col gap-8 pt-4">
      <section className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray">
        <p className="text-sm-custom text-main-dark-gray/60">Watchlist</p>
        <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
          관심 심볼
        </h1>
      </section>

      <section className="grid grid-cols-2 gap-main-2">
        {markets.map((market) => (
          <Link
            key={market.symbol}
            href={`/markets/${market.symbol}`}
            className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray flex flex-col gap-4"
          >
            <div className="flex items-start justify-between gap-main">
              <div>
                <p className="text-xs-custom uppercase tracking-[0.2em] text-main-dark-gray/50">
                  {market.symbol}
                </p>
                <h2 className="mt-2 text-2xl-custom font-bold text-main-dark-gray">
                  {market.displayName}
                </h2>
              </div>
              <span className="rounded-full bg-main-blue/10 px-3 py-1 text-xs-custom text-main-blue">
                Watch
              </span>
            </div>
            <div className="grid grid-cols-3 gap-main">
              <MiniMetric label="가격" value={formatUsd(market.lastPrice)} />
              <MiniMetric label="24h" value={formatPercent(market.change24h)} />
              <MiniMetric
                label="Funding"
                value={formatPercent(market.fundingRate * 100)}
              />
            </div>
          </Link>
        ))}
      </section>
    </div>
  );
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-main bg-main-light-gray/40 px-main py-3">
      <p className="text-xs-custom text-main-dark-gray/60">{label}</p>
      <p className="mt-2 text-sm-custom font-semibold text-main-dark-gray">{value}</p>
    </div>
  );
}
