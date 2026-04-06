import { create } from "zustand";

export interface RecentViewStock {
  stockImage: string;
  stockCode: string;
  stockName: string;
}

interface RecentViewState {
  recentViewStocks: RecentViewStock[];
  setRecentViewStocks: (stocks: RecentViewStock[]) => void;
}

export const useRecentViewStore = create<RecentViewState>((set) => ({
  recentViewStocks:
    typeof window !== "undefined" && localStorage.getItem("latestViewStocks")
      ? JSON.parse(localStorage.getItem("latestViewStocks") || "[]").filter(
          (stock: any) => stock && stock.stockCode && stock.stockName
        )
      : [],
  setRecentViewStocks: (stocks) => {
    // 유효한 데이터만 필터링
    const validStocks = stocks.filter(
      (stock) => stock && stock.stockCode && stock.stockName
    );

    if (validStocks.length > 10) {
      validStocks.pop();
    }
    set({ recentViewStocks: validStocks });
    if (typeof window !== "undefined") {
      localStorage.setItem("latestViewStocks", JSON.stringify(validStocks));
    }
  },
}));
