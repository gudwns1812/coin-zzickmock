"use client";

import { logFrontendPageTiming } from "@/lib/frontend-performance-log";
import { usePathname } from "next/navigation";
import { useEffect, useRef } from "react";

export default function FrontendPerformanceLogger() {
  const pathname = usePathname();
  const previousPathnameRef = useRef<string | null>(null);
  const previousCompletedAtRef = useRef<number | null>(null);

  useEffect(() => {
    const route = pathname || "/";
    const completedAt = now();
    const previousPathname = previousPathnameRef.current;
    const previousCompletedAt = previousCompletedAtRef.current;

    previousPathnameRef.current = route;
    previousCompletedAtRef.current = completedAt;

    window.requestAnimationFrame(() => {
      if (previousPathname === null) {
        logInitialPageTiming(route);
        return;
      }

      if (previousPathname !== route && previousCompletedAt !== null) {
        logFrontendPageTiming({
          route,
          source: "route_change",
          durationMs: now() - previousCompletedAt,
        });
      }
    });
  }, [pathname]);

  return null;
}

function logInitialPageTiming(route: string) {
  const navigation = performance.getEntriesByType("navigation")[0];

  if (navigation instanceof PerformanceNavigationTiming) {
    logFrontendPageTiming({
      route,
      source: "initial_load",
      durationMs: navigation.loadEventEnd > 0 ? navigation.loadEventEnd : now(),
      ttfbMs: navigation.responseStart,
    });
    return;
  }

  logFrontendPageTiming({
    route,
    source: "initial_load",
    durationMs: now(),
  });
}

function now() {
  return performance.now();
}
