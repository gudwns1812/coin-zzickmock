import type { FuturesWalletHistoryPoint } from "@/lib/futures-api";

const KST_DATE_LABEL_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  month: "2-digit",
  day: "2-digit",
});

export type WalletBalanceChartPoint = {
  label: string;
  walletBalance: number;
  availableMargin: number;
  recordedAt: string;
};

export function buildWalletBalanceChartPoints(
  history: FuturesWalletHistoryPoint[]
): WalletBalanceChartPoint[] {
  return [...history]
    .filter((point) => Number.isFinite(point.walletBalance))
    .sort(
      (left, right) =>
        new Date(left.recordedAt).getTime() - new Date(right.recordedAt).getTime()
    )
    .map((point) => ({
      label: KST_DATE_LABEL_FORMATTER.format(new Date(point.recordedAt)),
      walletBalance: point.walletBalance,
      availableMargin: point.availableMargin,
      recordedAt: point.recordedAt,
    }));
}
