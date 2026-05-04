import type { FuturesWalletHistoryPoint } from "@/lib/futures-api";

export type DailyWalletChange = {
  dateKey: string;
  dailyWalletChange: number;
};

export type CalendarDay = {
  dateKey: string;
  dayOfMonth: number;
  inMonth: boolean;
  dailyWalletChange: number;
};

const KST_TIME_ZONE = "Asia/Seoul";

export function toKstDateKey(value: string | Date): string {
  const date = typeof value === "string" ? new Date(value) : value;
  const parts = new Intl.DateTimeFormat("en-US", {
    day: "2-digit",
    month: "2-digit",
    timeZone: KST_TIME_ZONE,
    year: "numeric",
  }).formatToParts(date);

  const year = getDatePart(parts, "year");
  const month = getDatePart(parts, "month");
  const day = getDatePart(parts, "day");

  return `${year}-${month}-${day}`;
}

export function buildDailyWalletChanges(
  history: Pick<
    FuturesWalletHistoryPoint,
    "snapshotDate" | "dailyWalletChange"
  >[]
): DailyWalletChange[] {
  return history
    .map((point) => ({
      dateKey: point.snapshotDate,
      dailyWalletChange: point.dailyWalletChange,
    }))
    .sort((left, right) => left.dateKey.localeCompare(right.dateKey));
}

export function buildKstMonthCalendar(
  monthDate: Date,
  dailyChanges: DailyWalletChange[]
): CalendarDay[] {
  const monthKey = toKstDateKey(monthDate).slice(0, 7);
  const [year, month] = monthKey.split("-").map(Number);
  const firstDay = new Date(Date.UTC(year, month - 1, 1));
  const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate();
  const leadingDays = firstDay.getUTCDay();
  const changeByDate = new Map(
    dailyChanges.map((item) => [item.dateKey, item])
  );
  const cells: CalendarDay[] = [];

  for (let index = 0; index < leadingDays; index += 1) {
    const day = new Date(Date.UTC(year, month - 1, index - leadingDays + 1));
    const dateKey = toUtcDateKey(day);
    const change = changeByDate.get(dateKey);
    cells.push({
      dateKey,
      dayOfMonth: day.getUTCDate(),
      inMonth: false,
      dailyWalletChange: change?.dailyWalletChange ?? 0,
    });
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    const dateKey = `${year}-${pad(month)}-${pad(day)}`;
    const change = changeByDate.get(dateKey);
    cells.push({
      dateKey,
      dayOfMonth: day,
      inMonth: true,
      dailyWalletChange: change?.dailyWalletChange ?? 0,
    });
  }

  while (cells.length % 7 !== 0) {
    const next = new Date(Date.UTC(year, month - 1, cells.length - leadingDays + 1));
    const dateKey = toUtcDateKey(next);
    const change = changeByDate.get(dateKey);
    cells.push({
      dateKey,
      dayOfMonth: next.getUTCDate(),
      inMonth: false,
      dailyWalletChange: change?.dailyWalletChange ?? 0,
    });
  }

  return cells;
}

function getDatePart(
  parts: Intl.DateTimeFormatPart[],
  type: Intl.DateTimeFormatPartTypes
): string {
  return parts.find((part) => part.type === type)?.value ?? "00";
}

function toUtcDateKey(date: Date): string {
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(
    date.getUTCDate()
  )}`;
}

function pad(value: number): string {
  return value.toString().padStart(2, "0");
}
