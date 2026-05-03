import type { FuturesWalletHistoryPoint } from "@/lib/futures-api";

const KST_DATE_LABEL_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  month: "2-digit",
  day: "2-digit",
});

export type WalletBalanceChartPoint = {
  label: string;
  walletBalance: number;
  recordedAt: string;
};

export function buildWalletBalanceChartPoints(
  history: FuturesWalletHistoryPoint[]
): WalletBalanceChartPoint[] {
  return [...history]
    .filter((point) => Number.isFinite(point.walletBalance))
    .sort((left, right) => left.snapshotDate.localeCompare(right.snapshotDate))
    .map((point) => ({
      label: formatSnapshotDate(point.snapshotDate),
      walletBalance: point.walletBalance,
      recordedAt: point.recordedAt,
    }));
}

function formatSnapshotDate(snapshotDate: string): string {
  return KST_DATE_LABEL_FORMATTER.format(new Date(`${snapshotDate}T00:00:00+09:00`));
}
