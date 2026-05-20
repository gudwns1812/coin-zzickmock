"use client";

import { logFrontendPageTiming } from "@/lib/frontend-performance-log";
import { usePathname } from "next/navigation";
import { useEffect, useRef } from "react";

export default function FrontendPerformanceLogger() {
  const pathname = usePathname();
  const previousPathnameRef = useRef<string | null>(null);
  const navigationIntentAtRef = useRef<number | null>(null);

  useEffect(() => {
    const recordNavigationIntent = (event: MouseEvent) => {
      if (event.defaultPrevented || event.button !== 0) {
        return;
      }

      if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
        return;
      }

      const target = event.target;
      if (!(target instanceof Element)) {
        return;
      }

      const anchor = target.closest("a[href]");
      if (!(anchor instanceof HTMLAnchorElement)) {
        return;
      }

      const nextUrl = new URL(anchor.href, window.location.href);
      if (
        nextUrl.origin !== window.location.origin ||
        nextUrl.pathname === window.location.pathname
      ) {
        return;
      }

      navigationIntentAtRef.current = now();
    };

    window.addEventListener("click", recordNavigationIntent, true);

    return () => {
      window.removeEventListener("click", recordNavigationIntent, true);
    };
  }, []);

  useEffect(() => {
    const route = pathname || "/";
    const completedAt = now();
    const previousPathname = previousPathnameRef.current;
    const navigationIntentAt = navigationIntentAtRef.current;

    previousPathnameRef.current = route;

    window.requestAnimationFrame(() => {
      if (previousPathname === null) {
        logInitialPageTiming(route);
        return;
      }

      if (previousPathname !== route) {
        const startedAt = navigationIntentAt ?? completedAt;
        logFrontendPageTiming({
          route,
          source: "route_change",
          durationMs: now() - startedAt,
          navigationIntentGapMs:
            navigationIntentAt === null ? null : completedAt - navigationIntentAt,
        });
        navigationIntentAtRef.current = null;
      }
    });
  }, [pathname]);

  return null;
}

function logInitialPageTiming(route: string) {
  const navigation = performance.getEntriesByType("navigation")[0];

  if (navigation instanceof PerformanceNavigationTiming) {
    const completedAt = navigation.loadEventEnd > 0 ? navigation.loadEventEnd : now();
    const responseDownloadMs = Math.max(
      0,
      navigation.responseEnd - navigation.responseStart
    );

    logFrontendPageTiming({
      route,
      source: "initial_load",
      durationMs: completedAt,
      ttfbMs: navigation.responseStart,
      requestStartMs: navigation.requestStart,
      responseStartMs: navigation.responseStart,
      responseEndMs: navigation.responseEnd,
      responseDownloadMs,
      browserRenderMs: Math.max(0, completedAt - navigation.responseEnd),
      domInteractiveMs: navigation.domInteractive,
      domContentLoadedMs: navigation.domContentLoadedEventEnd,
      loadEventMs: navigation.loadEventEnd > 0 ? navigation.loadEventEnd : null,
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
