"use client";

import {
  ACTIVE_STOCK_SET_SOURCES,
  ActiveStockSetSource,
  publishActiveStockSet,
} from "@/api/stocks";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useRef } from "react";
import { useActiveStockSetStore } from "@/store/useActiveStockSetStore";
import { usePortfolioStore } from "@/store/usePortfolio";
import { JwtToken } from "@/type/jwt";

type InterestGroupResponse = {
  groupId: string;
  main: boolean;
};

type InterestStockResponse = {
  stockInfo: {
    stockCode: string;
  };
};

const areSameCodes = (prev: string[], next: string[]) =>
  prev.length === next.length && prev.every((code, idx) => code === next[idx]);

const extractDetailStockCode = (pathname: string | null): string | null => {
  if (!pathname) return null;
  const match = pathname.match(/^\/stock\/([^/]+)$/);
  return match ? decodeURIComponent(match[1]) : null;
};

export default function ActiveStockRequestCoordinator({
  token,
}: {
  token: JwtToken | null;
}) {
  const pathname = usePathname();
  const detailStockCode = extractDetailStockCode(pathname);

  const { portfolio } = usePortfolioStore();
  const { sourceStocks, setSourceStocks } = useActiveStockSetStore();
  const lastPublishedRef = useRef<Record<ActiveStockSetSource, string[]>>(
    ACTIVE_STOCK_SET_SOURCES.reduce((acc, source) => {
      acc[source] = [];
      return acc;
    }, {} as Record<ActiveStockSetSource, string[]>)
  );

  useEffect(() => {
    if (!token) {
      setSourceStocks("portfolio", []);
      return;
    }
    setSourceStocks(
      "portfolio",
      portfolio.map((stock) => stock.stockCode)
    );
  }, [portfolio, setSourceStocks, token]);

  useEffect(() => {
    setSourceStocks("stock-detail", detailStockCode ? [detailStockCode] : []);
  }, [detailStockCode, setSourceStocks]);

  useEffect(() => {
    let cancelled = false;

    const loadMainInterestStocks = async () => {
      if (!token) {
        setSourceStocks("interest-main", []);
        return;
      }

      try {
        const groupsResponse = await fetch(`/proxy/favorite/${token.memberId}`);
        if (!groupsResponse.ok) {
          if (!cancelled) setSourceStocks("interest-main", []);
          return;
        }

        const groupsJson: { data: InterestGroupResponse[] } =
          await groupsResponse.json();
        const groups = groupsJson.data ?? [];
        const mainGroup = groups.find((group) => group.main) ?? groups[0];

        if (!mainGroup) {
          if (!cancelled) setSourceStocks("interest-main", []);
          return;
        }

        const stocksResponse = await fetch(
          `/proxy/favorite/${token.memberId}/${mainGroup.groupId}`
        );
        if (!stocksResponse.ok) {
          if (!cancelled) setSourceStocks("interest-main", []);
          return;
        }

        const stocks: InterestStockResponse[] = await stocksResponse.json();
        if (!cancelled) {
          setSourceStocks(
            "interest-main",
            stocks.map((stock) => stock.stockInfo.stockCode)
          );
        }
      } catch {
        if (!cancelled) {
          setSourceStocks("interest-main", []);
        }
      }
    };

    void loadMainInterestStocks();

    return () => {
      cancelled = true;
    };
  }, [setSourceStocks, token]);

  const snapshots = useMemo(
    () =>
      ACTIVE_STOCK_SET_SOURCES.map((source) => ({
        source,
        stockCodes: sourceStocks[source],
      })),
    [sourceStocks]
  );

  useEffect(() => {
    const changedSnapshots = snapshots.filter(({ source, stockCodes }) => {
      const previous = lastPublishedRef.current[source] ?? [];
      return !areSameCodes(previous, stockCodes);
    });

    if (changedSnapshots.length === 0) return;

    const publish = async () => {
      await Promise.all(
        changedSnapshots.map(async ({ source, stockCodes }) => {
          try {
            await publishActiveStockSet(source, stockCodes);
            lastPublishedRef.current[source] = stockCodes;
          } catch {
            // Ignore transient failures; next snapshot change will retry.
          }
        })
      );
    };

    void publish();
  }, [snapshots]);

  return null;
}
