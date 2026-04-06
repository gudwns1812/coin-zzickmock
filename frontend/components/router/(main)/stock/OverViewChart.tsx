"use client";

import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Filler,
  Legend,
  ChartOptions,
  ScriptableContext,
} from "chart.js";
import { Line } from "react-chartjs-2";
import DownPrice from "@/components/ui/shared/DownPrice";
import UpPrice from "@/components/ui/shared/UpPrice";
import ErrorComponent from "@/components/ui/shared/ErrorComponent";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Filler,
  Legend
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
      display: false,
    },
    y: {
      display: true,
    },
  },
  elements: {
    line: {
      borderWidth: 2,
    },
  },
};

const symbolsByType = {
  FX: [
    { symbol: "dollar", label: "달러" },
    { symbol: "yen", label: "엔" },
    { symbol: "DOW", label: "다우존스" },
    { symbol: "NQ", label: "나스닥" },
    { symbol: "SP500", label: "S&P 500" },
    { symbol: "Nikkei", label: "니케이" },
    { symbol: "HangSeng", label: "항셍" },
    { symbol: "ShangHai", label: "상하이" },
  ],
  Feed: [
    { symbol: "GOLD", label: "금" },
    { symbol: "SILVER", label: "은" },
    { symbol: "WTI", label: "WTI유" },
    { symbol: "CORN", label: "옥수수" },
    { symbol: "COFFEE", label: "커피" },
    { symbol: "COTTON", label: "면화" },
  ],
  Bonds: [
    { symbol: "KRBONDS1", label: "한국 국채 1년" },
    { symbol: "KRBONDS3", label: "한국 국채 3년" },
    { symbol: "KRBONDS5", label: "한국 국채 5년" },
    { symbol: "KRBONDS10", label: "한국 국채 10년" },
    { symbol: "KRBONDS20", label: "한국 국채 20년" },
    { symbol: "KRBONDS30", label: "한국 국채 30년" },
    { symbol: "USBONDS1", label: "미국 국채 1년" },
    { symbol: "USBONDS10", label: "미국 국채 10년" },
  ],
};

type TypeKey = keyof typeof symbolsByType;

interface ForexData {
  changePrice: string; // "12.5000",
  changeSign: string; // "2",
  changeRate: string; // "0.92",
  prevPrice: string; // "1357.1000",
  highPrice: string; // "1373.0000",
  lowPrice: string; // "1352.0000",
  openPrice: string; // "1355.0000",
  currentPrice: string; // "1369.6000",
  pastInfo: {
    stck_bsop_date: string; // "20250613",
    ovrs_nmix_prpr: string; // "1369.6000",
    ovrs_nmix_oprc: string; // "1355.0000",
    ovrs_nmix_hgpr: string; // "1373.0000",
    ovrs_nmix_lwpr: string; // "1352.0000"
  }[];
}

const fetchForexData = async (
  type: TypeKey,
  symbol: string
): Promise<ForexData> => {
  const res = await fetch(`/proxy2/v2/stocks/FX?type=${type}&symbol=${symbol}`);
  if (!res.ok) throw new Error("데이터 로딩 실패");
  const json = await res.json();
  return json.data;
};

export default function OverViewChart() {
  const [type, setType] = useState<TypeKey>("FX");
  const [symbol, setSymbol] = useState<string>(symbolsByType["FX"][0].symbol);

  const {
    data: forexData,
    error,
    isLoading,
  } = useQuery({
    queryKey: ["forexData", type, symbol],
    queryFn: () => fetchForexData(type, symbol),
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
    placeholderData: keepPreviousData,
    retry: 1,
  });

  // 타입 변경 시 첫 번째 심볼로 설정
  const handleTypeChange = (newType: TypeKey) => {
    setType(newType);
    setSymbol(symbolsByType[newType][0].symbol);
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

  if (error) {
    return (
      <div className="p-main-2 bg-white shadow-md rounded-main space-y-4">
        <ErrorComponent
          message={error.message || "데이터를 불러오는데 실패했습니다."}
          className="min-h-[200px]"
        />
      </div>
    );
  }

  if (!forexData) return null;

  const labels = forexData.pastInfo
    .slice()
    .reverse()
    .map((item) => item.stck_bsop_date);

  const data = {
    labels,
    datasets: [
      {
        fill: true,
        data: forexData.pastInfo
          .slice()
          .reverse()
          .map((item) => item.ovrs_nmix_prpr),
        borderColor:
          forexData.changeSign === "1" || forexData.changeSign === "2"
            ? "#f04251"
            : forexData.changeSign === "4" || forexData.changeSign === "5"
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

          if (forexData.changeSign === "1" || forexData.changeSign === "2") {
            gradient.addColorStop(0, "rgba(240, 66, 81, 0.5)");
            gradient.addColorStop(1, "rgba(240, 66, 81, 0)");
          } else if (
            forexData.changeSign === "4" ||
            forexData.changeSign === "5"
          ) {
            gradient.addColorStop(0, "rgba(52, 133, 250, 0.5)");
            gradient.addColorStop(1, "rgba(52, 133, 250, 0)");
          }

          return gradient;
        },
        tension: 0.1,
        pointRadius: 0,
        borderWidth: 1,
        pointHoverRadius: 0,
      },
    ],
  };

  return (
    <div className="p-main-2 bg-white shadow-md rounded-main space-y-4">
      <div className="flex gap-[5px]">
        {(Object.keys(symbolsByType) as TypeKey[]).map((t) => (
          <button
            key={t}
            onClick={() => handleTypeChange(t)}
            className={`px-2 py-1 rounded-full text-sm-custom hover:bg-main-blue/10 transition-colors duration-300 ${
              type === t ? "text-main-blue font-semibold" : "text-main-blue/50"
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      <div>
        <Line options={options} data={data} plugins={[verticalLinePlugin]} />
      </div>

      <div className="flex flex-wrap gap-2">
        {symbolsByType[type].map((s) => (
          <button
            key={s.symbol}
            onClick={() => setSymbol(s.symbol)}
            className={`w-fit px-main py-1 rounded-main text-sm-custom transition-colors duration-300 ${
              symbol === s.symbol
                ? "bg-main-blue text-white"
                : "bg-main-blue/10 text-main-blue hover:bg-main-blue/20"
            }`}
          >
            {s.label}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-[auto_1fr_auto_1fr] gap-x-main gap-y-2 text-sm-custom">
        <div className="text-gray-500">현재가</div>
        <span>{Number(forexData.currentPrice).toLocaleString()}</span>

        <div className="text-gray-500">전일가</div>
        <span>{Number(forexData.prevPrice).toLocaleString()}</span>

        <div className="text-gray-500">시가</div>
        <span>{Number(forexData.openPrice).toLocaleString()}</span>

        <div className="text-gray-500">고가</div>
        <div className="font-semibold text-red-600">
          {Number(forexData.highPrice).toLocaleString()}
        </div>

        <div className="text-gray-500">저가</div>
        <div className="font-semibold text-blue-600">
          {Number(forexData.lowPrice).toLocaleString()}
        </div>

        <div className="text-gray-500">변동</div>
        {(forexData.changeSign === "1" || forexData.changeSign === "2") && (
          <UpPrice
            change={(
              Number(forexData.currentPrice) - Number(forexData.prevPrice)
            ).toFixed(2)}
            changeRate={Number(forexData.changeRate)}
          />
        )}
        {(forexData.changeSign === "4" || forexData.changeSign === "5") && (
          <DownPrice
            change={(
              Number(forexData.currentPrice) - Number(forexData.prevPrice)
            ).toFixed(2)}
            changeRate={Number(forexData.changeRate)}
          />
        )}
      </div>
    </div>
  );
}
