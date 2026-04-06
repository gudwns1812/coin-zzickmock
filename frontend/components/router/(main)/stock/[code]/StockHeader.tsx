"use client";

import DownPrice from "@/components/ui/shared/DownPrice";
import UpPrice from "@/components/ui/shared/UpPrice";
import { useRecentViewStore } from "@/store/useRecentViewStore";
import { RealTimeStockData } from "@/hooks/useRealTimeStock";
import clsx from "clsx";
import Image from "next/image";
import React, { useEffect, useState } from "react";

function isMarketOpen(now: Date = new Date()) {
  const dayOfWeek = now.getDay(); // 0=일요일, 6=토요일
  if (dayOfWeek === 0 || dayOfWeek === 6) return false; // 주말

  const hour = now.getHours();
  const minute = now.getMinutes();
  const currentTime = hour * 100 + minute;

  return currentTime >= 900 && currentTime <= 1530; // 09:00 ~ 15:30
}

interface StockHeaderProps {
  code: string;
  realTimeStock?: RealTimeStockData;
}

const StockHeader = ({ code, realTimeStock }: StockHeaderProps) => {
  const [marketOpen, setMarketOpen] = useState(false);
  const { recentViewStocks, setRecentViewStocks } = useRecentViewStore();

  useEffect(() => {
    if (!realTimeStock) return;

    if (recentViewStocks.some((stock) => stock.stockCode === code)) return;
    setRecentViewStocks([
      {
        stockImage: realTimeStock.stockImage,
        stockCode: realTimeStock.stockCode,
        stockName: realTimeStock.stockName,
      },
      ...recentViewStocks,
    ]);
  }, [realTimeStock, code]);

  // 주식장 오픈 여부 체크
  useEffect(() => {
    const checkMarket = () => setMarketOpen(isMarketOpen());
    checkMarket(); // 최초 1회
    const timer = setInterval(checkMarket, 60 * 1000); // 1분마다 체크
    return () => clearInterval(timer);
  }, []);

  if (!realTimeStock) return <div>로딩 중...</div>;

  return (
    <div className="flex items-center gap-2 w-full">
      <div className="relative flex items-center justify-center size-[40px] shrink-0">
        {realTimeStock.stockImage ? (
          <Image
            src={realTimeStock.stockImage}
            alt={realTimeStock.stockName}
            fill
            className="rounded-full"
          />
        ) : (
          <div className="bg-main-blue/10 rounded-full size-[40px] shrink-0 flex items-center justify-center">
            <span className="text-main-blue font-semibold">
              {realTimeStock.stockName[0]}
            </span>
          </div>
        )}
      </div>
      <div className="flex flex-col flex-1 truncate text-sm-custom">
        <p className="flex items-center gap-main text-gray-800 truncate w-full">
          <span className="font-bold">{realTimeStock.stockName}</span>
          <span className="text-gray-400">{realTimeStock.stockCode}</span>
          <span
            className={clsx(
              "px-2 py-1 rounded-main font-bold text-sm-custom",
              marketOpen
                ? "text-main-blue bg-main-blue/10"
                : "text-main-red bg-main-red/10"
            )}
          >
            {marketOpen ? "OPEN" : "CLOSED"}
          </span>
        </p>
        <div className="flex items-center gap-main">
          <span className="text-main-dark-gray text-xl-custom font-bold">
            {Number(realTimeStock.price).toLocaleString()}원
          </span>
          <div className="flex justify-between h-fit">
            {(realTimeStock.sign === "1" || realTimeStock.sign === "2") && (
              <UpPrice
                change={Number(realTimeStock.changeAmount)}
                changeRate={Number(realTimeStock.changeRate)}
              />
            )}
            {realTimeStock.sign === "3" && (
              <span className="text-gray-400 font-medium">
                {Number(realTimeStock.changeAmount)} (
                {Number(realTimeStock.changeRate)}%)
              </span>
            )}
            {(realTimeStock.sign === "4" || realTimeStock.sign === "5") && (
              <DownPrice
                change={Number(realTimeStock.changeAmount)}
                changeRate={Number(realTimeStock.changeRate)}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default StockHeader;
