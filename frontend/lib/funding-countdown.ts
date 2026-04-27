export function formatFundingCountdown(
  nextFundingAt: string | null,
  nowMs = Date.now(),
  serverTime: string | null = null,
  serverTimeReceivedAtMs = nowMs
): string {
  if (!nextFundingAt) {
    return "--:--:--";
  }

  const nextFundingMs = new Date(nextFundingAt).getTime();
  if (Number.isNaN(nextFundingMs)) {
    return "--:--:--";
  }

  const effectiveNowMs = resolveEffectiveNowMs(nowMs, serverTime, serverTimeReceivedAtMs);
  const totalSeconds = Math.max(0, Math.floor((nextFundingMs - effectiveNowMs) / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  return [hours, minutes, seconds]
    .map((value) => value.toString().padStart(2, "0"))
    .join(":");
}

function resolveEffectiveNowMs(
  nowMs: number,
  serverTime: string | null,
  serverTimeReceivedAtMs: number
): number {
  if (!serverTime) {
    return nowMs;
  }

  const serverTimeMs = new Date(serverTime).getTime();
  if (Number.isNaN(serverTimeMs)) {
    return nowMs;
  }

  return serverTimeMs + Math.max(0, nowMs - serverTimeReceivedAtMs);
}
