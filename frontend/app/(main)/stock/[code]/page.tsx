"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useRealTimeStock } from "@/hooks/useRealTimeStock";
import IntervalSelector, {
  IntervalKey,
} from "@/components/router/(main)/stock/[code]/IntervalSelector";
import StockChart from "@/components/router/(main)/stock/[code]/StockChart";
import StockHeader from "@/components/router/(main)/stock/[code]/StockHeader";

const StockDetailPage = () => {
  const params = useParams<{ code: string }>();
  const code = params!.code;
  const [selectedInterval, setSelectedInterval] = useState<IntervalKey>("D");

  // 실시간 주식 데이터
  const { data: realTimeStock } = useRealTimeStock(code);

  return (
    <div className="grid grid-cols-3 gap-main-2">
      {/* 주식 헤더 */}
      <div className="col-span-3 flex gap-main">
        <StockHeader code={code} realTimeStock={realTimeStock} />
      </div>

      {/* 기간 선택 버튼 */}
      <div className="col-span-3">
        <IntervalSelector
          selectedInterval={selectedInterval}
          onIntervalChange={setSelectedInterval}
        />
      </div>

      {/* 차트 */}
      <div className="col-span-3 flex flex-col size-full gap-main">
        <StockChart
          code={code}
          selectedInterval={selectedInterval}
          realTimeData={realTimeStock}
        />
      </div>
    </div>
  );
};

export default StockDetailPage;
