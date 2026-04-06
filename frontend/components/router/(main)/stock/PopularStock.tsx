"use client";

import { ChevronRight } from "lucide-react";
import React from "react";
import { useRouter } from "next/navigation";
import UpPrice from "@/components/ui/shared/UpPrice";
import DownPrice from "@/components/ui/shared/DownPrice";
import Bookmark from "@/components/ui/shared/Bookmark";
import Scrab from "@/components/ui/shared/Scrab";
import { Popular, TestPopular } from "@/type/stocks/Popular";
import { JwtToken } from "@/type/jwt";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import clsx from "clsx";

const PopularStock = ({ token }: { token: JwtToken | null }) => {
  const {
    data: popularStocks,
    isLoading,
    error,
  } = useQuery<TestPopular[]>({
    queryKey: ["popularStocks"],
    queryFn: () =>
      fetch("/proxy2/v2/stocks/popular").then((res) =>
        res.json().then((data) => data.data)
      ),
  });

  const router = useRouter();

  const handleClickStock = (code: string) => {
    router.push(`/stock/${code}`);
  };

  // 로딩 중일 때 스켈레톤 표시
  if (isLoading) {
    return <PopularStockSkeleton />;
  }

  // 에러 또는 데이터가 없을 때
  if (error || !popularStocks) {
    return (
      <div className="col-span-2 p-main mb-main-2">
        <h2 className="text-xl font-bold text-gray-800 mb-4">인기 종목</h2>
        <div className="flex items-center justify-center h-32 text-gray-500">
          인기 종목을 불러올 수 없습니다.
        </div>
      </div>
    );
  }

  return (
    <div className="col-span-2 p-main mb-main-2">
      <h2 className="text-xl font-bold text-gray-800 mb-4">인기 종목</h2>
      <div className="grid grid-cols-2 gap-main">
        {popularStocks &&
          popularStocks.map((stock, index) => (
            <div
              key={`popular-${stock.stockName}`}
              className="rounded-main px-main-2 py-3 transition duration-300 border border-transparent hover:border-main-blue/20 hover:scale-102 cursor-pointer relative group"
              onClick={(e) => {
                e.stopPropagation();
                handleClickStock(stock.stockCode);
              }}
            >
              <Scrab
                stockCode={stock.stockCode}
                stockName={stock.stockName}
                stockInfo={{
                  stockImage: stock.stockImage ?? "",
                  changeAmount: stock.changeAmount,
                  changeRate: stock.changeRate,
                  sign: stock.sign,
                  currentPrice: stock.price,
                }}
                token={token}
              />

              <Bookmark className="absolute top-0 left-0" rank={index + 1} />
              <div className="flex gap-main w-full">
                <div className="relative flex items-center justify-center size-[40px] shrink-0">
                  {stock.stockImage ? (
                    <Image
                      src={stock.stockImage}
                      alt={stock.stockName}
                      fill
                      className="rounded-full"
                      sizes="40px"
                    />
                  ) : (
                    <div className="bg-main-blue/10 rounded-full size-[40px] shrink-0 flex items-center justify-center">
                      <span className="text-main-blue font-semibold">
                        {stock.stockName[0]}
                      </span>
                    </div>
                  )}
                </div>
                <div className="flex flex-col flex-1 truncate">
                  <div className="font-bold text-gray-800 truncate w-full flex items-baseline gap-1">
                    <span>{stock.stockName}</span>
                    <span className="text-gray-500 text-xs font-normal">
                      {stock.stockCode}
                    </span>
                  </div>
                  <div className="text-sm flex gap-main items-center">
                    <span
                      className={clsx(
                        "text-gray-500 text-sm font-semibold",
                        (stock.sign === "1" || stock.sign === "2") &&
                          "text-main-red",
                        (stock.sign === "4" || stock.sign === "5") &&
                          "text-main-blue",
                        stock.sign === "3" && "text-gray-500"
                      )}
                    >
                      {Number(stock.price).toLocaleString()}
                    </span>
                    <div className="flex justify-between h-fit">
                      {(stock.sign === "1" || stock.sign === "2") && (
                        <UpPrice
                          change={Number(stock.changeAmount)}
                          changeRate={Number(stock.changeRate)}
                        />
                      )}
                      {stock.sign === "3" && (
                        <span className="text-gray-400 font-medium">
                          {Number(stock.changeAmount)} (
                          {Number(stock.changeRate)}%)
                        </span>
                      )}
                      {(stock.sign === "4" || stock.sign === "5") && (
                        <DownPrice
                          change={Number(stock.changeAmount)}
                          changeRate={Number(stock.changeRate)}
                        />
                      )}
                    </div>
                  </div>
                </div>

                <ChevronRight
                  className="hidden group-hover:block text-main-blue absolute top-1/2 -translate-y-1/2 right-main"
                  size={20}
                />
              </div>
            </div>
          ))}
      </div>
    </div>
  );
};

// 스켈레톤 컴포넌트
const PopularStockSkeleton = () => {
  return (
    <div className="col-span-2 p-main mb-main-2">
      <div className="h-7 w-20 bg-gray-200 rounded animate-pulse mb-4"></div>
      <div className="grid grid-cols-2 gap-main">
        {Array.from({ length: 6 }).map((_, index) => (
          <div
            key={`skeleton-${index}`}
            className="rounded-main px-main-2 py-3 border border-gray-100 relative"
          >
            {/* 북마크 스켈레톤 */}
            <div className="absolute top-0 left-0 w-8 h-6 bg-gray-200 rounded animate-pulse"></div>

            <div className="flex gap-main w-full">
              {/* 종목 이미지 스켈레톤 */}
              <div className="size-[40px] bg-gray-200 rounded-full animate-pulse shrink-0"></div>

              <div className="flex flex-col flex-1 truncate gap-1">
                {/* 종목명 스켈레톤 */}
                <div className="flex items-baseline gap-1">
                  <div className="h-4 w-16 bg-gray-200 rounded animate-pulse"></div>
                  <div className="h-3 w-12 bg-gray-200 rounded animate-pulse"></div>
                </div>

                {/* 가격 정보 스켈레톤 */}
                <div className="flex gap-main items-center">
                  <div className="h-4 w-20 bg-gray-200 rounded animate-pulse"></div>
                  <div className="h-3 w-14 bg-gray-200 rounded animate-pulse"></div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default PopularStock;
