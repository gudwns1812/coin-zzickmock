"use client";

import React, { useState } from "react";
import Calendar from "react-calendar";
import { useQuery } from "@tanstack/react-query";
import "./calendar.css";
import { JwtToken } from "@/type/jwt";
import clsx from "clsx";

interface Pnl {
  createdDate: string | null;
  lastModifiedDate: string;
  id: number;
  memberId: string;
  date: string | [number, number, number];
  asset: number;
  pnl: number;
}

const toDate = (value: Pnl["date"]) => {
  if (Array.isArray(value)) {
    return new Date(value[0], value[1] - 1, value[2]);
  }

  return new Date(value);
};

const ProfitLossCalendar = ({ token }: { token: JwtToken | null }) => {
  // 연간 PnL 데이터
  const { data: pnl } = useQuery({
    queryKey: ["yearlyPnl", token?.account],
    queryFn: async () => {
      if (!token) throw new Error("No token");
      const res = await fetch(
        `/proxy/v1/portfolios/asset/${token.account}?period=Y`,
        { credentials: "include" }
      );
      if (!res.ok) throw new Error("Failed to get yearly pnl");
      const json = await res.json();
      return json.data.pnlHistory as Pnl[];
    },
    enabled: !!token,
    staleTime: 0, // 캐싱 없음
    gcTime: 0, // 즉시 가비지 컬렉션
  });

  // Today PnL (20초마다 폴링) - MyAccountChart와 동일한 queryKey 사용
  const { data: todayPnl } = useQuery({
    queryKey: ["todayPnl", token?.account],
    queryFn: async () => {
      if (!token) throw new Error("No token");
      const res = await fetch(
        `/proxy/v1/portfolios/asset/pnl/${token.account}?period=Today`,
        { credentials: "include" }
      );
      if (!res.ok) throw new Error("Failed to get today pnl");
      const json = await res.json();
      return json.data.pnl as number;
    },
    enabled: !!token,
    refetchInterval: 20000, // 20초마다
    staleTime: 0, // 캐싱 없음
    gcTime: 0, // 즉시 가비지 컬렉션
  });

  if (!pnl || pnl.length === 0)
    return (
      <div className="relative">
        <span className="absolute top-1/2 -translate-y-1/2 left-1/2 -translate-x-1/2 text-main-dark-gray font-semibold z-20">
          손익 데이터가 없습니다.
        </span>
        <div className="blur-xs z-10 pointer-events-none">
          <Calendar
            prev2Label={null}
            next2Label={null}
            maxDate={new Date()}
            calendarType="gregory"
            view="month"
            navigationLabel={({ date }) => {
              const year = date.getFullYear();
              const month = String(date.getMonth() + 1).padStart(2, "0");
              return `${year}년 ${month}월`;
            }}
            formatDay={(locale, date) => String(date.getDate())}
            tileContent={({ date }) => {
              const dateString = `${date.getFullYear()}-${String(
                date.getMonth() + 1
              ).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;

              return (
                <span className="w-full flex justify-center text-xs-custom">
                  {Number(100000000).toLocaleString()}
                </span>
              );
            }}
            showNeighboringMonth={false}
            className="w-full h-full"
          />
        </div>
      </div>
    );

  return (
    <Calendar
      prev2Label={null}
      next2Label={null}
      maxDate={new Date()}
      minDate={new Date(new Date().getFullYear(), 0, 1)}
      calendarType="gregory"
      view="month"
      navigationLabel={({ date }) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        return `${year}년 ${month}월`;
      }}
      formatDay={(locale, date) => String(date.getDate())}
      tileContent={({ date }) => {
        const today = new Date();
        const isToday =
          date.getFullYear() === today.getFullYear() &&
          date.getMonth() === today.getMonth() &&
          date.getDate() === today.getDate();

        // 오늘 날짜인 경우 todayPnl 사용
        if (isToday && todayPnl !== undefined) {
          return (
            <div className="w-full flex flex-col items-center">
              <span
                className={clsx(
                  "flex justify-center text-xs-custom font-semibold",
                  todayPnl > 0 && "text-main-red",
                  todayPnl < 0 && "text-main-blue",
                  todayPnl === 0 && "text-main-dark-gray"
                )}
              >
                {todayPnl > 0 && "+"}
                {Number(todayPnl).toLocaleString()}
              </span>
            </div>
          );
        }

        // 과거 날짜는 기존 pnl 데이터에서 찾기
        const pnlData = pnl.find(
          (p) =>
            toDate(p.date).getFullYear() === date.getFullYear() &&
            toDate(p.date).getMonth() === date.getMonth() &&
            toDate(p.date).getDate() === date.getDate()
        );

        if (!pnlData)
          return (
            <span className="w-full flex justify-center text-xs-custom text-main-dark-gray">
              -
            </span>
          );

        return (
          <span
            className={clsx(
              "w-full flex justify-center text-xs-custom font-semibold",
              pnlData.pnl > 0 && "text-main-red",
              pnlData.pnl < 0 && "text-main-blue",
              pnlData.pnl === 0 && "text-main-dark-gray"
            )}
          >
            {pnlData.pnl > 0 && "+"}
            {Number(pnlData.pnl).toLocaleString()}
          </span>
        );
      }}
      showNeighboringMonth={false}
      className="w-full h-full"
    />
  );
};

export default ProfitLossCalendar;
