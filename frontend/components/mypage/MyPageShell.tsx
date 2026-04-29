"use client";

import clsx from "clsx";
import {
  BarChart3,
  ClipboardList,
  Coins,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";

const MY_PAGE_LINKS = [
  { href: "/mypage", label: "Info", icon: UserRound },
  { href: "/mypage/assets", label: "Assets", icon: BarChart3 },
  { href: "/mypage/points", label: "Point", icon: Coins },
  { href: "/mypage/redemptions", label: "교환 내역", icon: ClipboardList },
];

export default function MyPageShell({
  children,
  isAdmin,
}: {
  children: ReactNode;
  isAdmin: boolean;
}) {
  const pathname = usePathname();
  const currentPathname = pathname ?? "";

  return (
    <div className="grid grid-cols-[220px_1fr] gap-main-2 px-main-2 pb-24 pt-4">
      <aside className="h-fit rounded-main border border-main-light-gray bg-white p-main shadow-sm">
        <p className="px-2 text-xs-custom font-semibold uppercase text-main-dark-gray/45">
          My Page
        </p>
        <nav className="mt-4 flex flex-col gap-1">
          {MY_PAGE_LINKS.map((item) => {
            const active =
              currentPathname === item.href ||
              (item.href !== "/mypage" &&
                currentPathname.startsWith(`${item.href}/`));
            const Icon = item.icon;

            return (
              <Link
                aria-current={active ? "page" : undefined}
                className={clsx(
                  "flex items-center gap-3 rounded-main px-3 py-3 text-sm-custom font-semibold transition-colors",
                  active
                    ? "bg-main-blue text-white"
                    : "text-main-dark-gray/65 hover:bg-main-light-gray/45 hover:text-main-dark-gray"
                )}
                href={item.href}
                key={item.href}
              >
                <Icon size={17} />
                {item.label}
              </Link>
            );
          })}
        </nav>

        {isAdmin ? (
          <div className="mt-main border-t border-main-light-gray pt-main">
            <p className="px-2 text-xs-custom font-semibold uppercase text-main-dark-gray/45">
              Admin
            </p>
            <Link
              className="mt-3 flex items-center gap-3 rounded-main px-3 py-3 text-sm-custom font-semibold text-main-dark-gray/65 transition-colors hover:bg-main-light-gray/45 hover:text-main-dark-gray"
              href="/admin"
            >
              <ShieldCheck size={17} />
              관리자 페이지
            </Link>
          </div>
        ) : null}
      </aside>

      <main className="min-w-0">{children}</main>
    </div>
  );
}
