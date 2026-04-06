"use client";

import { JwtToken } from "@/type/jwt";
import { usePortfolioStore } from "@/store/usePortfolio";
import { useEffect } from "react";
import { driver } from "driver.js";

const CheckPortfolio = ({ token }: { token: JwtToken | null }) => {
  const { portfolio, isLoading } = usePortfolioStore();
  const investScore = token?.invest;

  useEffect(() => {
    if (!token) return;
    if (isLoading) return; // 로딩 중이면 실행하지 않음
    if (!portfolio) return;

    const steps = [];

    if (investScore === 0) {
      steps.push({
        element: "#investment-style",
        popover: {
          title: "투자성향 설정",
          description: "설문을 통해 본인의 투자성향을 설정해주세요",
        },
      });
    }

    if (portfolio.length === 0) {
      steps.push({
        element: "#add-holding",
        popover: {
          title: "보유 종목 추가",
          description: "갖고 계신 보유 종목을 추가해주세요",
        },
      });
    }

    const driverObj = driver({
      showProgress: true,
      steps,
      stagePadding: 5,
      allowClose: false,
      nextBtnText: "다음",
      prevBtnText: "이전",
      doneBtnText: "완료",
    });

    driverObj.drive();

    return () => driverObj.destroy();
  }, [portfolio, investScore, isLoading]);

  return null;
};

export default CheckPortfolio;
