"use client";

import clsx from "clsx";
import Link from "next/link";
import { usePathname } from "next/navigation";

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

  if (!pathname || pathname === "/") {
    return null;
  }

  return (
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
          >
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
