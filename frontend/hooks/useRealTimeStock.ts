import { useQuery } from "@tanstack/react-query";

interface RealTimeStockData {
  stockCode: string;
  stockName: string;
  categoryName: string;
  price: string;
  openPrice: string;
  highPrice: string;
  lowPrice: string;
  marketName: string;
  changeAmount: string;
  sign: string;
  changeRate: string;
  volume: string;
  volumeValue: string;
  stockImage: string;
}

// 시장 오픈 여부 체크
function isMarketOpen(now: Date = new Date()) {
  const dayOfWeek = now.getDay(); // 0=일요일, 6=토요일
  if (dayOfWeek === 0 || dayOfWeek === 6) return false; // 주말

  const hour = now.getHours();
  const minute = now.getMinutes();
  const currentTime = hour * 100 + minute;

  return currentTime >= 900 && currentTime <= 1530; // 09:00 ~ 15:30
}

export const useRealTimeStock = (stockCode: string) => {
  const marketOpen = isMarketOpen();

  return useQuery({
    queryKey: ["realTimeStock", stockCode],
    queryFn: async () => {
      const res = await fetch(`/proxy2/v2/stocks/info/${stockCode}`);
      if (!res.ok)
        throw new Error("실시간 주식 정보를 불러오는데 실패했습니다.");
      const json: { data: RealTimeStockData } = await res.json();
      return json.data;
    },
    enabled: !!stockCode,
    refetchInterval: marketOpen ? 20000 : false, // 시장 오픈 시에만 20초마다 폴링
    refetchIntervalInBackground: false,
    staleTime: 15000, // 15초 동안 fresh 상태 유지
    gcTime: 5 * 60 * 1000, // 5분
  });
};

export type { RealTimeStockData };
