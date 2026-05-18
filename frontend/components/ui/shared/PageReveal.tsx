"use client";

import { cn } from "@/lib/utils";
import {
  motion,
  useReducedMotion,
  type TargetAndTransition,
} from "motion/react";
import { usePathname } from "next/navigation";
import type { CSSProperties, ReactNode } from "react";

type PageRevealVariant =
  | "default"
  | "markets"
  | "trading"
  | "mypage"
  | "watchlist"
  | "community"
  | "shop"
  | "admin"
  | "auth";

type PageRevealPreset = {
  initial: TargetAndTransition;
  animate: TargetAndTransition;
  transition: TargetAndTransition["transition"];
};

type PageRevealProps = {
  children: ReactNode;
  className?: string;
  variant?: PageRevealVariant;
};

const easeOutExpressive = [0.16, 1, 0.3, 1] as const;
const easeOutQuick = [0.2, 0.8, 0.2, 1] as const;

const REVEAL_PRESETS: Record<PageRevealVariant, PageRevealPreset> = {
  default: {
    initial: { opacity: 0, y: 18, scale: 0.99, filter: "blur(4px)" },
    animate: { opacity: 1, y: 0, scale: 1, filter: "blur(0px)" },
    transition: { duration: 0.34, ease: easeOutExpressive },
  },
  markets: {
    initial: { opacity: 0, y: 24, scale: 0.986, filter: "blur(5px)" },
    animate: { opacity: 1, y: 0, scale: 1, filter: "blur(0px)" },
    transition: { duration: 0.4, ease: easeOutExpressive },
  },
  trading: {
    initial: { opacity: 0, x: 10, scale: 0.997 },
    animate: { opacity: 1, x: 0, scale: 1 },
    transition: { duration: 0.18, ease: easeOutQuick },
  },
  mypage: {
    initial: { opacity: 0, y: 22, scale: 0.986, filter: "blur(4px)" },
    animate: { opacity: 1, y: 0, scale: 1, filter: "blur(0px)" },
    transition: { duration: 0.38, ease: easeOutExpressive },
  },
  watchlist: {
    initial: { opacity: 0, y: 24, scale: 0.984, filter: "blur(5px)" },
    animate: { opacity: 1, y: 0, scale: 1, filter: "blur(0px)" },
    transition: { duration: 0.4, ease: easeOutExpressive },
  },
  community: {
    initial: { opacity: 0, y: 18, scale: 0.99, filter: "blur(4px)" },
    animate: { opacity: 1, y: 0, scale: 1, filter: "blur(0px)" },
    transition: { duration: 0.36, ease: easeOutExpressive },
  },
  shop: {
    initial: { opacity: 0, y: 28, scale: 0.978, filter: "blur(6px)" },
    animate: { opacity: 1, y: 0, scale: 1, filter: "blur(0px)" },
    transition: { duration: 0.44, ease: easeOutExpressive },
  },
  admin: {
    initial: { opacity: 0, y: 12, filter: "blur(3px)" },
    animate: { opacity: 1, y: 0, filter: "blur(0px)" },
    transition: { duration: 0.3, ease: easeOutQuick },
  },
  auth: {
    initial: { opacity: 0, y: 28, scale: 0.976, filter: "blur(6px)" },
    animate: { opacity: 1, y: 0, scale: 1, filter: "blur(0px)" },
    transition: { duration: 0.44, ease: easeOutExpressive },
  },
};

function resolveRevealVariant(pathname: string | null): PageRevealVariant {
  if (!pathname) return "default";
  if (/^\/markets\/[^/]+/.test(pathname)) return "trading";
  if (pathname === "/markets" || pathname.startsWith("/markets/")) {
    return "markets";
  }
  if (
    pathname === "/mypage" ||
    pathname.startsWith("/mypage/") ||
    pathname === "/portfolio"
  ) {
    return "mypage";
  }
  if (pathname === "/watchlist") return "watchlist";
  if (pathname === "/community" || pathname.startsWith("/community/")) {
    return "community";
  }
  if (pathname === "/shop") return "shop";
  if (pathname === "/admin" || pathname.startsWith("/admin/")) return "admin";
  if (pathname === "/login" || pathname === "/signup") return "auth";
  return "default";
}

export default function PageReveal({
  children,
  className,
  variant,
}: PageRevealProps) {
  const pathname = usePathname();
  const shouldReduceMotion = useReducedMotion();
  const resolvedVariant = variant ?? resolveRevealVariant(pathname);
  const preset = REVEAL_PRESETS[resolvedVariant];
  const style: CSSProperties | undefined = shouldReduceMotion
    ? undefined
    : { willChange: "opacity, transform, filter" };

  return (
    <motion.div
      key={`${pathname ?? resolvedVariant}:${resolvedVariant}`}
      animate={shouldReduceMotion ? { opacity: 1 } : preset.animate}
      className={cn("motion-safe:transform-gpu", className)}
      data-page-reveal={resolvedVariant}
      initial={shouldReduceMotion ? false : preset.initial}
      style={style}
      transition={shouldReduceMotion ? { duration: 0 } : preset.transition}
    >
      {children}
    </motion.div>
  );
}
