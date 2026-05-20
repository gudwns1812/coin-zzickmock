"use client";

import { getFuturesAuthUserClient } from "@/lib/futures-auth-state";
import { futuresQueryKeys } from "@/lib/futures-query-keys";
import { useQuery } from "@tanstack/react-query";

export const FUTURES_AUTH_USER_STALE_TIME_MS = 30_000;

export function useFuturesAuthUser() {
  return useQuery({
    queryKey: futuresQueryKeys.authMe,
    queryFn: getFuturesAuthUserClient,
    retry: false,
    staleTime: FUTURES_AUTH_USER_STALE_TIME_MS,
  });
}
