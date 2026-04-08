import {
  ACTIVE_STOCK_SET_SOURCES,
  ActiveStockSetSource,
} from "@/api/stocks";
import { create } from "zustand";

type ActiveStockSets = Record<ActiveStockSetSource, string[]>;

interface ActiveStockSetState {
  sourceStocks: ActiveStockSets;
  setSourceStocks: (source: ActiveStockSetSource, stockCodes: string[]) => void;
  clearSourceStocks: (source: ActiveStockSetSource) => void;
}

const emptySourceStocks = ACTIVE_STOCK_SET_SOURCES.reduce(
  (acc, source) => {
    acc[source] = [];
    return acc;
  },
  {} as ActiveStockSets
);

const normalizeStockCodes = (stockCodes: string[]) =>
  Array.from(
    new Set(
      stockCodes
        .map((stockCode) => stockCode?.trim())
        .filter((stockCode): stockCode is string => Boolean(stockCode))
    )
  );

export const useActiveStockSetStore = create<ActiveStockSetState>((set) => ({
  sourceStocks: emptySourceStocks,
  setSourceStocks: (source, stockCodes) =>
    set((state) => ({
      sourceStocks: {
        ...state.sourceStocks,
        [source]: normalizeStockCodes(stockCodes),
      },
    })),
  clearSourceStocks: (source) =>
    set((state) => ({
      sourceStocks: {
        ...state.sourceStocks,
        [source]: [],
      },
    })),
}));
