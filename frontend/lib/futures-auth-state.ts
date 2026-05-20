"use client";

import type { AuthUser } from "@/lib/futures-api";
import { fetchFuturesBackendApi } from "@/lib/futures-api-request";

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export const FUTURES_AUTH_CHANGED_EVENT = "futures-auth-changed";

export type FuturesAuthChangeAction =
  | "login"
  | "logout"
  | "withdraw"
  | "refresh";

export type FuturesAuthChangedEvent = CustomEvent<{
  action: FuturesAuthChangeAction;
}>;

export function notifyFuturesAuthChanged(
  action: FuturesAuthChangeAction = "refresh"
) {
  window.dispatchEvent(
    new CustomEvent(FUTURES_AUTH_CHANGED_EVENT, { detail: { action } })
  );
}

export async function getFuturesAuthUserClient(): Promise<AuthUser | null> {
  const response = await fetchFuturesBackendApi("/auth/me", {
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
