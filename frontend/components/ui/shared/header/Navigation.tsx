"use client";

import MarketDetailNavigationPreview from "@/components/futures/MarketDetailNavigationPreview";
import clsx from "clsx";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { type MouseEvent, useEffect, useState } from "react";
import { flushSync } from "react-dom";

const NAV_ITEMS = [
  { href: "/markets", label: "마켓" },
  { href: "/mypage", label: "계정" },
  { href: "/markets/BTCUSDT", label: "트레이딩" },
  { href: "/community", label: "커뮤니티" },
  { href: "/shop", label: "상점" },
];

function isActiveNavItem(pathname: string, href: string) {
  if (href === "/markets") {
    return pathname === "/markets";
  }

  if (href === "/markets/BTCUSDT") {
    return pathname.startsWith("/markets/");
  }

  return pathname === href || pathname.startsWith(`${href}/`);
}

export default function Navigation() {
  const pathname = usePathname();
  const router = useRouter();
  const [showTradingPreview, setShowTradingPreview] = useState(false);

  useEffect(() => {
    router.prefetch("/markets/BTCUSDT");
  }, [router]);

  useEffect(() => {
    if (pathname?.startsWith("/markets/")) {
      setShowTradingPreview(false);
    }
  }, [pathname]);

  useEffect(() => {
    if (!showTradingPreview) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      setShowTradingPreview(false);
    }, 8_000);

    return () => window.clearTimeout(timeoutId);
  }, [showTradingPreview]);

  if (!pathname || pathname === "/") {
    return null;
  }

  const handleNavClick = (
    event: MouseEvent<HTMLAnchorElement>,
    href: string
  ) => {
    if (
      href !== "/markets/BTCUSDT" ||
      pathname.startsWith("/markets/") ||
      event.defaultPrevented ||
      event.button !== 0 ||
      event.metaKey ||
      event.ctrlKey ||
      event.shiftKey ||
      event.altKey
    ) {
      return;
    }

    event.preventDefault();
    flushSync(() => {
      setShowTradingPreview(true);
    });
    window.requestAnimationFrame(() => {
      router.push(href);
    });
  };

  return (
    <>
      <nav className="flex items-center gap-6 absolute left-1/2 -translate-x-1/2 top-1/2 -translate-y-1/2 min-w-[500px] justify-center">
        {NAV_ITEMS.map((item) => {
          const isActive = isActiveNavItem(pathname, item.href);

          return (
            <Link
              key={item.href}
              href={item.href}
              className={clsx(
                "relative px-2 py-2 text-sm-custom transition-colors after:absolute after:left-1/2 after:top-full after:h-[2px] after:w-0 after:-translate-x-1/2 after:rounded-full after:bg-main-blue after:transition-all",
                isActive
                  ? "text-main-blue font-semibold after:w-full"
                  : "text-sub hover:text-main-dark-gray hover:after:w-1/2 hover:after:bg-main-light-gray"
              )}
              aria-current={isActive ? "page" : undefined}
              onClick={(event) => handleNavClick(event, item.href)}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>
      {showTradingPreview ? <MarketDetailNavigationPreview /> : null}
    </>
  );
}
