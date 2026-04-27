"use client";

import CancelOrderButton from "@/components/futures/CancelOrderButton";
import ClosePositionButton from "@/components/futures/ClosePositionButton";
import FuturesPriceChart from "@/components/futures/FuturesPriceChart";
import OrderEntryPanel from "@/components/futures/OrderEntryPanel";
import type {
  FuturesAccountSummary,
  FuturesOpenOrder,
  FuturesOrderHistory,
  FuturesPosition,
  FuturesPositionHistory,
  FuturesTradingExecutionEvent,
  MarketApiResponse,
} from "@/lib/futures-api";
import { formatFundingCountdown } from "@/lib/funding-countdown";
import {
  formatCompactUsd,
  formatPercent,
  formatSignedUsd,
  formatUsd,
  type MarketSnapshot,
} from "@/lib/markets";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { toast } from "react-toastify";

type Props = {
  initialMarket: MarketSnapshot;
  isAuthenticated: boolean;
  accountSummary: FuturesAccountSummary | null;
  chartOpenOrders: FuturesOpenOrder[];
  chartPositions: FuturesPosition[];
  openOrders: FuturesOpenOrder[];
  positions: FuturesPosition[];
  positionHistory: FuturesPositionHistory[];
  orderHistory: FuturesOrderHistory[];
};

type TradingTab =
  | "POSITIONS"
  | "POSITION_HISTORY"
  | "OPEN_ORDERS"
  | "ORDER_HISTORY";

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export default function MarketDetailRealtimeView({
  initialMarket,
  isAuthenticated,
  accountSummary,
  chartOpenOrders,
  chartPositions,
  openOrders,
  positions,
  positionHistory,
  orderHistory,
}: Props) {
  const router = useRouter();
  const [market, setMarket] = useState(initialMarket);
  const [marketUpdatedAt, setMarketUpdatedAt] = useState(() => Date.now());
  const [fundingCountdownNow, setFundingCountdownNow] = useState(() =>
    Date.now()
  );
  const [activeTab, setActiveTab] = useState<TradingTab>("POSITIONS");
  const [executionEvents, setExecutionEvents] = useState<
    FuturesTradingExecutionEvent[]
  >([]);

  useEffect(() => {
    const stream = new EventSource(
      `/api/futures/markets/${encodeURIComponent(initialMarket.symbol)}/stream`
    );

    stream.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as MarketApiResponse;
        const receivedAt = Date.now();

        setMarket((current) => ({
          ...current,
          displayName: data.displayName,
          lastPrice: data.lastPrice,
          markPrice: data.markPrice,
          indexPrice: data.indexPrice,
          fundingRate: data.fundingRate,
          nextFundingAt: data.nextFundingAt ?? current.nextFundingAt,
          fundingIntervalHours:
            data.fundingIntervalHours ?? current.fundingIntervalHours,
          serverTime: data.serverTime ?? current.serverTime,
          change24h: data.change24h,
        }));
        setMarketUpdatedAt(receivedAt);
        setFundingCountdownNow(receivedAt);
      } catch {
        // Keep the last known snapshot when the stream sends malformed data.
      }
    };

    return () => {
      stream.close();
    };
  }, [initialMarket.symbol]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }

    const stream = new EventSource("/api/futures/orders/stream");

    stream.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as FuturesTradingExecutionEvent;

        if (data.symbol !== initialMarket.symbol) {
          return;
        }

        setExecutionEvents((current) => [data, ...current].slice(0, 4));
        setActiveTab(data.type === "ORDER_FILLED" ? "ORDER_HISTORY" : "POSITIONS");
        router.refresh();
      } catch {
        // Keep the stream alive when a malformed event slips through.
      }
    };

    return () => {
      stream.close();
    };
  }, [initialMarket.symbol, isAuthenticated, router]);

  useEffect(() => {
    if (!market.nextFundingAt) {
      return;
    }

    const timer = window.setInterval(() => {
      setFundingCountdownNow(Date.now());
    }, 1000);

    return () => {
      window.clearInterval(timer);
    };
  }, [market.nextFundingAt]);

  const unrealizedPnl = useMemo(
    () => positions.reduce((sum, position) => sum + position.unrealizedPnl, 0),
    [positions]
  );
  const fundingCountdown = useMemo(
    () =>
      formatFundingCountdown(
        market.nextFundingAt,
        fundingCountdownNow,
        market.serverTime,
        marketUpdatedAt
      ),
    [fundingCountdownNow, market.nextFundingAt, market.serverTime, marketUpdatedAt]
  );

  return (
    <div className="flex w-full flex-col gap-main-2 px-main-2 pb-24">
      <section className="grid w-full grid-cols-[minmax(0,1fr)_360px] gap-main-2 pt-4">
        <div className="flex min-w-0 flex-col gap-main-2">
          <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
            <div className="flex items-start justify-between gap-main">
              <div className="min-w-0">
                <p className="text-xs-custom uppercase text-main-dark-gray/50">
                  {market.symbol}
                </p>
                <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
                  {market.displayName}
                </h1>
                <p className="mt-3 max-w-3xl text-sm-custom text-main-dark-gray/70 break-keep">
                  차트, 주문, 포지션, 미체결 주문을 한 화면에서 처리하는 메인
                  트레이딩 화면입니다.
                </p>
              </div>
              <div
                className={[
                  "rounded-main px-main py-3 text-right",
                  market.change24h >= 0
                    ? "bg-emerald-50 text-emerald-600"
                    : "bg-rose-50 text-rose-600",
                ].join(" ")}
              >
                <p className="text-xs-custom font-semibold uppercase">24H</p>
                <p className="mt-2 text-lg-custom font-bold">
                  {formatPercent(market.change24h)}
                </p>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-5 gap-main">
              <Stat
                label="최신 체결가"
                tone={market.change24h >= 0 ? "positive" : "negative"}
                value={formatUsd(market.lastPrice)}
              />
              <Stat label="Mark Price" value={formatUsd(market.markPrice)} />
              <Stat label="Index Price" value={formatUsd(market.indexPrice)} />
              <Stat
                label="Funding"
                tone={market.fundingRate >= 0 ? "positive" : "negative"}
                value={formatPercent(market.fundingRate * 100)}
                subValue={`Next ${fundingCountdown}`}
              />
              <Stat label="24h 거래량" value={formatCompactUsd(market.volume24h)} />
            </div>
          </div>

          <FuturesPriceChart
            change24h={market.change24h}
            currentPrice={market.lastPrice}
            currentPriceUpdatedAt={marketUpdatedAt}
            openOrders={chartOpenOrders}
            positions={chartPositions}
            symbol={market.symbol}
          />

          <ExecutionEventPanel events={executionEvents} />

          <TradingBlotter
            activeTab={activeTab}
            onTabChange={setActiveTab}
            openOrders={openOrders}
            orderHistory={orderHistory}
            positions={positions}
            positionHistory={positionHistory}
            unrealizedPnl={unrealizedPnl}
          />
        </div>

        <aside
          className={[
            "sticky top-4 h-fit rounded-main border border-main-light-gray",
            "bg-white p-main-2 shadow-sm",
          ].join(" ")}
        >
          <div className="mb-5 flex items-start justify-between gap-main">
            <div>
              <p className="text-sm-custom font-semibold text-main-dark-gray">
                주문
              </p>
              <p className="mt-1 text-xs-custom text-main-dark-gray/55">
                {market.symbol} Perpetual
              </p>
            </div>
            <span
              className={[
                "rounded-full px-2.5 py-1 text-xs-custom font-bold",
                market.change24h >= 0
                  ? "bg-emerald-50 text-emerald-600"
                  : "bg-rose-50 text-rose-600",
              ].join(" ")}
            >
              {formatPercent(market.change24h)}
            </span>
          </div>

          <OrderEntryPanel
            accountSummary={accountSummary}
            currentPrice={market.lastPrice}
            isAuthenticated={isAuthenticated}
            symbol={market.symbol}
          />
        </aside>
      </section>
    </div>
  );
}

function ExecutionEventPanel({
  events,
}: {
  events: FuturesTradingExecutionEvent[];
}) {
  if (events.length === 0) {
    return null;
  }

  return (
    <section>
      <div className="flex flex-col gap-3">
        {events.map((event, index) => {
          const liquidation = event.type === "POSITION_LIQUIDATED";
          return (
            <div
              className={[
                "flex items-center justify-between gap-main rounded-main border px-main py-3",
                liquidation
                  ? "border-rose-100 bg-rose-50 text-rose-700"
                  : "border-emerald-100 bg-emerald-50 text-emerald-700",
              ].join(" ")}
              key={`${event.type}-${event.orderId ?? "position"}-${index}`}
            >
              <div className="min-w-0">
                <p className="text-sm-custom font-bold">{event.message}</p>
                <p className="mt-1 text-xs-custom opacity-75">
                  {event.symbol} · {event.positionSide} · {event.marginMode}
                </p>
              </div>
              <div className="shrink-0 text-right">
                <p className="text-sm-custom font-bold">
                  {formatUsd(event.executionPrice)}
                </p>
                <p className="mt-1 text-xs-custom opacity-75">
                  {event.quantity.toFixed(3)}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function TradingBlotter({
  activeTab,
  onTabChange,
  openOrders,
  orderHistory,
  positions,
  positionHistory,
  unrealizedPnl,
}: {
  activeTab: TradingTab;
  onTabChange: (tab: TradingTab) => void;
  openOrders: FuturesOpenOrder[];
  orderHistory: FuturesOrderHistory[];
  positions: FuturesPosition[];
  positionHistory: FuturesPositionHistory[];
  unrealizedPnl: number;
}) {
  const tabs: Array<{ label: string; value: TradingTab; count: number }> = [
    { label: "포지션", value: "POSITIONS", count: positions.length },
    {
      label: "포지션 히스토리",
      value: "POSITION_HISTORY",
      count: positionHistory.length,
    },
    { label: "Open orders", value: "OPEN_ORDERS", count: openOrders.length },
    { label: "Order history", value: "ORDER_HISTORY", count: orderHistory.length },
  ];

  return (
    <section className="rounded-main border border-main-light-gray bg-white shadow-sm">
      <div
        className={[
          "flex items-center justify-between gap-main border-b",
          "border-main-light-gray px-main-2 pt-4",
        ].join(" ")}
      >
        <div className="flex flex-wrap gap-6">
          {tabs.map((tab) => {
            const active = tab.value === activeTab;
            return (
              <button
                className={[
                  "border-b-2 pb-3 text-sm-custom font-semibold transition-colors",
                  active
                    ? "border-main-dark-gray text-main-dark-gray"
                    : "border-transparent text-main-dark-gray/45 hover:text-main-dark-gray",
                ].join(" ")}
                key={tab.value}
                onClick={() => onTabChange(tab.value)}
                type="button"
              >
                {tab.label} ({tab.count})
              </button>
            );
          })}
        </div>
        <p className="pb-3 text-xs-custom font-semibold text-main-dark-gray/60">
          미실현 손익 {formatSignedUsd(unrealizedPnl)}
        </p>
      </div>

      <div className="min-h-[340px] px-main-2 py-4">
        {activeTab === "POSITIONS" && <PositionsTable positions={positions} />}
        {activeTab === "POSITION_HISTORY" && (
          <PositionHistoryTable histories={positionHistory} />
        )}
        {activeTab === "OPEN_ORDERS" && <OpenOrdersTable orders={openOrders} />}
        {activeTab === "ORDER_HISTORY" && (
          <OrderHistoryTable orders={orderHistory} />
        )}
      </div>
    </section>
  );
}

function PositionsTable({ positions }: { positions: FuturesPosition[] }) {
  if (positions.length === 0) {
    return <EmptyPanelMessage message="현재 열린 포지션이 없습니다." />;
  }

  return (
    <div className="grid gap-main lg:grid-cols-2">
      {positions.map((position) => (
        <PositionCard
          key={`${position.symbol}-${position.positionSide}-${position.marginMode}`}
          position={position}
        />
      ))}
    </div>
  );
}

function PositionCard({ position }: { position: FuturesPosition }) {
  const closeableQuantity = getCloseableQuantity(position);
  const realizedPnl = position.realizedPnl ?? 0;
  const sideTone =
    position.positionSide === "LONG" ? "text-emerald-600" : "text-rose-600";
  const pnlTone =
    position.unrealizedPnl >= 0 ? "text-emerald-600" : "text-rose-600";
  const realizedTone = realizedPnl >= 0 ? "text-emerald-600" : "text-rose-600";

  return (
    <article className="rounded-main border border-main-light-gray bg-white px-main-2 py-5 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-main">
        <div className="min-w-0">
          <p className="text-xs-custom font-semibold uppercase text-main-dark-gray/50">
            Position
          </p>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <span className="text-lg-custom font-bold text-main-dark-gray">
              {position.symbol}
            </span>
            <span className={`text-sm-custom font-bold ${sideTone}`}>
              {position.positionSide}
            </span>
            <span className="text-sm-custom font-semibold text-main-dark-gray/55">
              {position.leverage}x
            </span>
          </div>
        </div>
        <ClosePositionButton
          disabled={closeableQuantity <= 0}
          marginMode={position.marginMode}
          positionSide={position.positionSide}
          quantity={closeableQuantity}
          suggestedLimitPrice={position.markPrice}
          symbol={position.symbol}
        />
      </div>

      <div className="mt-5 grid gap-x-main gap-y-4 border-y border-main-light-gray py-4 sm:grid-cols-2 xl:grid-cols-3">
        <PositionMetric label="Entry Price" value={formatUsd(position.entryPrice)} />
        <PositionMetric label="Margin" value={formatUsd(position.margin)} />
        <PositionMetric
          label="Unrealized PnL"
          toneClassName={pnlTone}
          value={formatSignedUsd(position.unrealizedPnl)}
        />
        <PositionMetric label="Mark Price" value={formatUsd(position.markPrice)} />
        <PositionMetric
          label="ROE"
          toneClassName={position.roi >= 0 ? "text-emerald-600" : "text-rose-600"}
          value={formatPercent(position.roi * 100)}
        />
        <PositionMetric
          label="Liq. Price"
          value={
            position.liquidationPrice ? formatUsd(position.liquidationPrice) : "-"
          }
        />
        <PositionMetric
          label="Realized PnL"
          toneClassName={realizedTone}
          value={formatSignedUsd(realizedPnl)}
        />
        <PositionMetric
          label="Close amount"
          value={`${formatPlainNumber(closeableQuantity)} / ${formatPlainNumber(
            position.quantity
          )} ${getBaseAsset(position.symbol)}`}
        />
        <PositionMetric
          label="Position TP/SL"
          value={`${formatOptionalUsd(position.takeProfitPrice)} / ${formatOptionalUsd(
            position.stopLossPrice
          )}`}
        />
      </div>

      <PositionTpslEditor position={position} />
    </article>
  );
}

function PositionMetric({
  label,
  value,
  toneClassName = "text-main-dark-gray",
}: {
  label: string;
  value: string;
  toneClassName?: string;
}) {
  return (
    <div className="min-w-0">
      <p className="text-xs-custom font-semibold text-main-dark-gray/50">
        {label}
      </p>
      <p className={`mt-1 truncate text-sm-custom font-bold ${toneClassName}`}>
        {value}
      </p>
    </div>
  );
}

function PositionTpslEditor({ position }: { position: FuturesPosition }) {
  const router = useRouter();
  const [takeProfitPrice, setTakeProfitPrice] = useState(() =>
    formatEditablePrice(position.takeProfitPrice)
  );
  const [stopLossPrice, setStopLossPrice] = useState(() =>
    formatEditablePrice(position.stopLossPrice)
  );
  const [isPending, setIsPending] = useState(false);

  useEffect(() => {
    setTakeProfitPrice(formatEditablePrice(position.takeProfitPrice));
    setStopLossPrice(formatEditablePrice(position.stopLossPrice));
  }, [
    position.marginMode,
    position.positionSide,
    position.stopLossPrice,
    position.symbol,
    position.takeProfitPrice,
  ]);

  async function submit(nextTakeProfitPrice: string, nextStopLossPrice: string) {
    const parsedTakeProfitPrice = parseOptionalPositiveNumber(nextTakeProfitPrice);
    const parsedStopLossPrice = parseOptionalPositiveNumber(nextStopLossPrice);

    if (parsedTakeProfitPrice === "invalid" || parsedStopLossPrice === "invalid") {
      toast.error("TP/SL 가격을 다시 확인해주세요.");
      return;
    }

    setIsPending(true);

    try {
      const response = await fetch("/proxy-futures/positions/tpsl", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          symbol: position.symbol,
          positionSide: position.positionSide,
          marginMode: position.marginMode,
          takeProfitPrice: parsedTakeProfitPrice,
          stopLossPrice: parsedStopLossPrice,
        }),
      });
      const payload =
        (await response.json()) as ClientApiResponse<FuturesPosition>;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message ?? "TP/SL 저장에 실패했습니다.");
      }

      toast.success("TP/SL이 저장되었습니다.");
      router.refresh();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "TP/SL 저장에 실패했습니다."
      );
    } finally {
      setIsPending(false);
    }
  }

  return (
    <div className="mt-5 grid gap-main sm:grid-cols-2">
      <TpslInput
        label="TP"
        onChange={setTakeProfitPrice}
        value={takeProfitPrice}
      />
      <TpslInput label="SL" onChange={setStopLossPrice} value={stopLossPrice} />
      <div className="grid grid-cols-2 gap-2 sm:col-span-2 sm:w-40">
        <button
          className="rounded-main bg-main-dark-gray px-3 py-2 text-xs-custom font-bold text-white disabled:cursor-not-allowed disabled:opacity-50"
          disabled={isPending}
          onClick={() => submit(takeProfitPrice, stopLossPrice)}
          type="button"
        >
          Save
        </button>
        <button
          className="rounded-main border border-main-light-gray px-3 py-2 text-xs-custom font-bold text-main-dark-gray/70 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={isPending}
          onClick={() => submit("", "")}
          type="button"
        >
          Clear
        </button>
      </div>
    </div>
  );
}

function TpslInput({
  label,
  onChange,
  value,
}: {
  label: string;
  onChange: (value: string) => void;
  value: string;
}) {
  return (
    <label className="block">
      <span className="text-xs-custom font-semibold text-main-dark-gray/60">
        {label}
      </span>
      <div className="mt-2 flex items-center gap-2 border-b border-main-light-gray pb-2">
        <input
          className="min-w-0 flex-1 bg-transparent text-sm-custom font-bold text-main-dark-gray outline-none"
          min="0"
          onChange={(event) => onChange(event.target.value)}
          placeholder="-"
          step="0.1"
          type="number"
          value={value}
        />
        <span className="text-xs-custom font-semibold text-main-dark-gray/45">
          USDT
        </span>
      </div>
    </label>
  );
}

function PositionHistoryTable({
  histories,
}: {
  histories: FuturesPositionHistory[];
}) {
  if (histories.length === 0) {
    return <EmptyPanelMessage message="아직 종료된 포지션 히스토리가 없습니다." />;
  }

  return (
    <ScrollableTableFrame>
      <table className="w-full min-w-[1280px] text-left text-sm-custom">
        <thead className="text-xs-custom text-main-dark-gray/50">
          <tr className="border-b border-main-light-gray">
            <th className="py-3 font-semibold">Open time</th>
            <th className="py-3 font-semibold">Symbol</th>
            <th className="py-3 font-semibold">Side</th>
            <th className="py-3 font-semibold">Entry</th>
            <th className="py-3 font-semibold">Exit</th>
            <th className="py-3 font-semibold">Size</th>
            <th className="py-3 font-semibold">PnL</th>
            <th className="py-3 font-semibold">ROI</th>
            <th className="py-3 font-semibold">Reason</th>
            <th className="py-3 text-right font-semibold">Closed</th>
          </tr>
        </thead>
        <tbody>
          {histories.map((history) => (
            <tr
              className="border-b border-main-light-gray/70 last:border-b-0"
              key={`${history.symbol}-${history.positionSide}-${history.openedAt}-${history.closedAt}`}
            >
              <td className="py-3 text-main-dark-gray/70">
                {formatDateTime(history.openedAt)}
              </td>
              <td className="py-3 font-semibold text-main-dark-gray">
                {history.symbol}
              </td>
              <td
                className={[
                  "py-3 font-semibold",
                  history.positionSide === "LONG"
                    ? "text-emerald-600"
                    : "text-rose-600",
                ].join(" ")}
              >
                {history.positionSide} · {history.marginMode} · {history.leverage}x
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {formatUsd(history.averageEntryPrice)}
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {formatUsd(history.averageExitPrice)}
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {history.positionSize.toFixed(3)}
              </td>
              <td
                className={[
                  "py-3 font-semibold",
                  history.realizedPnl >= 0 ? "text-emerald-600" : "text-rose-600",
                ].join(" ")}
              >
                {formatSignedUsd(history.realizedPnl)}
              </td>
              <td
                className={[
                  "py-3 font-semibold",
                  history.roi >= 0 ? "text-emerald-600" : "text-rose-600",
                ].join(" ")}
              >
                {formatPercent(history.roi * 100)}
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {formatCloseReason(history.closeReason)}
              </td>
              <td className="py-3 text-right text-main-dark-gray/70">
                {formatDateTime(history.closedAt)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </ScrollableTableFrame>
  );
}

function OrderHistoryTable({ orders }: { orders: FuturesOrderHistory[] }) {
  if (orders.length === 0) {
    return <EmptyPanelMessage message="아직 주문 히스토리가 없습니다." />;
  }

  return (
    <ScrollableTableFrame>
      <table className="w-full min-w-[1040px] text-left text-sm-custom">
        <thead className="text-xs-custom text-main-dark-gray/50">
          <tr className="border-b border-main-light-gray">
            <th className="py-3 font-semibold">Time</th>
            <th className="py-3 font-semibold">Direction</th>
            <th className="py-3 font-semibold">Coin</th>
            <th className="py-3 font-semibold">Order</th>
            <th className="py-3 font-semibold">Quantity</th>
            <th className="py-3 font-semibold">Price</th>
            <th className="py-3 font-semibold">Fee</th>
            <th className="py-3 text-right font-semibold">Status</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => {
            const orderTime = formatOrderHistoryTime(order.orderTime);

            return (
              <tr
                className="border-b border-main-light-gray/70 last:border-b-0"
                key={order.orderId}
              >
                <td className="py-3 text-main-dark-gray/70">
                  <span className="block font-semibold text-main-dark-gray">
                    {orderTime.date}
                  </span>
                  <span className="mt-1 block text-xs-custom">
                    {orderTime.time}
                  </span>
                </td>
                <td
                  className={[
                    "py-3 font-semibold",
                    order.positionSide === "LONG"
                      ? "text-emerald-600"
                      : "text-rose-600",
                  ].join(" ")}
                >
                  {order.positionSide}
                </td>
                <td className="py-3 font-semibold text-main-dark-gray">
                  {order.symbol}
                </td>
                <td className="py-3 text-main-dark-gray/70">
                  {formatOrderPurpose(order.orderPurpose)} · {order.orderType}
                </td>
                <td className="py-3 text-main-dark-gray/70">
                  {formatOrderQuantity(order)}
                </td>
                <td className="py-3 text-main-dark-gray/70">
                  {formatPlainNumber(order.executionPrice)}
                </td>
                <td className="py-3 text-main-dark-gray/70">
                  {formatPlainNumber(order.estimatedFee)} USDT
                </td>
                <td className="py-3 text-right font-semibold text-main-dark-gray">
                  {formatOrderStatus(order.status)}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </ScrollableTableFrame>
  );
}

function OpenOrdersTable({ orders }: { orders: FuturesOpenOrder[] }) {
  if (orders.length === 0) {
    return <EmptyPanelMessage message="현재 열린 지정가 주문이 없습니다." />;
  }

  return (
    <ScrollableTableFrame>
      <table className="w-full min-w-[1040px] text-left text-sm-custom">
        <thead className="text-xs-custom text-main-dark-gray/50">
          <tr className="border-b border-main-light-gray">
            <th className="py-3 font-semibold">Order time</th>
            <th className="py-3 font-semibold">Direction</th>
            <th className="py-3 font-semibold">Coin</th>
            <th className="py-3 font-semibold">Order</th>
            <th className="py-3 font-semibold">Quantity</th>
            <th className="py-3 font-semibold">Limit</th>
            <th className="py-3 font-semibold">Status</th>
            <th className="py-3 text-right font-semibold">Action</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => {
            const orderTime = formatOrderHistoryTime(order.orderTime);

            return (
              <tr
                className="border-b border-main-light-gray/70 last:border-b-0"
                key={order.orderId}
              >
                <td className="py-3 text-main-dark-gray/70">
                  <span className="block font-semibold text-main-dark-gray">
                    {orderTime.date}
                  </span>
                  <span className="mt-1 block text-xs-custom">
                    {orderTime.time}
                  </span>
                </td>
                <td
                  className={[
                    "py-3 font-semibold",
                    getOrderDirectionTone(order),
                  ].join(" ")}
                >
                  {formatOrderDirection(order)}
                </td>
                <td className="py-3 font-semibold text-main-dark-gray">
                  {order.symbol}
                </td>
                <td className="py-3 text-main-dark-gray/70">
                  <span className="block font-semibold text-main-dark-gray">
                    {order.orderType}
                  </span>
                  <span className="mt-1 block text-xs-custom">
                    #{order.orderId.slice(0, 8)}
                  </span>
                </td>
                <td className="py-3 text-main-dark-gray/70">
                  {formatOrderQuantity(order)}
                </td>
                <td className="py-3 text-main-dark-gray/70">
                  {order.limitPrice ? formatUsd(order.limitPrice) : "-"}
                </td>
                <td className="py-3 font-semibold text-main-dark-gray">
                  {formatOrderStatus(order.status)}
                </td>
                <td className="py-3 text-right">
                  <CancelOrderButton orderId={order.orderId} />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </ScrollableTableFrame>
  );
}

function ScrollableTableFrame({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <div className="futures-table-scroll">
      {children}
    </div>
  );
}

function Stat({
  label,
  value,
  subValue,
  tone = "neutral",
}: {
  label: string;
  value: string;
  subValue?: string;
  tone?: "positive" | "negative" | "neutral";
}) {
  const toneClassName =
    tone === "positive"
      ? "text-emerald-600"
      : tone === "negative"
        ? "text-rose-600"
        : "text-main-dark-gray";

  return (
    <div className="rounded-main border border-main-light-gray px-main py-3">
      <p className="text-xs-custom text-main-dark-gray/60">{label}</p>
      <p className={`mt-2 text-lg-custom font-semibold ${toneClassName}`}>
        {value}
      </p>
      {subValue ? (
        <p className="mt-1 text-xs-custom text-main-dark-gray/55">
          {subValue}
        </p>
      ) : null}
    </div>
  );
}

function EmptyPanelMessage({ message }: { message: string }) {
  return (
    <div
      className={[
        "rounded-main border border-dashed border-main-light-gray px-main py-8",
        "text-center text-sm-custom text-main-dark-gray/60",
      ].join(" ")}
    >
      {message}
    </div>
  );
}

function formatDateTime(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function formatOrderHistoryTime(value: string): { date: string; time: string } {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return {
      date: "-",
      time: "-",
    };
  }

  return {
    date: new Intl.DateTimeFormat("en-CA", {
      day: "2-digit",
      month: "2-digit",
      timeZone: "Asia/Seoul",
      year: "numeric",
    }).format(date),
    time: new Intl.DateTimeFormat("en-GB", {
      hour: "2-digit",
      hour12: false,
      minute: "2-digit",
      second: "2-digit",
      timeZone: "Asia/Seoul",
    }).format(date),
  };
}

function formatOrderPurpose(value: FuturesOpenOrder["orderPurpose"]): string {
  return value === "CLOSE_POSITION" ? "Close" : "Open";
}

function formatOrderQuantity(
  order: Pick<FuturesOrderHistory, "quantity" | "symbol">
): string {
  return `${formatPlainNumber(order.quantity)} ${getBaseAsset(order.symbol)}`;
}

function getCloseableQuantity(position: FuturesPosition): number {
  return position.closeableQuantity ?? position.quantity;
}

function getBaseAsset(symbol: string): string {
  return symbol.endsWith("USDT") ? symbol.slice(0, -4) : symbol;
}

function formatPlainNumber(value: number): string {
  if (!Number.isFinite(value)) {
    return "-";
  }

  return value.toFixed(8).replace(/\.?0+$/, "");
}

function formatOrderStatus(status: FuturesOrderHistory["status"]): string {
  if (status === "FILLED") {
    return "Executed";
  }

  if (status === "CANCELLED") {
    return "Cancelled";
  }

  if (status === "PENDING" || status === "OPEN") {
    return "Pending";
  }

  return "Rejected";
}

function formatOrderDirection(
  order: Pick<FuturesOpenOrder, "orderPurpose" | "positionSide">
): string {
  return `${formatOrderPurpose(order.orderPurpose)} ${
    order.positionSide === "LONG" ? "Long" : "Short"
  }`;
}

function getOrderDirectionTone(
  order: Pick<FuturesOpenOrder, "orderPurpose" | "positionSide">
): string {
  const green =
    (order.orderPurpose === "OPEN_POSITION" && order.positionSide === "LONG") ||
    (order.orderPurpose === "CLOSE_POSITION" && order.positionSide === "SHORT");
  return green ? "text-emerald-600" : "text-rose-600";
}

function formatCloseReason(reason: string): string {
  if (reason === "LIMIT_CLOSE") {
    return "Limit";
  }

  if (reason === "LIQUIDATION") {
    return "Liquidation";
  }

  if (reason === "TAKE_PROFIT") {
    return "TP";
  }

  if (reason === "STOP_LOSS") {
    return "SL";
  }

  return "Market";
}

function formatOptionalUsd(value: number | null | undefined): string {
  return typeof value === "number" ? formatUsd(value) : "-";
}

function formatEditablePrice(value: number | null | undefined): string {
  return typeof value === "number" ? formatPlainNumber(value) : "";
}

function parseOptionalPositiveNumber(value: string): number | null | "invalid" {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return null;
  }

  const parsed = Number(trimmed);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return "invalid";
  }
  return parsed;
}
