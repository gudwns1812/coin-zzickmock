"use client";

import { createFuturesBackendApiUrl } from "@/lib/futures-sse-url";
import type { AuthUser, FuturesLeaderboard } from "@/lib/futures-api";

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export const FUTURES_AUTH_CHANGED_EVENT = "futures-auth-changed";

export function notifyFuturesAuthChanged() {
  window.dispatchEvent(new Event(FUTURES_AUTH_CHANGED_EVENT));
}

export async function getFuturesAuthUserClient(): Promise<AuthUser | null> {
  const response = await fetch(createFuturesBackendApiUrl("/auth/me"), {
    cache: "no-store",
    credentials: "include",
  });

  const payload = (await response.json().catch(() => null)) as
    | ClientApiResponse<AuthUser>
    | null;

  if (!response.ok || !payload?.success || !payload.data) {
    return null;
  }

  return payload.data;
}

export async function getFuturesLeaderboardClient(): Promise<FuturesLeaderboard | null> {
  const response = await fetch(createFuturesBackendApiUrl("/leaderboard"), {
    cache: "no-store",
    credentials: "include",
  });

  const payload = (await response.json().catch(() => null)) as
    | ClientApiResponse<FuturesLeaderboard>
    | null;

  if (!response.ok || !payload?.success || !payload.data) {
    return null;
  }

  return payload.data;
}
