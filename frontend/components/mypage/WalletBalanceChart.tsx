"use client";

import { buildWalletBalanceChartPoints } from "@/lib/wallet-history-chart";
import { formatUsd } from "@/lib/markets";
import {
  CategoryScale,
  Chart as ChartJS,
  Filler,
  LineElement,
  LinearScale,
  PointElement,
  ScriptableContext,
  Tooltip,
  type ChartOptions,
} from "chart.js";
import { Line } from "react-chartjs-2";
import type { FuturesWalletHistoryPoint } from "@/lib/futures-api";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip);

type Props = {
  history: FuturesWalletHistoryPoint[];
};

const options: ChartOptions<"line"> = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    mode: "index",
    intersect: false,
  },
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      displayColors: false,
      callbacks: {
        label: (context) => `Wallet ${formatUsd(Number(context.parsed.y ?? 0))}`,
      },
    },
  },
  scales: {
    x: {
      grid: {
        display: false,
      },
      ticks: {
        color: "rgba(37, 41, 52, 0.48)",
        maxTicksLimit: 5,
      },
    },
    y: {
      border: {
        display: false,
      },
      grid: {
        color: "rgba(52, 133, 250, 0.08)",
      },
      ticks: {
        color: "rgba(37, 41, 52, 0.5)",
        callback: (value) => formatUsd(Number(value)).replace(".00", ""),
        maxTicksLimit: 4,
      },
    },
  },
  elements: {
    line: {
      borderWidth: 2,
      tension: 0.28,
    },
    point: {
      radius: 2,
      hitRadius: 12,
      hoverRadius: 5,
    },
  },
};

export default function WalletBalanceChart({ history }: Props) {
  const chartPoints = buildWalletBalanceChartPoints(history);
  const hasHistory = chartPoints.length > 0;

  const data = {
    labels: chartPoints.map((point) => point.label),
    datasets: [
      {
        fill: true,
        data: chartPoints.map((point) => point.walletBalance),
        borderColor: "#3485fa",
        pointBackgroundColor: "#3485fa",
        pointBorderColor: "#ffffff",
        backgroundColor: (context: ScriptableContext<"line">) => {
          const chart = context.chart;
          const { ctx, chartArea } = chart;

          if (!chartArea) {
            return "rgba(52, 133, 250, 0.12)";
          }

          const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
          gradient.addColorStop(0, "rgba(52, 133, 250, 0.22)");
          gradient.addColorStop(1, "rgba(52, 133, 250, 0)");
          return gradient;
        },
      },
    ],
  };

  return (
    <div className="flex min-h-[210px] flex-col">
      <div className="mb-4 flex items-start justify-between gap-main">
        <div>
          <p className="text-xs-custom font-semibold uppercase tracking-normal text-main-dark-gray/45">
            Wallet balance
          </p>
          <p className="mt-1 text-sm-custom text-main-dark-gray/60">
            최근 30일 잔고 변화
          </p>
        </div>
        <span className="rounded-full bg-main-blue/10 px-3 py-1 text-xs-custom font-semibold text-main-blue">
          30D
        </span>
      </div>

      {hasHistory ? (
        <div className="min-h-0 flex-1">
          <Line data={data} options={options} />
        </div>
      ) : (
        <div className="flex flex-1 items-center justify-center rounded-main border border-dashed border-main-light-gray text-sm-custom text-main-dark-gray/50">
          잔고 변화 기록을 준비 중입니다.
        </div>
      )}
    </div>
  );
}
