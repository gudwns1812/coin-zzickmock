"use client";

import React, { useState } from "react";
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
import { faker } from "@faker-js/faker";
import clsx from "clsx";

ChartJS.register(
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Filler
);

const labels = Array.from({ length: 50 }, (_, i) => `${i}일`);
const data = {
  labels,
  datasets: [
    {
      fill: true,
      data: labels.map(() => faker.number.float({ min: 1500, max: 2000 })),
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
      tension: 0.2,
      pointRadius: 0,
      borderWidth: 2,
      pointHoverRadius: 0,
    },
  ],
};

const options: ChartOptions<"line"> = {
  responsive: true,
  maintainAspectRatio: false,
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
      display: true, // X축 비활성화
      grid: {
        color: "rgba(0, 0, 0, 0.02)",
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

const chartTypes = [
  { label: "1D", state: "1D" },
  { label: "1W", state: "1W" },
  { label: "1M", state: "1M" },
  { label: "1Y", state: "1Y" },
];

type ChartType = (typeof chartTypes)[number]["state"];

const AccountChart = () => {
  const [chartType, setChartType] = useState<ChartType>("1D");

  return (
    <div className="w-full flex gap-main">
      <nav className="flex flex-col gap-main">
        {chartTypes.map((type) => (
          <button
            key={type.state}
            className={clsx(
              "px-main-2 py-2 rounded-main transition-colors",
              chartType === type.state
                ? "bg-main-light-gray text-black"
                : "hover:bg-main-light-gray text-main-dark-gray"
            )}
            onClick={() => setChartType(type.state)}
          >
            {type.label}
          </button>
        ))}
      </nav>
      <div className="flex-1">
        <Line data={data} options={options} />
      </div>
    </div>
  );
};

export default AccountChart;
