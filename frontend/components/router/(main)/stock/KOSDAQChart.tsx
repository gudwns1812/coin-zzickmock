"use client";

import React, { useEffect, useState } from "react";
import { Line } from "react-chartjs-2";
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
} from "chart.js";
import DownPrice from "@/components/ui/shared/DownPrice";
import UpPrice from "@/components/ui/shared/UpPrice";
import clsx from "clsx";
import { RefreshCcw } from "lucide-react";
import { toast } from "react-toastify";
import { KOSDAQ } from "@/type/stocks/KOSDAQ";
import { useQuery } from "@tanstack/react-query";
import ErrorComponent from "@/components/ui/shared/ErrorComponent";

ChartJS.register(
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Filler
);

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
      enabled: true,
    },
  },
  scales: {
    x: {
      display: false, // X축 비활성화
    },
    y: {
      display: false, // Y축 비활성화
    },
  },
  elements: {
    line: {
      borderWidth: 2,
    },
  },
};

const KOSDAQChart = () => {
  const {
    data: KOSDAQData,
    isLoading,
    error,
  } = useQuery<KOSDAQ>({
    queryKey: ["kosdaq"],
    queryFn: async () => {
      const res = await fetch("/proxy2/v2/stocks/indices/KOSDAQ");
      if (!res.ok) throw new Error("데이터 로딩 실패");
      return res.json().then((data) => data.data);
    },
    retry: 1,
  });

  if (error)
    return (
      <ErrorComponent
        message="차트 데이터를 불러오는데 실패했습니다."
        className="min-h-[232px]"
      />
    );

  if (isLoading) {
    return (
      <div className="p-4 bg-gray-100 animate-pulse rounded-md text-center min-h-[232px] flex items-center justify-center">
        📈 차트 데이터를 불러오는 중입니다...
      </div>
    );
  }

  if (!KOSDAQData) return null;

  const labels = KOSDAQData.indices
    .slice()
    .reverse()
    .map((item) => item.stck_bsop_date);
  const data = {
    labels,
    datasets: [
      {
        fill: true,
        data: KOSDAQData.indices
          .slice()
          .reverse()
          .map((item) => Number(item.bstp_nmix_prpr)),
        borderColor:
          KOSDAQData.sign === "1" || KOSDAQData.sign === "2"
            ? "#f04251"
            : KOSDAQData.sign === "4" || KOSDAQData.sign === "5"
            ? "#3485fa"
            : "#3485fa",
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

          if (KOSDAQData.sign === "1" || KOSDAQData.sign === "2") {
            gradient.addColorStop(0, "rgba(240, 66, 81, 0.5)");
            gradient.addColorStop(1, "rgba(240, 66, 81, 0)");
          } else if (KOSDAQData.sign === "4" || KOSDAQData.sign === "5") {
            gradient.addColorStop(0, "rgba(52, 133, 250, 0.5)");
            gradient.addColorStop(1, "rgba(52, 133, 250, 0)");
          }

          return gradient;
        },
        tension: 0.1,
        pointRadius: 0,
        borderWidth: 1.5,
        pointHoverRadius: 0,
      },
    ],
  };

  return (
    <div className="flex flex-col gap-main bg-white p-main">
      <div
        className={clsx(
          "grid grid-cols-[1fr_auto] items-start gap-1",
          KOSDAQData.sign === "1" || KOSDAQData.sign === "2"
            ? "border-l-4 border-main-red pl-main"
            : KOSDAQData.sign === "4" || KOSDAQData.sign === "5"
            ? "border-l-4 border-main-blue pl-main"
            : "border-l-4 border-main-dark-gray pl-main"
        )}
      >
        <span className="text-sm">KOSDAQ</span>

        <div className="row-span-2 flex flex-col items-center gap-1">
          <span className="text-sm text-gray-500">
            고가 {KOSDAQData.indices[0].bstp_nmix_hgpr}
          </span>
          <span className="text-sm text-gray-500">
            저가 {KOSDAQData.indices[0].bstp_nmix_lwpr}
          </span>
        </div>
        <div className="flex items-center gap-main">
          <span className="text-xl font-semibold">
            {KOSDAQData.indices[0].bstp_nmix_prpr}
          </span>
          {KOSDAQData.sign === "1" ||
            (KOSDAQData.sign === "2" && (
              <UpPrice
                change={(
                  Number(KOSDAQData.indices[0].bstp_nmix_prpr) -
                  Number(KOSDAQData.indices[1].bstp_nmix_prpr)
                ).toFixed(2)}
                changeRate={Number(KOSDAQData.prev_rate)}
              />
            ))}
          {KOSDAQData.sign === "4" ||
            (KOSDAQData.sign === "5" && (
              <DownPrice
                change={(
                  Number(KOSDAQData.indices[0].bstp_nmix_prpr) -
                  Number(KOSDAQData.indices[1].bstp_nmix_prpr)
                ).toFixed(2)}
                changeRate={Number(KOSDAQData.prev_rate)}
              />
            ))}
        </div>
      </div>
      <div className="w-full">
        <Line data={data} options={options} />
      </div>
    </div>
  );
};

export default KOSDAQChart;
