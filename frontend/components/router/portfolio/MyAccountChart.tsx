"use client";

import React, { useEffect, useState } from "react";
import { Line } from "react-chartjs-2";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { PortfolioData } from "@/type/portfolio";
import {
  Chart as ChartJS,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Filler,
  ScriptableContext,
  ChartOptions,
  ChartData,
} from "chart.js";
import { faker } from "@faker-js/faker";
import clsx from "clsx";
import { JwtToken } from "@/type/jwt";
import MyProfit from "./MyProfit";
import Button from "@/components/ui/shared/Button";
import { usePortfolioStore } from "@/store/usePortfolio";

ChartJS.register(
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Filler
);

const chartTypes = [
  // { label: "1일", state: "D" },
  { label: "1주", state: "W" },
  { label: "1개월", state: "M" },
  { label: "3개월", state: "3M" },
  { label: "1년", state: "Y" },
];

type ChartType = (typeof chartTypes)[number]["state"];

interface Asset {
  periodAsset: number;
  pnlHistory: {
    date: number[]; // [2025, 6, 30] 형태
    asset: number;
    pnl: number;
  }[];
}

const MyAccountChart = ({ token }: { token: JwtToken | null }) => {
  const [chartType, setChartType] = useState<ChartType>("W");
  const [dummyData, setDummyData] = useState<ChartData<"line"> | null>(null);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  // 포트폴리오 스토어에서 현재 보유 종목 데이터 가져오기
  const { portfolio } = usePortfolioStore();

  // 현재 포트폴리오 총 평가금액 계산
  const currentPortfolioValue = portfolio.reduce((total, stock) => {
    return total + stock.currentPrice * stock.stockCount;
  }, 0);

  // Asset 데이터 (차트 타입에 따라 변경)
  const { data: asset } = useQuery({
    queryKey: ["portfolioAsset", token?.account, chartType],
    queryFn: async () => {
      if (!token) throw new Error("No token");
      const res = await fetch(
        `/proxy/v1/portfolios/asset/${token.account}?period=${chartType}`,
        { credentials: "include" }
      );
      if (!res.ok) throw new Error("Failed to get asset data");
      const json = await res.json();
      return json.data as Asset;
    },
    enabled: !!token,
    placeholderData: keepPreviousData,
    staleTime: 0, // 캐싱 없음
    gcTime: 0, // 즉시 가비지 컬렉션
  });

  // Today PnL (20초마다 폴링)
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

  // Month PnL (초기 로드만)
  const { data: periodPnl } = useQuery({
    queryKey: ["monthPnl", token?.account],
    queryFn: async () => {
      if (!token) throw new Error("No token");
      const res = await fetch(
        `/proxy/v1/portfolios/asset/pnl/${token.account}?period=M`,
        { credentials: "include" }
      );
      if (!res.ok) throw new Error("Failed to get month pnl");
      const json = await res.json();
      return json.data.pnl as number;
    },
    enabled: !!token,
    staleTime: 0, // 캐싱 없음
    gcTime: 0, // 즉시 가비지 컬렉션
  });

  // Total PnL (초기 로드만)
  const { data: totalPnl } = useQuery({
    queryKey: ["totalPnl", token?.account],
    queryFn: async () => {
      if (!token) throw new Error("No token");
      const res = await fetch(
        `/proxy/v1/portfolios/asset/pnl/${token.account}?period=Total`,
        { credentials: "include" }
      );
      if (!res.ok) throw new Error("Failed to get total pnl");
      const json = await res.json();
      return json.data.pnl as number;
    },
    enabled: !!token,
    staleTime: 0, // 캐싱 없음
    gcTime: 0, // 즉시 가비지 컬렉션
  });

  const options: ChartOptions<"line"> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: "x",
      intersect: false,
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        mode: "index",
        intersect: false,
        axis: "x",
        enabled: true,
        external: (context) => {
          const tooltip = context.tooltip;
          if (tooltip?.dataPoints?.[0]) {
            const index = tooltip.dataPoints[0].dataIndex;
            setHoveredIndex(index);
          } else {
            setHoveredIndex(null);
          }
        },
        callbacks: {
          label: (context) => {
            const value = context.parsed.y ?? 0;
            return `${value.toLocaleString()}원`;
          },
        },
      },
    },
    onHover: (event, chartElement) => {
      if (chartElement.length > 0) {
        setHoveredIndex(chartElement[0].index);
      } else {
        setHoveredIndex(null);
      }
    },
    scales: {
      x: {
        display: false, // X축 비활성화
        grid: {
          color: "rgba(0, 0, 0, 0.02)",
          drawOnChartArea: true,
        },
      },
      y: {
        display: false, // Y축 비활성화
      },
    },
    elements: {
      line: {
        borderWidth: 1,
      },
      point: {
        hitRadius: 10,
        hoverRadius: 10,
      },
    },
  };

  const verticalLinePlugin = {
    id: "verticalLine",
    afterDraw(chart: ChartJS) {
      const activeElements = chart.tooltip?.getActiveElements?.();
      if (activeElements && activeElements.length > 0) {
        const ctx = chart.ctx;
        const x = activeElements[0].element.x;
        const topY = chart.scales.y.top;
        const bottomY = chart.scales.y.bottom;

        ctx.save();
        ctx.beginPath();
        ctx.setLineDash([4, 4]);
        ctx.moveTo(x, topY);
        ctx.lineTo(x, bottomY);
        ctx.lineWidth = 1;
        ctx.strokeStyle = "rgba(0,0,0,0.3)";
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.restore();
      }
    },
  };

  useEffect(() => {
    const labels = Array.from({ length: 50 }, (_, i) => `${i}일`);
    const data = {
      labels,
      datasets: [
        {
          fill: true,
          data: labels.map(() =>
            faker.number.int({ min: 100000, max: 20000000 })
          ),
          borderColor: "#3485fa",
          backgroundColor: (context: ScriptableContext<"line">) => {
            const chart = context.chart;
            const { ctx, chartArea } = chart;
            if (!chartArea) return;
            const gradient = ctx.createLinearGradient(
              0,
              chartArea.top,
              0,
              chartArea.bottom
            );
            gradient.addColorStop(0, "rgba(52, 133, 250, 0.5)");
            gradient.addColorStop(1, "rgba(52, 133, 250, 0)");
            return gradient;
          },
          tension: 0.1,
          borderWidth: 1,
          pointRadius: (context: ScriptableContext<"line">) => {
            return context.dataIndex === hoveredIndex ? 5 : 2;
          },
          pointHoverRadius: (context: ScriptableContext<"line">) => {
            return context.dataIndex === hoveredIndex ? 5 : 2;
          },
        },
      ],
    };
    setDummyData(data);
  }, []);

  if (!asset)
    return (
      <div className="relative size-full">
        <div className="absolute inset-0 bg-white/50 z-10 size-full flex items-center justify-center">
          <span className="text-main-dark-gray font-semibold">
            투자 내역이 없습니다.
          </span>
        </div>

        <div className="size-full flex flex-col gap-main blur-xs">
          <div className="grid grid-cols-[auto_1px_1fr] gap-main">
            <div className="flex flex-col gap-main">
              <div className="flex gap-2 items-baseline">
                <span className="text-lg-custom font-semibold">
                  {Number(12801235).toLocaleString()}원
                </span>
              </div>

              <nav className="flex gap-main h-fit self-end">
                {chartTypes.map((type) => (
                  <button
                    key={type.state}
                    className={clsx(
                      "px-main-2 py-2 rounded-main transition-colors",
                      chartType === type.state
                        ? "bg-main-blue/20 text-main-blue"
                        : "hover:bg-main-dark-gray/20 text-main-dark-gray"
                    )}
                    onClick={() => setChartType(type.state)}
                  >
                    {type.label}
                  </button>
                ))}
              </nav>
            </div>

            <div className="h-full bg-main-dark-gray/10" />

            <div className="flex gap-main">
              <MyProfit title="당일 손익" profit={null} />
              <MyProfit title="월 누적 손익" profit={null} />
              <MyProfit title="총 누적 손익" profit={null} />
            </div>
          </div>

          <div className="flex-1 w-full max-h-[400px]">
            <Line
              data={
                dummyData ?? {
                  labels: [],
                  datasets: [],
                }
              }
              options={options}
              plugins={[]}
            />
          </div>
        </div>
      </div>
    );

  // 날짜 순서대로 정렬
  const sortedPnlHistory = asset.pnlHistory.slice().sort((a, b) => {
    const dateA = new Date(a.date[0], a.date[1] - 1, a.date[2]);
    const dateB = new Date(b.date[0], b.date[1] - 1, b.date[2]);
    return dateA.getTime() - dateB.getTime();
  });

  const labels = sortedPnlHistory.map((p) => {
    const [year, month, day] = p.date;
    return `${year}-${month.toString().padStart(2, "0")}-${day
      .toString()
      .padStart(2, "0")}`;
  });

  // 차트 데이터 생성 (마지막 포인트는 현재 포트폴리오 평가금액)
  const chartData = sortedPnlHistory.map((p, index) => {
    // 마지막 데이터인 경우 현재 포트폴리오 평가금액 사용
    if (index === sortedPnlHistory.length - 1) {
      return currentPortfolioValue;
    }

    return p.asset;
  });

  const data = {
    labels,
    datasets: [
      {
        fill: true,
        data: chartData, // 실시간 손익이 반영된 데이터
        borderColor: "#3485fa",
        backgroundColor: (context: ScriptableContext<"line">) => {
          const chart = context.chart;
          const { ctx, chartArea } = chart;

          if (!chartArea) return;

          const gradient = ctx.createLinearGradient(
            0,
            chartArea.top,
            0,
            chartArea.bottom
          );
          gradient.addColorStop(0, "rgba(52, 133, 250, 0.5)");
          gradient.addColorStop(1, "rgba(52, 133, 250, 0)");

          return gradient;
        },
        tension: 0.05,
        pointRadius: (context: ScriptableContext<"line">) => {
          return context.dataIndex === hoveredIndex ? 3 : 0;
        },
        pointHoverRadius: (context: ScriptableContext<"line">) => {
          return context.dataIndex === hoveredIndex ? 3 : 0;
        },
        borderWidth: 2,
      },
    ],
  };

  return (
    <div className="size-full flex flex-col gap-main">
      <div className="grid grid-cols-[auto_1px_1fr] gap-main">
        <div className="flex flex-col gap-main items-start">
          <div className="flex gap-2 items-baseline">
            <span className="text-lg-custom font-semibold">
              {currentPortfolioValue.toLocaleString()}원
            </span>
            <span
              className={clsx(
                "text-xs-custom font-semibold",
                sortedPnlHistory.length > 0 &&
                  sortedPnlHistory[sortedPnlHistory.length - 1].pnl > 0
                  ? "text-main-red"
                  : "text-main-blue"
              )}
            >
              {chartType === "W" && "지난주보다"}
              {chartType === "M" && "지난달보다"}
              {chartType === "3M" && "지난 3개월보다"}
              {chartType === "Y" && "작년보다"}{" "}
              {sortedPnlHistory.length > 0 &&
              sortedPnlHistory[sortedPnlHistory.length - 1].pnl > 0
                ? "+"
                : ""}
              {sortedPnlHistory.length > 0
                ? (() => {
                    const currentData =
                      sortedPnlHistory[sortedPnlHistory.length - 1];
                    const previousAsset = currentData.asset - currentData.pnl;
                    return previousAsset > 0
                      ? ((currentData.pnl / previousAsset) * 100).toFixed(2)
                      : "0.00";
                  })()
                : "0.00"}
              %
            </span>
          </div>

          <nav className="flex gap-main-1/2 h-fit">
            {chartTypes.map((type) => (
              <Button
                key={type.state}
                variant={chartType === type.state ? "primary" : "ghost"}
                className="!rounded-full"
                onClick={() => setChartType(type.state)}
              >
                {type.label}
              </Button>
            ))}
          </nav>
        </div>

        <div className="h-full bg-main-dark-gray/10" />

        <div className="flex gap-main">
          {/* <MyProfit title="당일 손익" profit={asset.todayPnl} />
          <MyProfit title="월 누적 손익" profit={asset.periodPnl} />
          <MyProfit
            title="총 누적 손익"
            profit={asset.pnlHistory.reduce((acc, curr) => acc + curr.pnl, 0)}
          /> */}
          <MyProfit title="당일 손익" profit={todayPnl ?? null} />
          <MyProfit
            title="월 누적 손익"
            profit={(periodPnl ?? 0) + (todayPnl ?? 0)}
          />
          <MyProfit
            title="총 누적 손익"
            profit={(totalPnl ?? 0) + (todayPnl ?? 0)}
          />
        </div>
      </div>

      <div className="flex-1 size-full">
        <Line data={data} options={options} plugins={[verticalLinePlugin]} />
      </div>
    </div>
  );
};

export default MyAccountChart;
