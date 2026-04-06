import React from "react";
import KOSPIChart from "@/components/router/(main)/stock/KOSPIChart";
import KOSDAQChart from "@/components/router/(main)/stock/KOSDAQChart";
import PopularStock from "@/components/router/(main)/stock/PopularStock";
import CategoryStock from "@/components/router/(main)/stock/CategoryStock";
import { getJwtToken } from "@/utils/auth";
import OverViewChart from "@/components/router/(main)/stock/OverViewChart";

const StockPage = async () => {
  const token = await getJwtToken();

  return (
    <div className="grid grid-cols-3 gap-main">
      <KOSPIChart />

      <KOSDAQChart />

      <div className="row-span-8 relative">
        <div className="flex flex-col gap-main p-main sticky top-0">
          <OverViewChart />
        </div>
      </div>

      <div className="col-span-2 row-span-1">
        <PopularStock token={token} />
      </div>

      <div className="col-span-2 row-span-2">
        <CategoryStock token={token} />
      </div>
    </div>
  );
};

export default StockPage;
