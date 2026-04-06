import React from "react";
import Holidings from "@/components/router/portfolio/Holidings";
import ProfitLossCalendar from "@/components/router/portfolio/ProfitLossCalendar";
import MyAccountChart from "@/components/router/portfolio/MyAccountChart";
import { getJwtToken } from "@/utils/auth";
import GaugeChart from "@/components/router/portfolio/GaugeChart";
import CheckPortfolio from "@/components/router/portfolio/CheckPortfolio";

const MyPortfolioPage = async () => {
  const token = await getJwtToken();

  return (
    <div className="grid grid-cols-10 gap-main-2 max-w-[1200px] mx-auto min-w-[1000px] pb-[100px]">
      <CheckPortfolio token={token} />
      <div className="col-span-7 flex flex-col gap-main box-content p-main-2 bg-white rounded-main shadow-sm">
        <h2 className="text-2xl-custom font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent w-fit">
          내 계좌
        </h2>
        <MyAccountChart token={token} />
      </div>

      <div className="col-span-3 gap-main box-content p-main-2 bg-white rounded-main shadow-sm">
        <GaugeChart token={token} />
      </div>

      <div className="col-span-10 grid grid-cols-5 gap-main-2">
        <div className="col-span-2 box-content p-main-2 bg-white rounded-main shadow-sm">
          <Holidings token={token} />
        </div>

        <div className="col-span-3 relative w-full">
          <div className="sticky top-0 bg-white rounded-main p-main-2 shadow-sm">
            <h2 className="text-2xl-custom font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent w-fit">
              날짜별 손익
            </h2>
            <ProfitLossCalendar token={token} />
          </div>
        </div>
      </div>
    </div>
  );
};

export default MyPortfolioPage;
