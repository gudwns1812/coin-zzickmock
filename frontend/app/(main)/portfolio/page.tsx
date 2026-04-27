import ClosePositionButton from "@/components/futures/ClosePositionButton";
import {
  getFuturesAccountSummary,
  getFuturesPositions,
  getFuturesReward,
} from "@/lib/futures-api";
import { formatUsd } from "@/lib/markets";
import { getJwtToken } from "@/utils/auth";
import Link from "next/link";

export default async function PortfolioPage() {
  const token = await getJwtToken();
  const [account, positions, reward] = await Promise.all([
    getFuturesAccountSummary(),
    getFuturesPositions(),
    getFuturesReward(),
  ]);
  const unrealizedPnl = positions.reduce(
    (sum, position) => sum + position.unrealizedPnl,
    0
  );
  const totalEquity = account.walletBalance + unrealizedPnl;

  return (
    <div className="px-main-2 pb-24 flex flex-col gap-8 pt-4">
      <section className="grid grid-cols-[1.4fr_1fr] gap-main-2">
        <div className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray">
          <p className="text-sm-custom text-main-dark-gray/60">Portfolio</p>
          <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
            선물 계정 대시보드
          </h1>
          <p className="mt-3 text-sm-custom text-main-dark-gray/70 break-keep">
            기존 주식 포트폴리오 화면 대신, 총 자산과 증거금, 열린 포지션,
            손익, 포인트를 한 화면에서 읽는 코인 선물 계정 화면으로 전환하는
            중입니다.
          </p>
        </div>

        <div className="rounded-main bg-gradient-to-br from-main-blue to-cyan-500 text-white p-main-2 shadow-sm">
          <p className="text-sm-custom text-white/70">현재 사용자</p>
          <h2 className="mt-3 text-2xl-custom font-bold">
            {token ? token.memberName : account.memberName}
          </h2>
          <p className="mt-3 text-sm-custom text-white/80">
            회원가입 직후 `100000 USDT` 데모 자산이 제공되고, 실현 손익에 따라
            포인트가 누적됩니다.
          </p>
        </div>
      </section>

      <section className="grid grid-cols-4 gap-main-2">
        <PortfolioMetric label="총 평가 자산" value={formatUsd(totalEquity)} />
        <PortfolioMetric
          label="사용 가능 잔고"
          value={formatUsd(account.available)}
        />
        <PortfolioMetric label="미실현 손익" value={formatUsd(unrealizedPnl)} />
        <PortfolioMetric label="포인트" value={`${reward.rewardPoint} P`} />
      </section>

      <section className="grid grid-cols-[1.3fr_1fr] gap-main-2">
        <div className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray min-h-[280px]">
          <h2 className="text-xl-custom font-semibold text-main-dark-gray">
            열린 포지션
          </h2>
          {positions.length === 0 ? (
            <div className="mt-6 rounded-main bg-main-light-gray/40 p-main-2 text-sm-custom text-main-dark-gray/70">
              아직 열린 포지션이 없습니다. 마켓 상세에서 주문을 실행하면 여기에
              실시간으로 반영됩니다.
            </div>
          ) : (
            <div className="mt-6 flex flex-col gap-4">
              {positions.map((position) => (
                <article
                  key={`${position.symbol}-${position.positionSide}-${position.marginMode}`}
                  className="rounded-main border border-main-light-gray px-main py-4"
                >
                  <div className="flex items-start justify-between gap-main">
                    <div>
                      <p className="text-xs-custom uppercase tracking-[0.2em] text-main-dark-gray/50">
                        {position.symbol}
                      </p>
                      <h3 className="mt-2 text-lg-custom font-semibold text-main-dark-gray">
                        {position.positionSide} · {position.marginMode}
                      </h3>
                    </div>
                    <ClosePositionButton
                      symbol={position.symbol}
                      positionSide={position.positionSide}
                      marginMode={position.marginMode}
                      quantity={position.quantity}
                      markPrice={position.markPrice}
                    />
                  </div>

                  <div className="mt-4 grid grid-cols-4 gap-main">
                    <PortfolioMetric
                      label="수량"
                      value={position.quantity.toFixed(3)}
                    />
                    <PortfolioMetric
                      label="레버리지"
                      value={`${position.leverage}x`}
                    />
                    <PortfolioMetric
                      label="진입가"
                      value={formatUsd(position.entryPrice)}
                    />
                    <PortfolioMetric
                      label="미실현 손익"
                      value={formatUsd(position.unrealizedPnl)}
                    />
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>

        <div className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray min-h-[280px] flex flex-col gap-4">
          <h2 className="text-xl-custom font-semibold text-main-dark-gray">
            다음 액션
          </h2>
          <ActionLink href="/markets/BTCUSDT" label="BTCUSDT 주문 화면 보기" />
          <ActionLink href="/markets/ETHUSDT" label="ETHUSDT 주문 화면 보기" />
          <ActionLink href="/shop" label="포인트 상점 보기" />
        </div>
      </section>
    </div>
  );
}

function PortfolioMetric({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray">
      <p className="text-xs-custom text-main-dark-gray/60">{label}</p>
      <p className="mt-2 text-xl-custom font-semibold text-main-dark-gray">
        {value}
      </p>
    </div>
  );
}

function ActionLink({ href, label }: { href: string; label: string }) {
  return (
    <Link
      href={href}
      className="rounded-main border border-main-light-gray px-main py-3 text-sm-custom font-semibold text-main-dark-gray hover:border-main-blue hover:text-main-blue transition-colors"
    >
      {label}
    </Link>
  );
}
