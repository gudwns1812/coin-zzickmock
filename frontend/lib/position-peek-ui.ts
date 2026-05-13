import type {
  PositionPeekSnapshot,
  PositionPeekStatus,
} from "@/lib/futures-api";

const FORBIDDEN_POSITION_PEEK_KEYS = new Set([
  "memberId",
  "targetMemberId",
  "viewerMemberId",
  "takeProfitPrice",
  "stopLossPrice",
  "orderId",
  "historyId",
  "closeableQuantity",
  "pendingCloseQuantity",
]);

export function getPositionPeekItemCount(
  status: Pick<
    PositionPeekStatus,
    "peekItemCount" | "remainingPeekItemCount" | "itemCount"
  > | null | undefined
): number {
  if (!status) {
    return 0;
  }

  return Math.max(
    0,
    status.remainingPeekItemCount ?? status.peekItemCount ?? status.itemCount ?? 0
  );
}

export function getPositionPeekSnapshotCreatedAt(
  snapshot: Pick<PositionPeekSnapshot, "createdAt" | "snapshotCreatedAt"> | null
): string | null {
  return snapshot?.snapshotCreatedAt ?? snapshot?.createdAt ?? null;
}

export function hasForbiddenPositionPeekKey(value: unknown): boolean {
  if (!value || typeof value !== "object") {
    return false;
  }

  if (Array.isArray(value)) {
    return value.some((item) => hasForbiddenPositionPeekKey(item));
  }

  return Object.entries(value as Record<string, unknown>).some(([key, child]) => {
    if (FORBIDDEN_POSITION_PEEK_KEYS.has(key)) {
      return true;
    }

    return hasForbiddenPositionPeekKey(child);
  });
}

export function formatPeekLeverage(leverage: number): string {
  return `${leverage}x`;
}
