import {
  getFuturesReward,
  getRewardPointHistory,
  type RewardPointHistoryType,
} from "@/lib/futures-api";
import { Coins, MinusCircle, PlusCircle, RotateCcw } from "lucide-react";
import Link from "next/link";

export default async function MyPagePointsPage() {
  const [reward, histories] = await Promise.all([
    getFuturesReward(),
    getRewardPointHistory(),
  ]);

  return (
    <div className="flex flex-col gap-main-2">
      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-start justify-between gap-main-2">
          <div>
            <p className="text-sm-custom text-main-dark-gray/55">Point Wallet</p>
            <h1 className="mt-2 text-4xl-custom font-bold text-main-dark-gray">
              {reward.rewardPoint.toLocaleString("ko-KR")} P
            </h1>
            <p className="mt-3 text-sm-custom text-main-dark-gray/65 break-keep">
              실현 손익으로 적립된 포인트와 교환권 신청/환불 내역을 확인합니다.
            </p>
          </div>
          <Link
            className="rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white"
            href="/shop"
          >
            상점으로 이동
          </Link>
        </div>
      </section>

      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-center justify-between">
          <h2 className="text-xl-custom font-bold text-main-dark-gray">
            Point History
          </h2>
          <span className="text-sm-custom text-main-dark-gray/50">
            {histories.length.toLocaleString("ko-KR")} rows
          </span>
        </div>

        <div className="mt-main flex flex-col divide-y divide-main-light-gray">
          {histories.length === 0 ? (
            <div className="rounded-main bg-main-light-gray/35 p-main-2 text-sm-custom text-main-dark-gray/60">
              아직 포인트 기록이 없습니다.
            </div>
          ) : (
            histories.map((history, index) => (
              <div
                className="grid grid-cols-[1fr_auto_auto] items-center gap-main py-main"
                key={`${history.historyType}-${history.sourceReference}-${index}`}
              >
                <div className="flex items-center gap-main">
                  <HistoryIcon type={history.historyType} />
                  <div>
                    <p className="font-semibold text-main-dark-gray">
                      {historyLabel(history.historyType)}
                    </p>
                    <p className="mt-1 text-xs-custom text-main-dark-gray/45">
                      {history.sourceType} · {history.sourceReference}
                    </p>
                  </div>
                </div>
                <span
                  className={[
                    "text-sm-custom font-bold",
                    history.amount >= 0 ? "text-main-red" : "text-main-blue",
                  ].join(" ")}
                >
                  {formatPointDelta(history.amount)}
                </span>
                <span className="text-sm-custom font-semibold text-main-dark-gray/60">
                  {history.balanceAfter.toLocaleString("ko-KR")} P
                </span>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

function HistoryIcon({ type }: { type: RewardPointHistoryType }) {
  const className =
    "flex size-[36px] items-center justify-center rounded-main bg-main-light-gray/45";

  if (type === "GRANT") {
    return (
      <span className={`${className} text-main-red`}>
        <PlusCircle size={18} />
      </span>
    );
  }

  if (type === "REDEMPTION_REFUND") {
    return (
      <span className={`${className} text-main-blue`}>
        <RotateCcw size={18} />
      </span>
    );
  }

  return (
    <span className={`${className} text-main-dark-gray/55`}>
      <MinusCircle size={18} />
    </span>
  );
}

function historyLabel(type: RewardPointHistoryType): string {
  if (type === "GRANT") return "포인트 적립";
  if (type === "REDEMPTION_REFUND") return "교환권 환불";
  return "교환권 신청";
}

function formatPointDelta(value: number): string {
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toLocaleString("ko-KR")} P`;
}
