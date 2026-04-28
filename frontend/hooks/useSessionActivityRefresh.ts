"use client";

import { useCallback, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import {
  SESSION_ACTIVITY_REFRESH_EXTENSION_SECONDS,
  shouldRefreshSessionOnActivity,
} from "@/lib/session-activity-refresh";

const ACTIVITY_EVENTS = [
  "pointerdown",
  "keydown",
  "wheel",
  "touchstart",
] as const;

function useSessionActivityRefresh(expiresAt?: number) {
  const router = useRouter();
  const expiresAtRef = useRef(expiresAt);
  const lastAttemptedAtMsRef = useRef(0);
  const refreshInFlightRef = useRef(false);

  useEffect(() => {
    expiresAtRef.current = expiresAt;
  }, [expiresAt]);

  const refreshOnActivity = useCallback(async () => {
    const nowMs = Date.now();

    if (
      refreshInFlightRef.current ||
      !shouldRefreshSessionOnActivity({
        expiresAt: expiresAtRef.current,
        lastAttemptedAtMs: lastAttemptedAtMsRef.current,
        nowMs,
      })
    ) {
      return;
    }

    refreshInFlightRef.current = true;
    lastAttemptedAtMsRef.current = nowMs;

    try {
      const response = await fetch("/proxy/auth/refresh", {
        cache: "no-store",
        credentials: "include",
      });

      if (response.ok) {
        expiresAtRef.current =
          Math.floor(Date.now() / 1000) + SESSION_ACTIVITY_REFRESH_EXTENSION_SECONDS;
        return;
      }

      if (expiresAtRef.current && expiresAtRef.current <= Math.floor(Date.now() / 1000)) {
        router.refresh();
      }
    } catch {
      if (expiresAtRef.current && expiresAtRef.current <= Math.floor(Date.now() / 1000)) {
        router.refresh();
      }
    } finally {
      refreshInFlightRef.current = false;
    }
  }, [router]);

  useEffect(() => {
    const handleActivity = () => {
      void refreshOnActivity();
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void refreshOnActivity();
      }
    };

    ACTIVITY_EVENTS.forEach((eventName) => {
      window.addEventListener(eventName, handleActivity, { passive: true });
    });
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      ACTIVITY_EVENTS.forEach((eventName) => {
        window.removeEventListener(eventName, handleActivity);
      });
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [refreshOnActivity]);
}

export default useSessionActivityRefresh;
