export type FuturesChartHistoryStatus = "missing" | "ready" | "stale";

export type FuturesChartRenderMode =
  | "candles"
  | "live-fallback"
  | "loading-empty";

type RenderModeInput = {
  hasCandleData: boolean;
  hasQueryData: boolean;
  historyStatus: FuturesChartHistoryStatus;
  isError: boolean;
  isLoading: boolean;
};

export function getFuturesChartRenderMode({
  hasCandleData,
  hasQueryData,
  isError,
  isLoading,
}: RenderModeInput): FuturesChartRenderMode {
  if (hasCandleData) {
    return "candles";
  }

  if (!hasQueryData && isLoading && !isError) {
    return "loading-empty";
  }

  return "live-fallback";
}

type StatusInput = {
  hasNextPage: boolean | undefined;
  historyStatus: FuturesChartHistoryStatus;
  renderMode: FuturesChartRenderMode;
};

export function getFuturesChartStatus({
  hasNextPage,
  historyStatus,
  renderMode,
}: StatusInput): string {
  if (renderMode === "candles") {
    return hasNextPage ? "차트 준비됨" : "전체 히스토리";
  }

  if (renderMode === "loading-empty") {
    return "히스토리 로딩 중";
  }

  if (historyStatus === "stale") {
    return "오래된 히스토리 차단";
  }

  return "실시간 라인 셸";
}
