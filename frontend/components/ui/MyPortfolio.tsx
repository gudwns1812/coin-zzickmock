import { usePortfolioStore } from "@/store/usePortfolio";
import { JwtToken } from "@/type/jwt";
import clsx from "clsx";
import Image from "next/image";
import Link from "next/link";
import React from "react";

const MyPortfolio = ({ token }: { token: JwtToken | null }) => {
  const { portfolio } = usePortfolioStore();

  if (!token) {
    return (
      <div className="flex flex-col gap-main">
        <h2 className="text-xl-custom font-bold text-main-dark-gray">
          내 투자
        </h2>
        <div className="flex flex-col gap-main">
          <div className="w-full h-[200px] flex items-center justify-center">
            <p className="text-main-dark-gray">로그인 후 이용해주세요.</p>
          </div>
        </div>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-main">
      <h2 className="text-xl-custom font-bold text-main-dark-gray">내 투자</h2>
      <div className="flex flex-col gap-main">
        {!token && (
          <div className="w-full h-[200px] flex items-center justify-center">
            <p className="text-main-dark-gray">로그인 후 이용해주세요.</p>
          </div>
        )}
        {portfolio.length === 0 && (
          <div className="w-full h-[200px] flex items-center justify-center">
            <p className="text-main-dark-gray">포트폴리오가 비어있습니다.</p>
          </div>
        )}
        {portfolio.map((stock) => (
          <Link
            key={`my-portfolio-sidebar-${stock.stockCode}`}
            href={`/stock/${stock.stockCode}`}
            className="flex items-center gap-main hover:bg-main-blue/10 rounded-main transition-colors duration-200 p-main"
          >
            <div className="relative flex items-center justify-center size-[40px] shrink-0">
              {stock.stockImage ? (
                <Image
                  src={stock.stockImage}
                  alt={stock.stockName}
                  fill
                  className="rounded-full"
                />
              ) : (
                <div className="bg-main-blue/10 rounded-full size-[40px] shrink-0 flex items-center justify-center">
                  <span className="text-main-blue font-semibold">
                    {stock.stockName[0]}
                  </span>
                </div>
              )}
            </div>
            <div className="flex flex-col gap-1">
              <div className="flex gap-1 items-center">
                <span className="font-semibold">{stock.stockName}</span>
                <span className="text-gray-500 text-xs-custom">
                  {stock.stockCode}
                </span>
              </div>

              <div className="grid grid-cols-[auto_1fr] gap-1">
                <span className="text-gray-500 text-xs-custom">수익</span>
                <span
                  className={clsx(
                    "text-xs-custom font-semibold",
                    stock.profitLoss > 0 ? "text-main-red" : "text-main-blue"
                  )}
                >
                  {stock.profitLoss.toLocaleString()}원 (
                  {stock.profitLossRate.toFixed(2)}%)
                </span>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
};

export default MyPortfolio;
