import React from "react";
import Link from "next/link";
import clsx from "clsx";
import { TrendingUp } from "lucide-react";

const Footer = ({ className }: { className?: string }) => {
  return (
    <footer
      className={clsx(
        "w-full py-main-2 bg-main-light-gray/30 flex flex-col gap-main items-center justify-center text-center text-main-dark-gray rounded-t-main",
        className
      )}
    >
      <div className="font-bold text-lg-custom flex items-center gap-2">
        <Link
          href="/stock"
          className="size-[40px] rounded-main bg-main-blue text-white flex items-center justify-center"
          aria-label="주식 찍먹 홈"
        >
          <TrendingUp size={20} />
        </Link>
        <div className="flex flex-col">
          <span className="font-bold text-lg-custom text-black">주식 찍먹</span>
          <span className="text-sm-custom text-main-dark-gray">
            가볍게 시작하는 주식 탐색과 포트폴리오 흐름
          </span>
        </div>
      </div>

      <nav className="flex gap-6 text-base-custom font-medium">
        <Link
          href="/portfolio"
          className="hover:text-main-blue transition-colors font-semibold"
        >
          포트폴리오
        </Link>
        <Link
          href="/stock"
          className="hover:text-main-blue transition-colors font-semibold"
        >
          증권
        </Link>
      </nav>

      <div className="text-sm-custom text-gray-700">
        실시간 시세 확인, 종목 탐색, 관심종목 관리, 포트폴리오 흐름에 집중한
        프론트엔드 워크스페이스
      </div>
      <div className="text-xs-custom text-gray-400">© 2025 주식 찍먹</div>
    </footer>
  );
};

export default Footer;
