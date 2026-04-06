"use client";

import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  ChartData,
  ChartOptions,
  TooltipItem,
} from "chart.js";
import { Doughnut } from "react-chartjs-2";
import { useEffect, useRef, useState } from "react";
import { ChevronRight } from "lucide-react";
import InvestmentStyleModal from "./InvestmentStyleModal";
import { JwtToken } from "@/type/jwt";
import { usePortfolioStore } from "@/store/usePortfolio";

ChartJS.register(ArcElement, Tooltip, Legend);

const GaugeChart = ({ token }: { token: JwtToken | null }) => {
  const ref = useRef<any>(null);
  const [isOpenInvestmentStyleModal, setIsOpenInvestmentStyleModal] =
    useState(false);
  const { portfolio } = usePortfolioStore();
  const [investScore, setInvestScore] = useState(0);

  useEffect(() => {
    if (!token) return;

    setInvestScore(token.invest);
  }, [token, portfolio]);

  const dummyData: ChartData<"doughnut"> = {
    labels: ["안전형", "안정추구형", "위험중립형", "적극투자형", "공격투자형"],
    datasets: [
      {
        data: [20, 20, 20, 20, 20], // 총 100 맞추기
        backgroundColor: [
          "rgba(59, 130, 246, 0.25)", // 안전형 - 파란색
          "rgba(34, 197, 94, 0.25)", // 안정추구형 - 초록색
          "rgba(234, 179, 8, 0.25)", // 위험중립형 - 노란색
          "rgba(249, 115, 22, 0.25)", // 적극투자형 - 주황색
          "rgba(239, 68, 68, 0.25)", // 공격투자형 - 빨간색
        ],
        borderColor: [
          "rgba(59, 130, 246, 1)", // 안전형 - 파란색
          "rgba(34, 197, 94, 1)", // 안정추구형 - 초록색
          "rgba(234, 179, 8, 1)", // 위험중립형 - 노란색
          "rgba(249, 115, 22, 1)", // 적극투자형 - 주황색
          "rgba(239, 68, 68, 1)", // 공격투자형 - 빨간색
        ],
        borderWidth: 0.2,
        borderAlign: "center", // 내부 중심선 기준
        // spacing: 10, // 조각 간 간격
        // borderRadius: 5, // 둥근 모서리
        circumference: 180,
        rotation: 270,
      },
    ],
  };

  const options: ChartOptions<"doughnut"> = {
    responsive: true,
    maintainAspectRatio: true,
    cutout: "65%",
    plugins: {
      legend: {
        display: true,
        position: "bottom",
        labels: {
          boxWidth: 10,
          usePointStyle: true,
          pointStyle: "rectRounded",
        },
      },
      tooltip: {
        callbacks: {
          label: function (context: TooltipItem<"doughnut">) {
            return `${context.label}`;
          },
        },
      },
    },
  };

  useEffect(() => {
    const chart = ref.current;
    if (!chart) return;

    let value = 0;
    if (investScore === 1) value = 5; // 안전형
    else if (investScore === 2) value = 28; // 안정추구형
    else if (investScore === 3) value = 50; // 위험중립형
    else if (investScore === 4) value = 72; // 적극투자형
    else if (investScore === 5) value = 95; // 공격투자형
    else value = 0; // 기본값 (설정되지 않음)

    const needle = {
      id: "needle",
      afterDatasetDraw(chart: any) {
        const {
          ctx,
          chartArea: { top, left, width, height },
        } = chart;

        const angle = (Math.PI * value) / 100;
        const cx = left + width / 2;
        const cy = top + height / 1.3;
        const radius = height * 0.5;

        const dx = radius * Math.cos(angle + Math.PI);
        const dy = radius * Math.sin(angle + Math.PI);

        const baseWidth = 5;
        const baseAngle = angle + Math.PI / 2;

        const x1 = -baseWidth * Math.cos(baseAngle);
        const y1 = -baseWidth * Math.sin(baseAngle);
        const x2 = baseWidth * Math.cos(baseAngle);
        const y2 = baseWidth * Math.sin(baseAngle);

        const tipX = dx;
        const tipY = dy;

        ctx.save();
        ctx.translate(cx, cy);
        ctx.fillStyle = "#707070";
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.lineTo(tipX, tipY);
        ctx.closePath();
        ctx.fill();
        ctx.restore();

        ctx.save();
        ctx.beginPath();
        ctx.fillStyle = "#707070";
        ctx.arc(cx, cy, 5, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
      },
    };

    // plugins 수정
    const plugins = chart.config.plugins as any[];
    const index = plugins.findIndex((p) => p.id === "needle");
    if (index !== -1) plugins.splice(index, 1);
    plugins.push(needle);

    chart.update();
  }, [investScore]);

  if (token && investScore === 0)
    return (
      <>
        <div className="size-full flex flex-col gap-main">
          <div className="flex justify-between items-end">
            <h2 className="text-2xl-custom font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent w-fit">
              내 투자성향
            </h2>

            <button
              id="investment-style"
              className="text-sm-custom text-main-dark-gray hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main pl-2 pr-1 py-1 flex items-center gap-1"
              onClick={() => setIsOpenInvestmentStyleModal(true)}
            >
              <span>투자성향 변경</span>
              <ChevronRight size={16} />
            </button>
          </div>

          <div className="relative">
            <div className="absolute inset-0 bg-white/50 z-20 size-full flex items-center justify-center">
              <span className="text-main-dark-gray font-semibold">
                투자성향을 설정해주세요.
              </span>
            </div>

            <div className="blur-xs h-full ">
              <div className="w-full h-full flex justify-center items-center flex-1 relative">
                <Doughnut
                  ref={ref}
                  data={dummyData}
                  options={options}
                  className="w-full"
                />
              </div>
            </div>
          </div>
        </div>
        <InvestmentStyleModal
          isOpen={isOpenInvestmentStyleModal}
          onClose={() => setIsOpenInvestmentStyleModal(false)}
          setInvestScore={setInvestScore}
          token={token}
        />
      </>
    );

  return (
    <>
      <div className="size-full flex flex-col gap-main">
        <div className="flex justify-between items-end">
          <h2 className="text-2xl-custom font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent w-fit">
            내 투자성향
          </h2>

          <button
            id="investment-style"
            className="text-sm-custom text-main-dark-gray hover:text-main-blue hover:bg-main-blue/10 transition-all duration-300 rounded-main pl-2 pr-1 py-1 flex items-center gap-1"
            onClick={() => setIsOpenInvestmentStyleModal(true)}
          >
            <span>투자성향 변경</span>
            <ChevronRight size={16} />
          </button>
        </div>

        <div className="w-full flex justify-center items-center flex-1">
          <Doughnut
            ref={ref}
            data={dummyData}
            options={options}
            className="w-full"
          />
        </div>
      </div>
      <InvestmentStyleModal
        isOpen={isOpenInvestmentStyleModal}
        onClose={() => setIsOpenInvestmentStyleModal(false)}
        setInvestScore={setInvestScore}
        token={token}
      />
    </>
  );
};

export default GaugeChart;
