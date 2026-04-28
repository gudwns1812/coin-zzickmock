import {
  getFuturesAccountSummary,
  getFuturesPositionHistory,
  getFuturesPositions,
} from "@/lib/futures-api";
import {
  buildKstMonthCalendar,
  groupDailyNetRealizedPnl,
  toKstDateKey,
} from "@/lib/futures-pnl-calendar";
import { formatUsd } from "@/lib/markets";
import { ChevronDown, Eye, TrendingDown, TrendingUp } from "lucide-react";

const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

export default async function MyPageAssetsPage() {
  const [account, positions, histories] = await Promise.all([
    getFuturesAccountSummary(),
    getFuturesPositions(),
    getFuturesPositionHistory(),
  ]);
  const unrealizedPnl = positions.reduce(
    (sum, position) => sum + position.unrealizedPnl,
    0
  );
  const totalBalance = account.walletBalance + unrealizedPnl;
  const dailyPnl = groupDailyNetRealizedPnl(histories);
  const calendarDays = buildKstMonthCalendar(new Date(), dailyPnl);
  const todayKey = toKstDateKey(new Date());
  const monthLabel = todayKey.slice(0, 7);
  const monthPnl = dailyPnl
    .filter((item) => item.dateKey.startsWith(monthLabel))
    .reduce((sum, item) => sum + item.netRealizedPnl, 0);

  return (
    <div className="flex flex-col gap-main-2">
      <section>
        <h1 className="text-2xl-custom font-bold text-main-dark-gray">
          Assets
        </h1>
        <div className="mt-main rounded-main border border-[#30323a] bg-[#111216] p-main-2 text-white shadow-sm">
          <div className="flex items-start justify-between gap-main-2">
            <div>
              <div className="flex items-center gap-2 text-sm-custom font-semibold text-white/55">
                <span>Total balance</span>
                <Eye size={16} />
              </div>
              <div className="mt-4 flex items-end gap-2">
                <strong className="text-4xl-custom font-bold">
                  {formatUsd(totalBalance).replace("$", "")}
                </strong>
                <span className="mb-1 text-base-custom font-bold text-white/85">
                  USDT
                </span>
              </div>
              <p className="mt-3 text-sm-custom text-white/50">
                Available {formatUsd(account.available)}
              </p>
            </div>

            <div className="flex min-w-[420px] flex-col items-end gap-main">
              <span
                aria-hidden="true"
                className="flex size-[28px] items-center justify-center rounded-main bg-white/10 text-white/65"
              >
                <ChevronDown size={18} />
              </span>
              <AssetSparkline value={unrealizedPnl} />
            </div>
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
                {day.netRealizedPnl > 0 && (
                  <TrendingUp size={14} className="text-main-red" />
                )}
                {day.netRealizedPnl < 0 && (
                  <TrendingDown size={14} className="text-main-blue" />
                )}
              </div>
              <p
                className={[
                  "mt-3 text-sm-custom font-bold",
                  day.netRealizedPnl > 0
                    ? "text-main-red"
                    : day.netRealizedPnl < 0
                      ? "text-main-blue"
                      : "text-main-dark-gray/35",
                ].join(" ")}
              >
                {day.netRealizedPnl === 0
                  ? "-"
                  : formatSignedUsd(day.netRealizedPnl)}
              </p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function AssetSparkline({ value }: { value: number }) {
  const positive = value >= 0;

  return (
    <div className="relative h-[96px] w-[420px] overflow-hidden">
      <div className="absolute left-0 top-1/2 h-px w-full bg-white/10" />
      <div
        className={[
          "absolute left-[48px] right-0 h-[2px] rounded-full",
          positive ? "top-[36px] bg-main-red" : "top-[58px] bg-main-blue",
        ].join(" ")}
      />
      <div
        className={[
          "absolute left-[48px] top-[36px] h-[28px] w-[88px] rounded-t-main opacity-20",
          positive ? "bg-main-red" : "bg-main-blue",
        ].join(" ")}
      />
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
