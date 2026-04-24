"use client";

import {
  formatPercent,
  formatUsd,
  MARKET_RANKING_FALLBACKS,
  type MarketRankingEntry,
  type MarketSnapshot,
  type MarketSymbol,
} from "@/lib/markets";
import {
  Trophy,
  TrendingUp,
  WalletCards,
} from "lucide-react";
import Link from "next/link";
import { useState } from "react";

export type DashboardSummaryCard = {
  title: string;
  value: string;
  support: string;
  icon: "wallet" | "trophy" | "trend";
  tone: "accent" | "positive" | "negative";
};

export type PriceFlashRenderState = {
  tone: "rise" | "fall";
  intensity: number;
};

type MarketSortKey = "default" | "price" | "change24h" | "volume24h";

const SORT_BUTTONS: Array<{
  key: MarketSortKey;
  label: string;
}> = [
  { key: "default", label: "기본순" },
  { key: "price", label: "가격순" },
  { key: "change24h", label: "변동률순" },
  { key: "volume24h", label: "거래량순" },
];

const MARKET_SORT_VALUE: Record<
  Exclude<MarketSortKey, "default">,
  (market: MarketSnapshot) => number
> = {
  price: (market) => market.lastPrice,
  change24h: (market) => market.change24h,
  volume24h: (market) => (market.hasExtendedMetrics ? market.volume24h : -1),
};

type MarketsLandingProps = {
  isMarketDataDegraded: boolean;
  markets: [MarketSnapshot, MarketSnapshot];
  summaryCards: DashboardSummaryCard[];
  lastUpdatedLabel: string;
  rankingEntries?: MarketRankingEntry[];
  priceFlashBySymbol?: Partial<Record<MarketSymbol, PriceFlashRenderState>>;
};

export default function MarketsLanding({
  isMarketDataDegraded,
  markets,
  summaryCards,
  lastUpdatedLabel,
  rankingEntries = MARKET_RANKING_FALLBACKS,
  priceFlashBySymbol,
}: MarketsLandingProps) {
  const [sortKey, setSortKey] = useState<MarketSortKey>("default");
  const sortedMarkets = [...markets].sort((left, right) => {
    if (sortKey === "default") {
      return 0;
    }

    return MARKET_SORT_VALUE[sortKey](right) - MARKET_SORT_VALUE[sortKey](left);
  });

  return (
    <div className="px-main-3 pb-24 pt-2 flex flex-col gap-7">
      <section className="flex items-end justify-between gap-main">
        <div>
          <h1 className="text-4xl-custom font-bold text-main-dark-gray">
            메인 대시보드
          </h1>
          <p className="mt-3 text-base-custom text-main-dark-gray/60">
            포트폴리오 및 시장 현황
          </p>
        </div>
        <p className="text-xs-custom text-main-dark-gray/50">
          Last update: {lastUpdatedLabel}
        </p>
      </section>

      <section className="grid grid-cols-3 gap-main-2">
        {summaryCards.map((card) => (
          <SummaryMetricCard key={card.title} card={card} />
        ))}
      </section>

      <section className="rounded-main border border-white/50 bg-white/70 backdrop-blur-md shadow-md overflow-hidden transition-all duration-300 hover:shadow-lg">
        <div className="flex items-start justify-between gap-main border-b border-main-light-gray/40 px-main-2 py-6">
          <div>
            <h2 className="text-xl-custom font-semibold text-main-dark-gray">
              코인 시세
            </h2>
            <p className="mt-2 text-sm-custom text-main-dark-gray/62 break-keep">
              가격, 24시간 변동, 거래량, 펀딩비, 시가총액과 점유율을 한 번에
              비교할 수 있습니다.
            </p>
          </div>
          <div className="flex items-center gap-2 rounded-main bg-main-light-gray/30 p-1">
            {SORT_BUTTONS.map((button) => {
              const isActive = sortKey === button.key;

              return (
                <button
                  key={button.key}
                  type="button"
                  onClick={() => setSortKey(button.key)}
                  className={`rounded-main px-3 py-2 text-xs-custom font-semibold transition-all duration-200 ${
                    isActive
                      ? "bg-main-blue text-white shadow-md"
                      : "text-main-dark-gray/60 hover:bg-main-blue/10 hover:text-main-blue"
                  }`}
                >
                  {button.label}
                </button>
              );
            })}
          </div>
        </div>

        {isMarketDataDegraded ? (
          <div className="border-b border-main-light-gray/40 bg-[#fff6eb]/80 px-main-2 py-4 text-sm-custom text-[#9a5a00]">
            외부 시세 수집에 실패했습니다. 현재 시장 데이터는 복구 중이며, 필드는
            `-`로 표시됩니다.
          </div>
        ) : null}

        <div className="overflow-x-auto">
          <table className="min-w-full table-fixed">
            <thead className="bg-main-blue/[0.02]">
              <tr className="text-left text-xs-custom text-main-dark-gray/60">
                <th className="px-main py-5 font-medium">코인</th>
                <th className="px-main py-5 font-medium">가격</th>
                <th className="px-main py-5 font-medium">24h 변동</th>
                <th className="px-main py-5 font-medium">Mark Price</th>
                <th className="px-main py-5 font-medium">Funding Rate</th>
                <th className="px-main py-5 font-medium">Index Price</th>
                <th className="px-main py-5 font-medium">시장 메모</th>
              </tr>
            </thead>
            <tbody>
              {sortedMarkets.map((market) => (
                <MarketTableRow
                  isMarketDataDegraded={isMarketDataDegraded}
                  key={market.symbol}
                  market={market}
                  priceFlash={priceFlashBySymbol?.[market.symbol]}
                />
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="rounded-main border border-white/50 bg-white/70 backdrop-blur-md shadow-md overflow-hidden transition-all duration-300 hover:shadow-lg">
        <div className="flex items-start justify-between gap-main border-b border-main-light-gray/40 px-main-2 py-6">
          <div>
            <h2 className="text-xl-custom font-semibold text-main-dark-gray">
              수익률 랭킹
            </h2>
            <p className="mt-2 text-sm-custom text-main-dark-gray/62 break-keep">
              서비스 내 상위 트레이더 흐름을 빠르게 살펴보고 현재 계정 화면으로
              이어질 수 있습니다.
            </p>
          </div>
          <Link
            href="/portfolio"
            className="rounded-main border border-main-light-gray bg-white/50 px-main py-3 text-sm-custom font-semibold text-main-dark-gray transition-all hover:border-main-blue hover:text-main-blue hover:bg-white"
          >
            포트폴리오 확인
          </Link>
        </div>

        {rankingEntries.length === 0 ? (
          <div className="px-main-2 py-8 text-sm-custom text-main-dark-gray/62">
            데이터 집계 중
          </div>
        ) : (
          <div className="divide-y divide-main-light-gray/30">
            {rankingEntries.map((entry) => (
              <RankingRow key={entry.rank} entry={entry} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function SummaryMetricCard({ card }: { card: DashboardSummaryCard }) {
  const toneClassName =
    card.tone === "accent"
      ? {
          panel: "border-white/50 bg-main-blue/[0.04]",
          icon: "bg-main-blue/15 text-main-blue shadow-sm",
          value: "text-main-dark-gray",
          support: "text-main-dark-gray/55",
        }
      : card.tone === "positive"
        ? {
            panel: "border-white/50 bg-[#21a453]/[0.04]",
            icon: "bg-[#21a453]/15 text-[#21a453] shadow-sm",
            value: "text-[#16a34a]",
            support: "text-[#16a34a]/80",
          }
        : {
            panel: "border-white/50 bg-main-red/[0.04]",
            icon: "bg-main-red/15 text-main-red shadow-sm",
            value: "text-main-red",
            support: "text-main-red/80",
          };

  return (
    <article
      className={`rounded-main border backdrop-blur-md px-main-2 py-6 shadow-sm transition-all duration-300 hover:shadow-md hover:-translate-y-1 ${toneClassName.panel}`}
    >
      <div className="flex items-start gap-3">
        <div
          className={`flex h-11 w-11 items-center justify-center rounded-main transition-transform hover:scale-110 ${toneClassName.icon}`}
        >
          <SummaryIcon icon={card.icon} />
        </div>
        <div>
          <p className="text-xs-custom text-main-dark-gray/60">{card.title}</p>
          <p className={`mt-3 text-3xl-custom font-bold ${toneClassName.value}`}>
            {card.value}
          </p>
          <p className={`mt-2 text-xs-custom ${toneClassName.support}`}>
            {card.support}
          </p>
        </div>
      </div>
    </article>
  );
}

function SummaryIcon({ icon }: { icon: DashboardSummaryCard["icon"] }) {
  if (icon === "wallet") return <WalletCards className="h-5 w-5" strokeWidth={2.2} />;
  if (icon === "trophy") return <Trophy className="h-5 w-5" strokeWidth={2.2} />;
  return <TrendingUp className="h-5 w-5" strokeWidth={2.2} />;
}

function MarketTableRow({
  isMarketDataDegraded,
  market,
  priceFlash,
}: {
  isMarketDataDegraded: boolean;
  market: MarketSnapshot;
  priceFlash?: PriceFlashRenderState;
}) {
  const changeClassName = market.change24h >= 0 ? "text-main-red" : "text-main-blue";
  const fundingClassName = market.fundingRate >= 0 ? "text-main-red" : "text-main-blue";
  const flashTone = priceFlash?.tone;
  const flashIntensity = priceFlash?.intensity ?? 0;
  const overlayClassName =
    flashTone === "rise"
      ? "bg-[linear-gradient(135deg,_rgba(34,197,94,0.24),_rgba(134,239,172,0.12))]"
      : "bg-[linear-gradient(135deg,_rgba(239,68,68,0.2),_rgba(252,165,165,0.12))]";
  return (
    <tr className="group border-b border-main-light-gray/30 text-sm-custom text-main-dark-gray transition-colors duration-200 hover:bg-main-blue/5 last:border-0">
      <td className="px-main py-5">
        <Link
          href={`/markets/${market.symbol}`}
          className="flex items-center gap-3 rounded-main transition-all group-hover:translate-x-1"
        >
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-[#4c8df7] to-[#2563eb] text-sm-custom font-bold text-white shadow-sm">
            {market.symbol.slice(0, 1)}
          </div>
          <div>
            <p className="font-semibold text-main-dark-gray">
              {market.symbol.replace("USDT", "")}
            </p>
            <p className="mt-1 text-xs-custom text-main-dark-gray/55">
              {market.assetName}
            </p>
          </div>
        </Link>
      </td>
      <td className="px-main py-5">
        <div
          className="relative overflow-hidden rounded-main bg-white/65 px-3 py-3 backdrop-blur-sm"
          style={{
            boxShadow:
              flashIntensity > 0
                ? `0 0 ${10 + flashIntensity * 10}px rgba(15, 23, 42, ${flashIntensity * 0.05})`
                : undefined,
          }}
        >
          {flashIntensity > 0 ? (
            <span
              aria-hidden="true"
              className={`pointer-events-none absolute inset-0 ${overlayClassName}`}
              style={{ opacity: Math.min(flashIntensity * 0.72, 0.72) }}
            />
          ) : null}
          <p className="relative z-10 font-semibold text-main-dark-gray">
            {isMarketDataDegraded ? "-" : formatUsd(market.lastPrice)}
          </p>
        </div>
      </td>
      <td className={`px-main py-5 font-semibold ${changeClassName}`}>
        {isMarketDataDegraded ? "-" : formatPercent(market.change24h)}
      </td>
      <td className="px-main py-5 text-main-dark-gray/70">
        {isMarketDataDegraded ? "-" : formatUsd(market.markPrice)}
      </td>
      <td className={`px-main py-5 font-semibold ${fundingClassName}`}>
        {isMarketDataDegraded ? "-" : formatPercent(market.fundingRate * 100)}
      </td>
      <td className="px-main py-5 text-main-dark-gray/70">
        {isMarketDataDegraded ? "-" : formatUsd(market.indexPrice)}
      </td>
      <td className="px-main py-5 text-main-blue/80 font-semibold">
        {market.openInterestLabel}
      </td>
    </tr>
  );
}

function RankingRow({ entry }: { entry: MarketRankingEntry }) {
  return (
    <div className="grid grid-cols-[120px_1fr_220px_180px] items-center gap-main px-main-2 py-6 transition-colors duration-200 hover:bg-main-blue/5">
      <div className="flex items-center gap-3">
        <RankBadge rank={entry.rank} />
        <span className="text-sm-custom font-semibold text-main-dark-gray/80">
          {entry.rank}위
        </span>
      </div>
      <p className="text-sm-custom font-semibold text-main-dark-gray">
        {entry.nickname}
      </p>
      <p className="text-sm-custom font-semibold text-main-dark-gray">
        {formatUsd(entry.totalAsset)}
      </p>
      <p className="text-sm-custom font-bold text-[#16a34a]">
        {formatPercent(entry.profitRate)}
      </p>
    </div>
  );
}

function RankBadge({ rank }: { rank: number }) {
  const toneClassName =
    rank === 1
      ? "bg-gradient-to-br from-[#ffd700] to-[#f79d00] text-white shadow-md"
      : rank === 2
        ? "bg-gradient-to-br from-[#e2e8f0] to-[#94a3b8] text-white shadow-sm"
        : rank === 3
          ? "bg-gradient-to-br from-[#fbc2eb] to-[#a6c1ee] text-white shadow-sm"
          : "bg-main-light-gray/50 text-main-dark-gray/70";

  return (
    <span
      className={`flex h-8 w-8 items-center justify-center rounded-full text-xs-custom font-bold ${toneClassName}`}
    >
      {rank}
    </span>
  );
}
