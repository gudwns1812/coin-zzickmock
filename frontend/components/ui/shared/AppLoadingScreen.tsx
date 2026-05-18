type AppLoadingScreenProps = {
  title?: string;
  description?: string;
};

const STATUS_STEPS = ["인증 확인", "시세 동기화", "포지션 계산"];
const SUMMARY_CARDS = [
  { label: "총 자산", widthClassName: "w-32" },
  { label: "실현 수익", widthClassName: "w-24" },
  { label: "오늘 수익", widthClassName: "w-28" },
];
const MARKET_ROWS = [
  { symbol: "BTC", toneClassName: "bg-emerald-500/70" },
  { symbol: "ETH", toneClassName: "bg-main-red/70" },
  { symbol: "USDT", toneClassName: "bg-main-blue/70" },
];
const CHART_BARS = [58, 78, 44, 92, 66, 112, 84, 126, 74, 98, 138, 104];

export default function AppLoadingScreen({
  title = "데이터를 불러오는 중입니다",
  description = "시장 정보와 계정 상태를 확인하고 있습니다.",
}: AppLoadingScreenProps) {
  return (
    <div
      className="flex min-h-[calc(100vh-78px)] w-full items-center justify-center bg-[linear-gradient(180deg,_rgba(52,133,250,0.055),_rgba(255,255,255,0)_42%)] px-main-3 py-main-6"
      role="status"
      aria-live="polite"
      aria-busy="true"
    >
      <section className="relative w-full max-w-[1040px] overflow-hidden rounded-main border border-white/70 bg-white shadow-[0_24px_70px_rgba(15,23,42,0.12)]">
        <div className="coin-loading-scan absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-transparent via-main-blue to-transparent" />

        <div className="flex items-start justify-between gap-main-3 border-b border-main-light-gray/60 bg-[linear-gradient(135deg,_rgba(52,133,250,0.08),_rgba(255,255,255,0)_62%)] px-main-3 py-main-3">
          <div className="min-w-0">
            <div className="mb-main flex items-center gap-2">
              <span className="flex items-center gap-2 rounded-full border border-main-blue/20 bg-main-blue/10 px-3 py-1 text-xs-custom font-bold uppercase tracking-normal text-main-blue">
                <span className="size-2 animate-pulse rounded-full bg-main-blue" />
                Live sync
              </span>
              <span className="text-xs-custom font-semibold text-main-dark-gray/45">
                Coin futures workstation
              </span>
            </div>
            <h2 className="text-2xl-custom font-bold text-main-dark-gray">
              {title}
            </h2>
            <p className="mt-2 max-w-[560px] text-sm-custom leading-6 text-main-dark-gray/60">
              {description}
            </p>
          </div>

          <div className="grid min-w-[330px] gap-2">
            {STATUS_STEPS.map((step, index) => (
              <div
                className="flex items-center justify-between rounded-main border border-main-light-gray/70 bg-white/80 px-main py-2 shadow-sm"
                key={step}
              >
                <span className="text-xs-custom font-semibold text-main-dark-gray/55">
                  {step}
                </span>
                <span
                  className={[
                    "h-2 rounded-full bg-main-blue/70 coin-loading-shimmer",
                    index === 0 ? "w-16" : index === 1 ? "w-24" : "w-20",
                  ].join(" ")}
                />
              </div>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-[minmax(0,1fr)_350px] gap-main-2 p-main-3">
          <div className="grid gap-main-2">
            <div className="grid grid-cols-3 gap-main">
              {SUMMARY_CARDS.map((card) => (
                <div
                  className="rounded-main border border-main-light-gray/70 bg-[linear-gradient(180deg,_rgba(52,133,250,0.045),_rgba(255,255,255,0))] p-main-2"
                  key={card.label}
                >
                  <div className="flex items-center gap-2">
                    <span className="grid size-8 place-items-center rounded-main bg-main-blue/10">
                      <span className="size-3 rounded-sm bg-main-blue/60" />
                    </span>
                    <span className="text-xs-custom font-semibold text-main-dark-gray/55">
                      {card.label}
                    </span>
                  </div>
                  <div
                    className={`mt-main-2 h-6 rounded-full bg-main-light-gray/75 coin-loading-shimmer ${card.widthClassName}`}
                  />
                  <div className="mt-main h-3 w-20 rounded-full bg-main-light-gray/55 coin-loading-shimmer" />
                </div>
              ))}
            </div>

            <div className="overflow-hidden rounded-main border border-main-light-gray/70 bg-white">
              <div className="grid grid-cols-[1.1fr_1fr_1fr_1fr] gap-main border-b border-main-light-gray/60 bg-main-blue/[0.025] px-main-2 py-main">
                <div className="h-3 w-16 rounded-full bg-main-dark-gray/15" />
                <div className="h-3 w-14 rounded-full bg-main-dark-gray/15" />
                <div className="h-3 w-20 rounded-full bg-main-dark-gray/15" />
                <div className="h-3 w-16 rounded-full bg-main-dark-gray/15" />
              </div>

              <div className="divide-y divide-main-light-gray/55">
                {MARKET_ROWS.map((row) => (
                  <div
                    className="grid grid-cols-[1.1fr_1fr_1fr_1fr] items-center gap-main px-main-2 py-main-2"
                    key={row.symbol}
                  >
                    <div className="flex items-center gap-main">
                      <span
                        className={`size-9 rounded-full shadow-sm ${row.toneClassName}`}
                      />
                      <div>
                        <p className="text-sm-custom font-bold text-main-dark-gray">
                          {row.symbol}
                        </p>
                        <div className="mt-1 h-2 w-16 rounded-full bg-main-light-gray/70 coin-loading-shimmer" />
                      </div>
                    </div>
                    <div className="h-4 w-24 rounded-full bg-main-light-gray/75 coin-loading-shimmer" />
                    <div className="h-4 w-16 rounded-full bg-main-light-gray/65 coin-loading-shimmer" />
                    <div className="h-4 w-20 rounded-full bg-main-light-gray/65 coin-loading-shimmer" />
                  </div>
                ))}
              </div>
            </div>
          </div>

          <aside className="grid gap-main-2">
            <div className="rounded-main border border-main-light-gray/70 bg-main-dark-gray p-main-2 text-white shadow-md">
              <div className="flex items-center justify-between gap-main">
                <div>
                  <p className="text-sm-custom font-bold">시장 스냅샷</p>
                  <p className="mt-1 text-xs-custom text-white/55">
                    실시간 캔들 준비 중
                  </p>
                </div>
                <span className="rounded-full bg-white/10 px-3 py-1 text-xs-custom font-bold text-white/75">
                  SSE
                </span>
              </div>

              <div className="mt-main-3 flex h-[150px] items-end gap-2 rounded-main border border-white/10 bg-white/[0.04] px-main py-main">
                {CHART_BARS.map((height, index) => (
                  <span
                    className={[
                      "flex-1 rounded-t-full coin-loading-float",
                      index % 4 === 1
                        ? "bg-emerald-400/80"
                        : index % 4 === 2
                          ? "bg-main-red/75"
                          : "bg-main-blue/80",
                    ].join(" ")}
                    key={`${height}-${index}`}
                    style={{
                      height,
                      animationDelay: `${index * 70}ms`,
                    }}
                  />
                ))}
              </div>
            </div>

            <div className="rounded-main border border-main-light-gray/70 bg-white p-main-2">
              <div className="flex items-center justify-between gap-main">
                <p className="text-sm-custom font-bold text-main-dark-gray">
                  랭킹 계산
                </p>
                <div className="h-2 w-16 rounded-full bg-main-blue/55 coin-loading-shimmer" />
              </div>
              <div className="mt-main-2 grid gap-main">
                {[1, 2, 3].map((rank) => (
                  <div
                    className="flex items-center gap-main rounded-main bg-main-light-gray/25 px-main py-2"
                    key={rank}
                  >
                    <span className="grid size-7 place-items-center rounded-full bg-white text-xs-custom font-bold text-main-dark-gray/50 shadow-sm">
                      {rank}
                    </span>
                    <div className="min-w-0 grow">
                      <div className="h-3 w-24 rounded-full bg-main-light-gray/80 coin-loading-shimmer" />
                      <div className="mt-2 h-2 w-16 rounded-full bg-main-light-gray/60 coin-loading-shimmer" />
                    </div>
                    <span className="h-4 w-12 rounded-full bg-emerald-500/25 coin-loading-shimmer" />
                  </div>
                ))}
              </div>
            </div>
          </aside>
        </div>
      </section>
    </div>
  );
}
