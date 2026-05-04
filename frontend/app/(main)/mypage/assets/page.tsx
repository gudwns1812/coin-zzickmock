import {
  getFuturesAccountSummary,
  getFuturesPositions,
  getFuturesWalletHistory,
} from "@/lib/futures-api";
import WalletBalanceChart from "@/components/mypage/WalletBalanceChart";
import {
  buildDailyWalletChanges,
  buildKstMonthCalendar,
  toKstDateKey,
} from "@/lib/futures-pnl-calendar";
import { formatUsd } from "@/lib/markets";
import { Eye, TrendingDown, TrendingUp } from "lucide-react";

const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

export default async function MyPageAssetsPage() {
  const [account, positions, walletHistory] = await Promise.all([
    getFuturesAccountSummary(),
    getFuturesPositions(),
    getFuturesWalletHistory(),
  ]);
  const unrealizedPnl = positions.reduce(
    (sum, position) => sum + position.unrealizedPnl,
    0
  );
  const totalBalance = account.walletBalance + unrealizedPnl;
  const dailyWalletChanges = buildDailyWalletChanges(walletHistory);
  const calendarDays = buildKstMonthCalendar(new Date(), dailyWalletChanges);
  const todayKey = toKstDateKey(new Date());
  const monthLabel = todayKey.slice(0, 7);
  const monthPnl = dailyWalletChanges
    .filter((item) => item.dateKey.startsWith(monthLabel))
    .reduce((sum, item) => sum + item.dailyWalletChange, 0);

  return (
    <div className="flex flex-col gap-main-2">
      <section>
        <h1 className="text-2xl-custom font-bold text-main-dark-gray">
          Assets
        </h1>
        <div className="mt-main rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
          <div className="grid grid-cols-1 gap-main-2 xl:grid-cols-[minmax(0,0.92fr)_minmax(360px,1fr)]">
            <div className="flex min-w-0 flex-col justify-between">
              <div>
                <div className="flex items-center gap-2 text-sm-custom font-semibold text-main-dark-gray/55">
                <span>Total balance</span>
                <Eye size={16} />
                </div>
                <div className="mt-4 flex items-end gap-2">
                  <strong className="text-4xl-custom font-bold text-main-dark-gray">
                    {formatUsd(totalBalance).replace("$", "")}
                  </strong>
                  <span className="mb-1 text-base-custom font-bold text-main-dark-gray/70">
                    USDT
                  </span>
                </div>
                <p className="mt-3 text-sm-custom text-main-dark-gray/55">
                  Wallet {formatUsd(account.walletBalance)}
                </p>
              </div>

              <div className="mt-main-2 grid grid-cols-2 gap-main border-t border-main-light-gray/60 pt-main">
                <BalanceMetric
                  label="Available"
                  value={formatUsd(account.available)}
                />
                <BalanceMetric
                  label="Unrealized PnL"
                  tone={unrealizedPnl >= 0 ? "positive" : "negative"}
                  value={formatSignedUsd(unrealizedPnl)}
                />
              </div>
            </div>

            <WalletBalanceChart history={walletHistory} />
          </div>
        </div>
      </section>

      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-start justify-between gap-main-2">
          <div>
            <p className="text-sm-custom text-main-dark-gray/55">
              Realized PnL Calendar
            </p>
            <h2 className="mt-2 text-2xl-custom font-bold text-main-dark-gray">
              {monthLabel}
            </h2>
          </div>
          <div className="rounded-main bg-main-light-gray/45 px-main py-2 text-sm-custom font-semibold text-main-dark-gray">
            월간 {formatSignedUsd(monthPnl)}
          </div>
        </div>

        <div className="mt-main grid grid-cols-7 gap-2">
          {WEEKDAYS.map((weekday) => (
            <div
              className="px-2 text-xs-custom font-semibold text-main-dark-gray/45"
              key={weekday}
            >
              {weekday}
            </div>
          ))}

          {calendarDays.map((day) => (
            <div
              className={[
                "min-h-[78px] rounded-main border p-2",
                day.inMonth
                  ? "border-main-light-gray bg-white"
                  : "border-transparent bg-main-light-gray/20 text-main-dark-gray/35",
                day.dateKey === todayKey ? "ring-1 ring-main-blue" : "",
              ].join(" ")}
              key={day.dateKey}
            >
              <div className="flex items-center justify-between">
                <span className="text-xs-custom font-semibold">
                  {day.dayOfMonth}
                </span>
                {day.dailyWalletChange > 0 && (
                  <TrendingUp size={14} className="text-main-red" />
                )}
                {day.dailyWalletChange < 0 && (
                  <TrendingDown size={14} className="text-main-blue" />
                )}
              </div>
              <p
                className={[
                  "mt-3 text-sm-custom font-bold",
                  day.dailyWalletChange > 0
                    ? "text-main-red"
                    : day.dailyWalletChange < 0
                      ? "text-main-blue"
                      : "text-main-dark-gray/35",
                ].join(" ")}
              >
                {day.dailyWalletChange === 0
                  ? "-"
                  : formatSignedUsd(day.dailyWalletChange)}
                
              </p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function BalanceMetric({
  label,
  value,
  tone = "neutral",
}: {
  label: string;
  value: string;
  tone?: "neutral" | "positive" | "negative";
}) {
  const toneClassName =
    tone === "positive"
      ? "text-main-red"
      : tone === "negative"
        ? "text-main-blue"
        : "text-main-dark-gray";

  return (
    <div>
      <p className="text-xs-custom font-semibold uppercase tracking-normal text-main-dark-gray/45">
        {label}
      </p>
      <p className={`mt-2 text-lg-custom font-bold ${toneClassName}`}>
        {value}
      </p>
    </div>
  );
}

function formatSignedUsd(value: number): string {
  if (value === 0) {
    return "$0.00";
  }

  const sign = value > 0 ? "+" : "-";
  return `${sign}${formatUsd(Math.abs(value))}`;
}
