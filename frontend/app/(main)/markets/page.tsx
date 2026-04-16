import { getJwtToken } from "@/utils/auth";
import {
  formatPercent,
  formatUsd,
} from "@/lib/markets";
import {
  getFuturesAccountSummary,
  getFuturesMarkets,
} from "@/lib/futures-api";
import Link from "next/link";

export default async function MarketsPage() {
  const token = await getJwtToken();
  const [account, markets] = await Promise.all([
    getFuturesAccountSummary(),
    getFuturesMarkets(),
  ]);

  return (
    <div className="px-main-2 pb-24 flex flex-col gap-8">
      <section className="grid grid-cols-[1.3fr_1fr] gap-main-2 pt-4">
        <div className="rounded-main bg-gradient-to-br from-main-blue to-sky-500 text-white p-main-2 shadow-sm">
          <p className="text-sm-custom uppercase tracking-[0.2em] text-white/70">
            Coin Futures Mock
          </p>
          <h1 className="mt-3 text-3xl-custom font-bold">
            Bitget 기반 코인 선물 체험 마켓
          </h1>
          <p className="mt-3 text-sm-custom max-w-[560px] text-white/80 break-keep">
            BTCUSDT와 ETHUSDT 두 심볼로 레버리지, 마진 모드, 청산가를
            부담 없이 연습하는 데스크톱 우선 MVP 기반 화면입니다.
          </p>
          <div className="mt-6 flex gap-main">
            <Link
              href="/markets/BTCUSDT"
              className="rounded-main bg-white text-main-blue px-main py-2 font-semibold"
            >
              BTCUSDT 보기
            </Link>
            <Link
              href="/portfolio"
              className="rounded-main border border-white/40 px-main py-2 font-semibold text-white"
            >
              내 계정 보기
            </Link>
          </div>
        </div>

        <div className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray">
          <p className="text-sm-custom text-main-dark-gray/60">계정 요약</p>
          <h2 className="mt-2 text-2xl-custom font-bold text-main-dark-gray">
            {token
              ? `${token.memberName}님의 선물 계정`
              : `${account.memberName}님의 데모 계정`}
          </h2>
          <div className="mt-6 grid grid-cols-2 gap-main">
            <SummaryCard label="지갑 잔고" value={formatUsd(account.walletBalance)} />
            <SummaryCard
              label="사용 가능 증거금"
              value={formatUsd(account.availableMargin)}
            />
            <SummaryCard label="최대 레버리지" value="50x" />
            <SummaryCard label="보상 포인트" value={`${account.rewardPoint} P`} />
          </div>
        </div>
      </section>

      <section className="grid grid-cols-2 gap-main-2">
        {markets.map((market) => (
          <article
            key={market.symbol}
            className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray flex flex-col gap-5"
          >
            <div className="flex items-start justify-between gap-main">
              <div>
                <p className="text-xs-custom uppercase tracking-[0.2em] text-main-dark-gray/50">
                  {market.symbol}
                </p>
                <h3 className="mt-2 text-2xl-custom font-bold text-main-dark-gray">
                  {market.displayName}
                </h3>
                <p className="mt-2 text-sm-custom text-main-dark-gray/70 break-keep">
                  {market.description}
                </p>
              </div>
              <span className="rounded-full bg-main-light-gray px-3 py-1 text-xs-custom text-main-dark-gray/70">
                {market.openInterestLabel}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-main">
              <MetricCard label="최신 체결가" value={formatUsd(market.lastPrice)} />
              <MetricCard label="24h 변화율" value={formatPercent(market.change24h)} />
              <MetricCard label="Mark Price" value={formatUsd(market.markPrice)} />
              <MetricCard label="Funding" value={formatPercent(market.fundingRate * 100)} />
            </div>

            <div className="flex items-center justify-between gap-main border-t border-main-light-gray pt-5">
              <div className="text-sm-custom text-main-dark-gray/60">
                24h 거래량 {formatUsd(market.volume24h)}
              </div>
              <Link
                href={`/markets/${market.symbol}`}
                className="rounded-main bg-main-blue px-main py-2 text-white font-semibold"
              >
                상세 보기
              </Link>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-main bg-main-light-gray/40 px-main py-3">
      <p className="text-xs-custom text-main-dark-gray/60">{label}</p>
      <p className="mt-2 text-lg-custom font-semibold text-main-dark-gray">
        {value}
      </p>
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-main border border-main-light-gray px-main py-3">
      <p className="text-xs-custom text-main-dark-gray/60">{label}</p>
      <p className="mt-2 text-lg-custom font-semibold text-main-dark-gray">
        {value}
      </p>
    </div>
  );
}
