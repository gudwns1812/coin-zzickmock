import { formatPercent, formatUsd, type MarketSnapshot } from "@/lib/markets";
import Link from "next/link";

const PRIMARY_ACTIONS = [
  {
    title: "트레이드하러 가기",
    description: "주문 화면으로 바로 이동해 진입가, 레버리지, 마진 모드를 연습합니다.",
    href: "/markets/BTCUSDT",
    accentClassName:
      "from-main-blue to-cyan-400 text-white border-transparent shadow-color",
    label: "BTCUSDT 주문 열기",
  },
  {
    title: "관심 심볼 모아보기",
    description: "두 심볼의 변화율과 펀딩비를 한 번에 비교하면서 흐름을 읽습니다.",
    href: "/watchlist",
    accentClassName:
      "from-white to-white text-main-dark-gray border-main-light-gray",
    label: "Watchlist 보기",
  },
  {
    title: "포인트 상점 둘러보기",
    description: "실현 손익으로 모은 포인트를 배지와 테마 아이템으로 바꿉니다.",
    href: "/shop",
    accentClassName:
      "from-[#f3f7ff] to-[#eef4ff] text-main-dark-gray border-[#cfe0ff]",
    label: "상점 이동",
  },
] as const;

type MarketsLandingProps = {
  markets: [MarketSnapshot, MarketSnapshot];
};

export default function MarketsLanding({ markets }: MarketsLandingProps) {
  const [btcMarket, ethMarket] = markets;

  return (
    <div className="px-main-2 pb-24 pt-4 flex flex-col gap-8">
      <section className="grid grid-cols-[1.45fr_0.95fr] gap-main-2">
        <div className="rounded-main bg-[radial-gradient(circle_at_top_left,_rgba(255,255,255,0.18),_transparent_32%),linear-gradient(135deg,_#1d4fd7_0%,_#3485fa_55%,_#7d6cff_100%)] p-main-2 text-white shadow-color">
          <div className="flex items-center gap-3">
            <span className="rounded-full border border-white/20 bg-white/12 px-3 py-1 text-xs-custom uppercase tracking-[0.24em] text-white/80">
              Coin Futures Mock
            </span>
            <span className="rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs-custom text-white/75">
              Desktop Market Overview
            </span>
          </div>

          <h1 className="mt-6 max-w-[620px] text-4xl-custom font-bold leading-[1.15]">
            BTCUSDT와 ETHUSDT 흐름을 한 화면에서 보고 바로 트레이드로
            들어가는 메인 보드
          </h1>
          <p className="mt-4 max-w-[620px] text-sm-custom leading-6 text-white/82 break-keep">
            지금 움직이는 두 심볼만 앞에 두었습니다. 가격, 24시간 변화율,
            펀딩비, 거래량을 빠르게 훑고 원하는 주문 화면으로 바로 이동할 수
            있습니다.
          </p>

          <div className="mt-8 grid grid-cols-2 gap-main">
            <HeroSymbolStrip market={btcMarket} />
            <HeroSymbolStrip market={ethMarket} />
          </div>

          <div className="mt-8 flex items-center gap-main">
            <Link
              href="/markets/BTCUSDT"
              className="rounded-main bg-white px-main py-3 text-sm-custom font-semibold text-main-blue transition-transform duration-200 hover:-translate-y-0.5"
            >
              BTCUSDT 트레이드하러 가기
            </Link>
            <Link
              href="/markets/ETHUSDT"
              className="rounded-main border border-white/30 bg-white/8 px-main py-3 text-sm-custom font-semibold text-white transition-colors hover:bg-white/14"
            >
              ETHUSDT 열기
            </Link>
          </div>
        </div>

        <div className="grid grid-rows-[1fr_auto] gap-main-2">
          <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
            <p className="text-sm-custom text-main-dark-gray/55">오늘의 포커스</p>
            <h2 className="mt-2 text-2xl-custom font-bold text-main-dark-gray">
              빠르게 읽고 바로 행동하는 메인 화면
            </h2>
            <p className="mt-3 text-sm-custom leading-6 text-main-dark-gray/72 break-keep">
              빈 화면 대신 지금 필요한 진입점만 남겼습니다. 상승폭이 큰 심볼,
              비교용 심볼, 다음 액션을 같은 시선 흐름 안에서 볼 수 있습니다.
            </p>

            <div className="mt-6 grid grid-cols-2 gap-main">
              <SpotlightMetric
                label="가장 큰 움직임"
                value={formatPercent(btcMarket.change24h)}
                tone="rise"
              />
              <SpotlightMetric
                label="비교용 심볼"
                value={ethMarket.symbol}
                tone="calm"
              />
              <SpotlightMetric
                label="24h 거래량"
                value={formatUsd(btcMarket.volume24h)}
                tone="calm"
              />
              <SpotlightMetric
                label="평균 펀딩비"
                value={formatPercent(
                  ((btcMarket.fundingRate + ethMarket.fundingRate) / 2) * 100
                )}
                tone="rise"
              />
            </div>
          </section>

          <section className="rounded-main border border-[#d7e5ff] bg-[linear-gradient(180deg,_rgba(52,133,250,0.08),_rgba(255,255,255,0.96))] p-main-2 shadow-sm">
            <p className="text-xs-custom uppercase tracking-[0.22em] text-main-blue/70">
              Quick Actions
            </p>
            <div className="mt-4 flex flex-col gap-3">
              {PRIMARY_ACTIONS.map((action) => (
                <ActionTile key={action.href} {...action} />
              ))}
            </div>
          </section>
        </div>
      </section>

      <section className="grid grid-cols-2 gap-main-2">
        <MarketSummaryCard market={btcMarket} tone="rise" />
        <MarketSummaryCard market={ethMarket} tone="cool" />
      </section>

      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-start justify-between gap-main">
          <div>
            <p className="text-sm-custom text-main-dark-gray/55">바로 이동</p>
            <h2 className="mt-2 text-2xl-custom font-bold text-main-dark-gray">
              비어 있는 자리 대신 다음 행동을 남겼습니다
            </h2>
            <p className="mt-3 max-w-[760px] text-sm-custom leading-6 text-main-dark-gray/72 break-keep">
              메인 화면은 이제 계정 현황을 설명하는 곳이 아니라, 두 심볼을
              읽고 주문 화면으로 넘어가는 진입점입니다. 보고 싶은 흐름에 맞춰
              바로 이동하세요.
            </p>
          </div>
          <Link
            href="/portfolio"
            className="rounded-main border border-main-light-gray px-main py-3 text-sm-custom font-semibold text-main-dark-gray transition-colors hover:border-main-blue hover:text-main-blue"
          >
            포트폴리오 확인
          </Link>
        </div>

        <div className="mt-6 grid grid-cols-3 gap-main">
          <ShortcutCard
            eyebrow="Trade"
            title="BTCUSDT 주문 화면"
            description="가장 빠르게 진입 연습을 시작할 수 있는 메인 주문 탭입니다."
            href="/markets/BTCUSDT"
            label="주문 열기"
          />
          <ShortcutCard
            eyebrow="Compare"
            title="ETHUSDT 비교 보기"
            description="BTC와 다른 흐름을 비교하며 마진 모드와 변동성을 읽습니다."
            href="/markets/ETHUSDT"
            label="ETH 열기"
          />
          <ShortcutCard
            eyebrow="Browse"
            title="관심 심볼 요약"
            description="두 심볼을 간단한 리스트 형태로 다시 훑고 싶을 때 이동합니다."
            href="/watchlist"
            label="목록 보기"
          />
        </div>
      </section>
    </div>
  );
}

function HeroSymbolStrip({ market }: { market: MarketSnapshot }) {
  const changeClassName =
    market.change24h >= 0 ? "text-[#ffd7dc]" : "text-[#d7ebff]";

  return (
    <div className="rounded-main border border-white/15 bg-white/10 px-main py-4 backdrop-blur-[2px]">
      <div className="flex items-start justify-between gap-main">
        <div>
          <p className="text-xs-custom uppercase tracking-[0.22em] text-white/68">
            {market.symbol}
          </p>
          <h2 className="mt-2 text-xl-custom font-semibold text-white">
            {formatUsd(market.lastPrice)}
          </h2>
        </div>
        <span
          className={`rounded-full border border-white/14 bg-white/10 px-3 py-1 text-xs-custom font-semibold ${changeClassName}`}
        >
          24h {formatPercent(market.change24h)}
        </span>
      </div>

      <div className="mt-4 grid grid-cols-3 gap-3 text-xs-custom text-white/74">
        <HeroMetric label="Mark" value={formatUsd(market.markPrice)} />
        <HeroMetric
          label="Funding"
          value={formatPercent(market.fundingRate * 100)}
        />
        <HeroMetric label="Volume" value={formatUsd(market.volume24h)} />
      </div>
    </div>
  );
}

function HeroMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-main bg-white/8 px-3 py-2">
      <p className="text-[10px] uppercase tracking-[0.18em] text-white/55">{label}</p>
      <p className="mt-2 text-sm-custom font-medium text-white">{value}</p>
    </div>
  );
}

function SpotlightMetric({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: "rise" | "calm";
}) {
  const toneClassName =
    tone === "rise"
      ? "bg-[#fff5f6] text-main-red border-[#ffd9de]"
      : "bg-[#f5f9ff] text-main-blue border-[#d7e5ff]";

  return (
    <div className={`rounded-main border px-main py-3 ${toneClassName}`}>
      <p className="text-xs-custom">{label}</p>
      <p className="mt-2 text-lg-custom font-semibold">{value}</p>
    </div>
  );
}

function MarketSummaryCard({
  market,
  tone,
}: {
  market: MarketSnapshot;
  tone: "rise" | "cool";
}) {
  const toneClassName =
    tone === "rise"
      ? {
          panel: "border-[#ffdce2] bg-[linear-gradient(180deg,_rgba(240,66,81,0.08),_rgba(255,255,255,1))]",
          badge: "bg-main-red/10 text-main-red",
          button: "bg-main-red hover:bg-main-red/90",
          accent: "text-main-red",
        }
      : {
          panel: "border-[#dce7ff] bg-[linear-gradient(180deg,_rgba(52,133,250,0.08),_rgba(255,255,255,1))]",
          badge: "bg-main-blue/10 text-main-blue",
          button: "bg-main-blue hover:bg-main-blue/90",
          accent: "text-main-blue",
        };

  return (
    <article className={`rounded-main border p-main-2 shadow-sm ${toneClassName.panel}`}>
      <div className="flex items-start justify-between gap-main">
        <div>
          <p className="text-xs-custom uppercase tracking-[0.22em] text-main-dark-gray/50">
            {market.symbol}
          </p>
          <h2 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
            {market.displayName}
          </h2>
          <p className="mt-3 max-w-[520px] text-sm-custom leading-6 text-main-dark-gray/72 break-keep">
            {market.description}
          </p>
        </div>
        <span className={`rounded-full px-3 py-1 text-xs-custom font-semibold ${toneClassName.badge}`}>
          {market.openInterestLabel}
        </span>
      </div>

      <div className="mt-8 grid grid-cols-4 gap-main">
        <MarketMetric label="최신 체결가" value={formatUsd(market.lastPrice)} />
        <MarketMetric
          label="24h 변화율"
          value={formatPercent(market.change24h)}
          valueClassName={toneClassName.accent}
        />
        <MarketMetric label="Mark Price" value={formatUsd(market.markPrice)} />
        <MarketMetric
          label="Funding"
          value={formatPercent(market.fundingRate * 100)}
        />
      </div>

      <div className="mt-6 flex items-center justify-between gap-main border-t border-main-light-gray/80 pt-5">
        <div className="flex flex-wrap gap-main text-sm-custom text-main-dark-gray/62">
          <span>Index {formatUsd(market.indexPrice)}</span>
          <span>24h 거래량 {formatUsd(market.volume24h)}</span>
        </div>
        <Link
          href={`/markets/${market.symbol}`}
          className={`rounded-main px-main py-3 text-sm-custom font-semibold text-white transition-colors ${toneClassName.button}`}
        >
          {market.symbol} 상세 보기
        </Link>
      </div>
    </article>
  );
}

function MarketMetric({
  label,
  value,
  valueClassName,
}: {
  label: string;
  value: string;
  valueClassName?: string;
}) {
  return (
    <div className="rounded-main border border-main-light-gray/85 bg-white/75 px-main py-3">
      <p className="text-xs-custom text-main-dark-gray/58">{label}</p>
      <p
        className={`mt-2 text-lg-custom font-semibold text-main-dark-gray ${
          valueClassName ?? ""
        }`}
      >
        {value}
      </p>
    </div>
  );
}

function ActionTile({
  title,
  description,
  href,
  accentClassName,
  label,
}: {
  title: string;
  description: string;
  href: string;
  accentClassName: string;
  label: string;
}) {
  return (
    <Link
      href={href}
      className={`rounded-main border bg-gradient-to-r px-main py-4 transition-transform duration-200 hover:-translate-y-0.5 ${accentClassName}`}
    >
      <p className="text-sm-custom font-semibold">{title}</p>
      <p className="mt-2 text-sm-custom leading-6 opacity-80">{description}</p>
      <p className="mt-4 text-xs-custom uppercase tracking-[0.2em] opacity-75">
        {label}
      </p>
    </Link>
  );
}

function ShortcutCard({
  eyebrow,
  title,
  description,
  href,
  label,
}: {
  eyebrow: string;
  title: string;
  description: string;
  href: string;
  label: string;
}) {
  return (
    <Link
      href={href}
      className="rounded-main border border-main-light-gray bg-[linear-gradient(180deg,_rgba(245,249,255,0.7),_rgba(255,255,255,1))] px-main py-4 transition-colors hover:border-main-blue/40"
    >
      <p className="text-xs-custom uppercase tracking-[0.2em] text-main-blue/70">
        {eyebrow}
      </p>
      <h3 className="mt-3 text-lg-custom font-semibold text-main-dark-gray">
        {title}
      </h3>
      <p className="mt-3 text-sm-custom leading-6 text-main-dark-gray/70 break-keep">
        {description}
      </p>
      <p className="mt-5 text-sm-custom font-semibold text-main-blue">{label}</p>
    </Link>
  );
}
