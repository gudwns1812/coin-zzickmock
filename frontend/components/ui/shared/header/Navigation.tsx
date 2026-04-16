"use client";

import clsx from "clsx";
import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV_ITEMS = [
  { href: "/markets", label: "마켓" },
  { href: "/portfolio", label: "계정" },
  { href: "/watchlist", label: "관심 심볼" },
  { href: "/shop", label: "상점" },
];

export default function Navigation() {
  const pathname = usePathname();

  if (!pathname || pathname === "/") {
    return null;
  }

  return (
    <nav className="flex items-center gap-5 absolute left-1/2 -translate-x-1/2 top-1/2 -translate-y-1/2 min-w-[560px] justify-center">
      {NAV_ITEMS.map((item) => {
        const isActive =
          pathname === item.href || pathname.startsWith(`${item.href}/`);

        return (
          <Link
            key={item.href}
            href={item.href}
            className={clsx(
              "rounded-full px-4 py-2 text-sm-custom transition-colors",
              isActive
                ? "bg-main-blue/10 text-main-blue font-semibold"
                : "text-sub hover:text-main-dark-gray"
            )}
          >
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
