"use client";

import {
  formatPercent,
  formatRatioPercent,
  formatCompactUsd,
  formatUsd,
  getMarketLogoPath,
  getMarketRankIconPath,
  MARKET_RANKING_FALLBACKS,
  type MarketRankingEntry,
  type MarketSnapshot,
  type MarketSymbol,
} from "@/lib/markets";
import { getSignedFinancialTextClassName } from "@/lib/financial-tone";
import {
  consumePositionPeek,
  FuturesClientApiError,
  getPositionPeekLatest,
  searchFuturesLeaderboardMembers,
} from "@/lib/futures-client-api";
import type {
  PositionPeekPublicPosition,
  PositionPeekSnapshot,
  PositionPeekStatus,
  PositionPeekTarget,
} from "@/lib/futures-api";
import { getPositionPeekItemCount, getPositionPeekSnapshotCreatedAt } from "@/lib/position-peek-ui";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  CheckCircle2,
  Trophy,
  Search,
  TrendingUp,
  WalletCards,
  X,
} from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { type CSSProperties, useState } from "react";

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

type MarketSortKey = "default" | "price" | "change24h" | "turnover24hUsdt";

const SORT_BUTTONS: Array<{
  key: MarketSortKey;
  label: string;
}> = [
  { key: "default", label: "기본순" },
  { key: "price", label: "가격순" },
  { key: "change24h", label: "변동률순" },
  { key: "turnover24hUsdt", label: "거래대금순" },
];

const MARKET_SORT_VALUE: Record<
  Exclude<MarketSortKey, "default">,
  (market: MarketSnapshot) => number
> = {
  price: (market) => market.lastPrice,
  change24h: (market) => market.change24h,
  turnover24hUsdt: (market) =>
    market.hasExtendedMetrics ? market.turnover24hUsdt : -1,
};

function getRankPresentation(rank: number) {
  if (rank === 1) {
    return {
      rankLabel: "1위",
      rowClass:
        "border-l-[#f59e0b] bg-[linear-gradient(90deg,_rgba(245,158,11,0.14),_rgba(255,255,255,0.72)_46%,_rgba(255,255,255,0.35))]",
      selectedClass: "ring-[#f59e0b]/35",
      badgeHalo: "bg-[#f59e0b]/12 ring-[#fbbf24]/35",
      numericBadge: "bg-gradient-to-br from-[#fde68a] to-[#f59e0b] text-white shadow-md",
      accentText: "text-[#b45309]",
    };
  }

  if (rank === 2) {
    return {
      rankLabel: "2위",
      rowClass:
        "border-l-[#94a3b8] bg-[linear-gradient(90deg,_rgba(148,163,184,0.16),_rgba(255,255,255,0.7)_46%,_rgba(255,255,255,0.35))]",
      selectedClass: "ring-[#64748b]/30",
      badgeHalo: "bg-slate-200/55 ring-slate-300",
      numericBadge: "bg-gradient-to-br from-[#f8fafc] to-[#94a3b8] text-white shadow-sm",
      accentText: "text-slate-600",
    };
  }

  if (rank === 3) {
    return {
      rankLabel: "3위",
      rowClass:
        "border-l-[#b45309] bg-[linear-gradient(90deg,_rgba(180,83,9,0.13),_rgba(255,255,255,0.7)_46%,_rgba(255,255,255,0.35))]",
      selectedClass: "ring-[#b45309]/25",
      badgeHalo: "bg-orange-100/70 ring-orange-200",
      numericBadge: "bg-gradient-to-br from-[#fed7aa] to-[#b45309] text-white shadow-sm",
      accentText: "text-orange-700",
    };
  }

  return {
    rankLabel: `${rank.toLocaleString("ko-KR")}위`,
    rowClass: "border-l-main-light-gray bg-white/40",
    selectedClass: "ring-main-blue/30",
    badgeHalo: "bg-main-light-gray/35 ring-main-light-gray",
    numericBadge: "bg-main-light-gray/70 text-main-dark-gray/70",
    accentText: "text-main-blue",
  };
}

const PEEK_PANEL_ARROW_BASE_PX = 40;
const PEEK_PANEL_ROW_HEIGHT_PX = 82;

type MarketsLandingProps = {
  isMarketDataDegraded: boolean;
  isAuthenticated: boolean;
  markets: [MarketSnapshot, MarketSnapshot];
  summaryCards: DashboardSummaryCard[];
  lastUpdatedLabel: string;
  rankingEntries?: MarketRankingEntry[];
  priceFlashBySymbol?: Partial<Record<MarketSymbol, PriceFlashRenderState>>;
};

export default function MarketsLanding({
  isMarketDataDegraded,
  isAuthenticated,
  markets,
  summaryCards,
  lastUpdatedLabel,
  rankingEntries = MARKET_RANKING_FALLBACKS,
  priceFlashBySymbol,
}: MarketsLandingProps) {
  const [sortKey, setSortKey] = useState<MarketSortKey>("default");
  const [leaderboardSearchQuery, setLeaderboardSearchQuery] = useState("");
  const [selectedPeekTarget, setSelectedPeekTarget] = useState<PositionPeekTarget | null>(null);
  const trimmedLeaderboardSearchQuery = leaderboardSearchQuery.trim();
  const isSearchingLeaderboard = trimmedLeaderboardSearchQuery.length > 0;
  const leaderboardSearch = useQuery({
    queryKey: ["leaderboard-peek-search", trimmedLeaderboardSearchQuery],
    queryFn: () =>
      searchFuturesLeaderboardMembers(trimmedLeaderboardSearchQuery, { limit: 4 }),
    enabled: isSearchingLeaderboard,
    staleTime: 20_000,
  });
  const togglePeekTarget = (target: PositionPeekTarget) => {
    setSelectedPeekTarget((currentTarget) =>
      currentTarget?.targetToken === target.targetToken ? null : target
    );
  };
  const displayedRankingEntries = isSearchingLeaderboard
    ? leaderboardSearch.data ?? []
    : rankingEntries;
  const selectedDisplayIndex = displayedRankingEntries.findIndex(
    (entry) =>
      Boolean(selectedPeekTarget?.targetToken) &&
      entry.targetToken === selectedPeekTarget?.targetToken
  );
  const isPeekOpen = Boolean(selectedPeekTarget);
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
          <p className="mt-3 text-base-custom text-main-dark-gray">
            포트폴리오 및 시장 현황
          </p>
        </div>
        <p className="text-xs-custom text-main-dark-gray">
          Last update: {lastUpdatedLabel}
        </p>
      </section>

      <section className="grid grid-cols-3 gap-main-2">
        {summaryCards.map((card) => (
          <SummaryMetricCard key={card.title} card={card} />
        ))}
      </section>

      <section className="rounded-main border border-white/50 bg-white/70 backdrop-blur-md shadow-md overflow-visible transition-all duration-300 hover:shadow-lg">
        <div className="flex items-start justify-between gap-main border-b border-main-light-gray/40 px-main-2 py-6">
          <div>
            <h2 className="text-xl-custom font-semibold text-main-dark-gray">
              코인 시세
            </h2>
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
                      : "text-main-dark-gray hover:bg-main-blue/10 hover:text-main-blue"
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
              <tr className="text-left text-xs-custom text-main-dark-gray">
                <th className="px-main py-5 font-medium">코인</th>
                <th className="px-main py-5 font-medium">가격</th>
                <th className="px-main py-5 font-medium">24h 변동</th>
                <th className="px-main py-5 font-medium">Mark Price</th>
                <th className="px-main py-5 font-medium">Funding Rate</th>
                <th className="px-main py-5 font-medium">Index Price</th>
                <th className="px-main py-5 font-medium">거래대금</th>
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

      <section className="rounded-main border border-white/50 bg-white/70 backdrop-blur-md shadow-md overflow-visible transition-all duration-300 hover:shadow-lg">
        <div className="flex items-start justify-between gap-main border-b border-main-light-gray/40 px-main-2 py-6">
          <div>
            <h2 className="text-xl-custom font-semibold text-main-dark-gray">
              실현 수익률 랭킹
            </h2>
          </div>
          <Link
            href="/portfolio"
            className="rounded-main border border-main-light-gray bg-white/50 px-main py-3 text-sm-custom font-semibold text-main-dark-gray transition-all hover:border-main-blue hover:text-main-blue hover:bg-white"
          >
            포트폴리오 확인
          </Link>
        </div>

        <LeaderboardPeekSearch
          query={leaderboardSearchQuery}
          isError={leaderboardSearch.isError}
          isLoading={leaderboardSearch.isFetching}
          compact={isPeekOpen}
          onQueryChange={(nextQuery) => {
            setLeaderboardSearchQuery(nextQuery);
            setSelectedPeekTarget(null);
          }}
        />

        <div
          className={`transition-all duration-300 ${
            isPeekOpen
              ? "grid grid-cols-[minmax(0,1fr)_360px] items-start gap-4 px-main-2 pb-5"
              : ""
          }`}
        >
          <div
            className={`min-w-0 transition-transform duration-300 ${
              isPeekOpen
                ? "-translate-x-2 overflow-hidden rounded-main border border-main-light-gray/40 bg-white/45 shadow-sm"
                : ""
            }`}
          >
            {leaderboardSearch.isFetching ? (
              <div className="px-main-2 py-8 text-sm-custom text-main-dark-gray">
                검색 중...
              </div>
            ) : displayedRankingEntries.length === 0 ? (
              <div className="px-main-2 py-8 text-sm-custom text-main-dark-gray">
                {isSearchingLeaderboard ? "검색 결과가 없습니다." : "데이터 집계 중"}
              </div>
            ) : (
              <div className="divide-y divide-main-light-gray/30">
                {displayedRankingEntries.map((entry) => (
                  <RankingRow
                    key={`${entry.rank}-${entry.nickname}-${entry.targetToken ?? "no-target"}`}
                    entry={entry}
                    isCompact={isPeekOpen}
                    isSelected={Boolean(
                      selectedPeekTarget?.targetToken &&
                        selectedPeekTarget.targetToken === entry.targetToken
                    )}
                    onSelect={entry.targetToken ? togglePeekTarget : undefined}
                  />
                ))}
              </div>
            )}
          </div>

          <PositionPeekPanel
            target={selectedPeekTarget}
            anchorIndex={Math.max(selectedDisplayIndex, 0)}
            isAuthenticated={isAuthenticated}
            onClose={() => setSelectedPeekTarget(null)}
          />
        </div>
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
          support: "text-main-dark-gray",
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
          <p className="text-xs-custom text-main-dark-gray">{card.title}</p>
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
  const changeClassName = getSignedFinancialTextClassName(market.change24h);
  const fundingClassName = getSignedFinancialTextClassName(market.fundingRate);
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
          <div className="flex h-10 w-10 items-center justify-center rounded-full border border-main-light-gray bg-white shadow-sm">
            <Image
              alt={`${market.assetName} logo`}
              className="h-8 w-8 rounded-full object-contain"
              height={32}
              src={getMarketLogoPath(market.symbol)}
              width={32}
            />
          </div>
          <div>
            <p className="font-semibold text-main-dark-gray">
              {market.symbol.replace("USDT", "")}
            </p>
            <p className="mt-1 text-xs-custom text-main-dark-gray">
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
        {isMarketDataDegraded ? "-" : formatRatioPercent(market.change24h)}
      </td>
      <td className="px-main py-5 text-main-dark-gray/70">
        {isMarketDataDegraded ? "-" : formatUsd(market.markPrice)}
      </td>
      <td className={`px-main py-5 font-semibold ${fundingClassName}`}>
        {isMarketDataDegraded ? "-" : formatRatioPercent(market.fundingRate, 4)}
      </td>
      <td className="px-main py-5 text-main-dark-gray/70">
        {isMarketDataDegraded ? "-" : formatUsd(market.indexPrice)}
      </td>
      <td className="px-main py-5 font-semibold text-main-dark-gray">
        {isMarketDataDegraded ? "-" : formatCompactUsd(market.turnover24hUsdt)}
      </td>
    </tr>
  );
}

function RankingRow({
  entry,
  isCompact,
  isSelected,
  onSelect,
}: {
  entry: MarketRankingEntry;
  isCompact: boolean;
  isSelected: boolean;
  onSelect?: (target: PositionPeekTarget) => void;
}) {
  const presentation = getRankPresentation(entry.rank);
  const content = (
    <>
      <div className="flex items-center gap-3">
        <RankBadge rank={entry.rank} />
        <span className={`text-sm-custom font-black ${presentation.accentText}`}>
          {presentation.rankLabel}
        </span>
      </div>
      <p className="text-sm-custom font-semibold text-main-dark-gray">
        {entry.nickname}
      </p>
      <p className="text-sm-custom font-semibold text-main-dark-gray">
        {formatUsd(entry.walletBalance)}
      </p>
      <p
        className={`text-sm-custom font-bold ${getSignedFinancialTextClassName(entry.profitRate)}`}
      >
        {formatPercent(entry.profitRate * 100)}
      </p>
    </>
  );

  const layoutClassName = isCompact
    ? "grid-cols-[150px_minmax(110px,1fr)_minmax(120px,140px)_110px] gap-3 px-4 py-4"
    : "grid-cols-[170px_1fr_220px_180px] gap-main px-main-2 py-6";
  const className = `grid w-full ${layoutClassName} items-center border-l-4 text-left transition-all duration-200 hover:bg-main-blue/5 ${presentation.rowClass} ${
    isSelected ? `relative z-10 ring-2 ${presentation.selectedClass}` : ""
  }`;

  if (!onSelect || !entry.targetToken) {
    return <div className={className}>{content}</div>;
  }

  return (
    <button
      type="button"
      className={`${className} cursor-pointer focus:outline-none focus:ring-2 focus:ring-main-blue/40`}
      onClick={() =>
        onSelect({
          rank: entry.rank,
          nickname: entry.nickname,
          walletBalance: entry.walletBalance,
          profitRate: entry.profitRate,
          targetToken: entry.targetToken ?? "",
        })
      }
    >
      {content}
    </button>
  );
}

function RankBadge({ rank }: { rank: number }) {
  const iconPath = getMarketRankIconPath(rank);
  const presentation = getRankPresentation(rank);
  if (iconPath) {
    return (
      <span
        className={`relative flex h-12 w-12 shrink-0 items-center justify-center rounded-full ring-2 ${presentation.badgeHalo}`}
      >
        <Image
          src={iconPath}
          alt={`${rank}위`}
          width={44}
          height={44}
          className="h-11 w-11 object-contain drop-shadow-sm"
        />
      </span>
    );
  }

  return (
    <span
      className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-xs-custom font-bold ${presentation.numericBadge}`}
    >
      {rank}
    </span>
  );
}

function LeaderboardPeekSearch({
  query,
  isError,
  isLoading,
  compact,
  onQueryChange,
}: {
  query: string;
  isError: boolean;
  isLoading: boolean;
  compact: boolean;
  onQueryChange: (query: string) => void;
}) {
  return (
    <div className={`border-b border-main-light-gray/40 bg-white/40 ${compact ? "px-4 py-4" : "px-main-2 py-5"}`}>
      <label className="text-xs-custom font-semibold text-main-dark-gray" htmlFor="leaderboard-peek-search">
        랭킹 사용자 검색
      </label>
      <div className="mt-2 flex items-center gap-3 rounded-main border border-main-light-gray bg-white/80 px-4 py-3 shadow-sm focus-within:border-main-blue">
        <Search className="h-4 w-4 text-main-dark-gray/50" aria-hidden="true" />
        <input
          id="leaderboard-peek-search"
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="닉네임으로 전체 랭킹 검색"
          className="w-full bg-transparent text-sm-custom text-main-dark-gray outline-none placeholder:text-main-dark-gray/40"
        />
        {isLoading ? (
          <span className="text-xs-custom font-semibold text-main-dark-gray/45">
            검색 중
          </span>
        ) : null}
      </div>
      {isError ? (
        <p className="mt-3 text-xs-custom text-main-red">검색 결과를 불러오지 못했습니다.</p>
      ) : null}
    </div>
  );
}

function PositionPeekPanel({
  target,
  anchorIndex,
  isAuthenticated,
  onClose,
}: {
  target: PositionPeekTarget | null;
  anchorIndex: number;
  isAuthenticated: boolean;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const latestQuery = useQuery({
    queryKey: ["position-peek-latest", target?.targetToken],
    queryFn: () => getPositionPeekLatest(target?.targetToken ?? ""),
    enabled: Boolean(target?.targetToken) && isAuthenticated,
    retry: (failureCount, error) => {
      if (
        error instanceof FuturesClientApiError &&
        [400, 401, 403].includes(error.status)
      ) {
        return false;
      }

      return failureCount < 2;
    },
  });
  const consumeMutation = useMutation({
    mutationFn: () => consumePositionPeek(target?.targetToken ?? "", crypto.randomUUID()),
    onSuccess: (snapshot) => {
      const targetToken = target?.targetToken;
      if (!targetToken) {
        return;
      }
      queryClient.setQueryData<PositionPeekStatus>(
        ["position-peek-latest", targetToken],
        (previous) => ({
          target: previous?.target ?? {
            rank: target.rank,
            nickname: target.nickname,
            walletBalance: target.walletBalance,
            profitRate: target.profitRate,
            targetToken,
          },
          latestSnapshot: snapshot,
          remainingPeekItemCount:
            snapshot.remainingPeekItemCount ?? getPositionPeekItemCount(previous),
        })
      );
      void queryClient.invalidateQueries({ queryKey: ["position-peek-latest", targetToken] });
    },
  });

  if (!target) {
    return null;
  }

  const status = latestQuery.data ?? null;
  const snapshot = status?.latestSnapshot ?? null;
  const itemCount = getPositionPeekItemCount(status);
  const isStatusLoading = latestQuery.isLoading;
  const isConsuming = consumeMutation.isPending;
  const isLoading = isStatusLoading || isConsuming;
  const rankPresentation =
    typeof target.rank === "number" ? getRankPresentation(target.rank) : null;
  const profitRateLabel =
    typeof target.profitRate === "number"
      ? `수익률 ${formatPercent(target.profitRate * 100)}`
      : "수익률 집계 중";
  const walletBalanceLabel =
    typeof target.walletBalance === "number"
      ? `잔고 ${formatUsd(target.walletBalance)}`
      : "잔고 집계 중";
  const errorMessage = latestQuery.isError || consumeMutation.isError
    ? "엿보기 정보를 불러오지 못했습니다. 보유 수량 또는 대상 선택을 다시 확인해 주세요."
    : null;
  const arrowTop = PEEK_PANEL_ARROW_BASE_PX + anchorIndex * PEEK_PANEL_ROW_HEIGHT_PX;
  const panelStyle = {
    "--peek-arrow-top": `${arrowTop}px`,
  } as CSSProperties;

  return (
    <aside
      className="relative sticky top-24 z-30 min-h-[332px] w-full rounded-main border border-white/60 bg-white/95 p-6 shadow-2xl backdrop-blur-xl before:absolute before:-left-2 before:top-[var(--peek-arrow-top)] before:h-4 before:w-4 before:rotate-45 before:border-b before:border-l before:border-white/60 before:bg-white/95"
      style={panelStyle}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          {typeof target.rank === "number" ? <RankBadge rank={target.rank} /> : null}
          <div>
            <p className={`text-xs-custom font-black ${rankPresentation?.accentText ?? "text-main-blue"}`}>
              {rankPresentation?.rankLabel ?? "등수 집계 중"}
            </p>
            <h3 className="mt-2 text-xl-custom font-bold text-main-dark-gray">
              {target.nickname}님의 포지션
            </h3>
            <p className="mt-1 text-xs-custom text-main-dark-gray/60">
              {profitRateLabel} · {walletBalanceLabel}
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="rounded-full p-2 text-main-dark-gray/60 transition-colors hover:bg-main-light-gray/50 hover:text-main-dark-gray"
          aria-label="포지션 엿보기 닫기"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="mt-5 rounded-main border border-main-light-gray/50 bg-main-light-gray/20 p-4">
        {isStatusLoading ? (
          <p className="text-sm-custom text-main-dark-gray">
            엿보기 상태 확인 중...
          </p>
        ) : null}
        {isConsuming ? (
          <p className="text-sm-custom text-main-dark-gray">처리 중...</p>
        ) : null}
        {errorMessage ? <p className="text-sm-custom text-main-red">{errorMessage}</p> : null}
        {!isAuthenticated ? <LoginRequiredPeekState /> : null}
        {isAuthenticated && !isLoading && !snapshot ? (
          <LockedPeekState
            itemCount={itemCount}
            onUse={() => consumeMutation.mutate()}
            isPending={consumeMutation.isPending}
          />
        ) : null}
        {isAuthenticated && snapshot ? (
          <UnlockedPeekState
            snapshot={snapshot}
            itemCount={itemCount}
            onUseAgain={() => consumeMutation.mutate()}
            isPending={consumeMutation.isPending}
          />
        ) : null}
      </div>
    </aside>
  );
}

function LockedPeekState({
  itemCount,
  onUse,
  isPending,
}: {
  itemCount: number;
  onUse: () => void;
  isPending: boolean;
}) {
  return (
    <div>
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm-custom font-semibold text-main-dark-gray">포지션 엿보기</p>
        <span className="rounded-full bg-white/80 px-3 py-1 text-xs-custom font-bold text-main-blue">
          보유 {itemCount}개
        </span>
      </div>
      <p className="mt-2 text-xs-custom text-main-dark-gray/65">
        이 랭커의 현재 공개 포지션을 확인하려면 엿보기권을 사용하세요.
      </p>
      <button
        type="button"
        disabled={itemCount <= 0 || isPending}
        onClick={onUse}
        className="mt-4 w-full rounded-main bg-main-blue px-4 py-3 text-sm-custom font-bold text-white transition disabled:cursor-not-allowed disabled:bg-main-light-gray disabled:text-main-dark-gray/50"
      >
        {isPending ? "사용 중..." : itemCount <= 0 ? "엿보기권 없음" : "엿보기권 사용"}
      </button>
      {itemCount <= 0 ? (
        <Link href="/shop" className="mt-3 block text-center text-xs-custom font-semibold text-main-blue hover:underline">
          상점에서 엿보기권 구매하기
        </Link>
      ) : null}
    </div>
  );
}

function LoginRequiredPeekState() {
  return (
    <div>
      <p className="text-sm-custom font-semibold text-main-dark-gray">
        로그인이 필요합니다.
      </p>
      <p className="mt-2 text-xs-custom text-main-dark-gray/65">
        로그인 후 포지션 엿보기를 사용할 수 있습니다.
      </p>
      <Link
        href="/login"
        className="mt-4 block w-full rounded-main bg-main-blue px-4 py-3 text-center text-sm-custom font-bold text-white transition hover:bg-main-blue/90"
      >
        로그인하기
      </Link>
    </div>
  );
}

function UnlockedPeekState({
  snapshot,
  itemCount,
  onUseAgain,
  isPending,
}: {
  snapshot: PositionPeekSnapshot;
  itemCount: number;
  onUseAgain: () => void;
  isPending: boolean;
}) {
  const createdAt = getPositionPeekSnapshotCreatedAt(snapshot);
  return (
    <div>
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm-custom font-semibold text-main-dark-gray">공개 포지션</p>
        <p className="text-xs-custom text-main-dark-gray/55">
          {createdAt ? `확인 ${new Date(createdAt).toLocaleString("ko-KR")}` : "확인 시각 없음"}
        </p>
      </div>
      {snapshot.positions.length === 0 ? (
        <EmptyPeekSnapshotState />
      ) : (
        <div className="mt-4 flex flex-col gap-3">
          {snapshot.positions.map((position) => (
            <PeekPositionCard key={`${position.symbol}-${position.positionSide}`} position={position} />
          ))}
        </div>
      )}
      <button
        type="button"
        disabled={itemCount <= 0 || isPending}
        onClick={onUseAgain}
        className="mt-4 w-full rounded-main border border-main-blue px-4 py-3 text-sm-custom font-bold text-main-blue transition hover:bg-main-blue hover:text-white disabled:cursor-not-allowed disabled:border-main-light-gray disabled:text-main-dark-gray/45 disabled:hover:bg-transparent"
      >
        {isPending ? "다시 사용 중..." : `다시 사용 (${itemCount}개 보유)`}
      </button>
    </div>
  );
}

function EmptyPeekSnapshotState() {
  return (
    <div className="mt-4 rounded-main border border-main-blue/20 bg-main-blue/[0.06] p-4">
      <div className="flex items-start gap-3">
        <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-main-blue/12 text-main-blue">
          <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
        </span>
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-sm-custom font-bold text-main-dark-gray">
              포지션 엿보기 완료
            </p>
            <span className="rounded-full bg-white/80 px-2.5 py-1 text-xs-custom font-bold text-main-blue">
              열린 포지션 0개
            </span>
          </div>
          <p className="mt-2 text-xs-custom leading-relaxed text-main-dark-gray/65">
            확인 시각 기준으로 공개할 열린 포지션이 없습니다.
          </p>
        </div>
      </div>
    </div>
  );
}

function PeekPositionCard({ position }: { position: PositionPeekPublicPosition }) {
  return (
    <article className="rounded-main border border-main-light-gray/50 bg-white/85 p-4">
      <div className="flex items-center justify-between">
        <p className="text-sm-custom font-bold text-main-dark-gray">{position.symbol}</p>
        <span className={`rounded-full px-2 py-1 text-xs-custom font-bold ${position.positionSide === "LONG" ? "bg-[#21a453]/10 text-[#16a34a]" : "bg-main-red/10 text-main-red"}`}>
          {position.positionSide} · {position.leverage}x
        </span>
      </div>
      <div className="mt-3 grid grid-cols-2 gap-3 text-xs-custom">
        <PeekMetric label="수량" value={position.positionSize.toLocaleString("ko-KR")} />
        <PeekMetric
          label="진입가"
          value={position.entryPrice == null ? "-" : formatUsd(position.entryPrice)}
        />
        <PeekMetric label="미실현 PnL" value={formatUsd(position.unrealizedPnl)} tone={position.unrealizedPnl} />
        <PeekMetric label="ROI" value={formatPercent(position.roi * 100)} tone={position.roi} />
      </div>
    </article>
  );
}

function PeekMetric({ label, value, tone }: { label: string; value: string; tone?: number }) {
  return (
    <div>
      <p className="text-main-dark-gray/50">{label}</p>
      <p className={`mt-1 font-bold ${typeof tone === "number" ? getSignedFinancialTextClassName(tone) : "text-main-dark-gray"}`}>
        {value}
      </p>
    </div>
  );
}
