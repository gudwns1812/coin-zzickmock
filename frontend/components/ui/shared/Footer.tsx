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
          href="/markets"
          className="size-[40px] rounded-main bg-main-blue text-white flex items-center justify-center"
          aria-label="코인 선물 찍먹 홈"
        >
          <TrendingUp size={20} />
        </Link>
        <div className="flex flex-col">
          <span className="font-bold text-lg-custom text-black">
            코인 선물 찍먹
          </span>
          <span className="text-sm-custom text-main-dark-gray">
            Bitget 기반 선물 체험과 포지션 흐름 정리
          </span>
        </div>
      </div>

      <nav className="flex gap-6 text-base-custom font-medium">
        <Link
          href="/portfolio"
          className="hover:text-main-blue transition-colors font-semibold"
        >
          계정
        </Link>
        <Link
          href="/markets"
          className="hover:text-main-blue transition-colors font-semibold"
        >
          마켓
        </Link>
        <Link
          href="/shop"
          className="hover:text-main-blue transition-colors font-semibold"
        >
          상점
        </Link>
      </nav>

      <div className="text-sm-custom text-gray-700">
        선물 마켓 탐색, 포지션 체험, 포인트 상점 흐름에 집중한 프론트엔드
        워크스페이스
      </div>
      <div className="text-xs-custom text-gray-400">© 2025 코인 선물 찍먹</div>
    </footer>
  );
};

export default Footer;
