"use client";

import CancelOrderButton from "@/components/futures/CancelOrderButton";
import ClosePositionButton from "@/components/futures/ClosePositionButton";
import FuturesPriceChart from "@/components/futures/FuturesPriceChart";
import OrderEntryPanel from "@/components/futures/OrderEntryPanel";
import type {
  FuturesOpenOrder,
  FuturesOrderHistory,
  FuturesPosition,
  FuturesTradingExecutionEvent,
  MarketApiResponse,
} from "@/lib/futures-api";
import {
  formatCompactUsd,
  formatPercent,
  formatSignedUsd,
  formatUsd,
  type MarketSnapshot,
} from "@/lib/markets";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

type Props = {
  initialMarket: MarketSnapshot;
  isAuthenticated: boolean;
  currentOpenOrders: FuturesOpenOrder[];
  currentPositions: FuturesPosition[];
  orderHistory: FuturesOrderHistory[];
};

type TradingTab = "POSITIONS" | "HISTORY" | "OPEN_ORDERS";

export default function MarketDetailRealtimeView({
  initialMarket,
  isAuthenticated,
  currentOpenOrders,
  currentPositions,
  orderHistory,
}: Props) {
  const router = useRouter();
  const [market, setMarket] = useState(initialMarket);
  const [marketUpdatedAt, setMarketUpdatedAt] = useState(() => Date.now());
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

        setMarket((current) => ({
          ...current,
          displayName: data.displayName,
          lastPrice: data.lastPrice,
          markPrice: data.markPrice,
          indexPrice: data.indexPrice,
          fundingRate: data.fundingRate,
          change24h: data.change24h,
        }));
        setMarketUpdatedAt(Date.now());
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
        setActiveTab(data.type === "ORDER_FILLED" ? "HISTORY" : "POSITIONS");
        router.refresh();
      } catch {
        // Keep the stream alive when a malformed event slips through.
      }
    };

    return () => {
      stream.close();
    };
  }, [initialMarket.symbol, isAuthenticated, router]);

  const unrealizedPnl = useMemo(
    () =>
      currentPositions.reduce((sum, position) => sum + position.unrealizedPnl, 0),
    [currentPositions]
  );

  return (
    <div className="flex flex-col gap-main-2 px-main-2 pb-24">
      <section className="grid grid-cols-[minmax(0,1fr)_360px] gap-main-2 pt-4">
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
              />
              <Stat label="24h 거래량" value={formatCompactUsd(market.volume24h)} />
            </div>
          </div>

          <FuturesPriceChart
            change24h={market.change24h}
            currentPrice={market.lastPrice}
            currentPriceUpdatedAt={marketUpdatedAt}
            openOrders={currentOpenOrders}
            positions={currentPositions}
            symbol={market.symbol}
          />

          <ExecutionEventPanel events={executionEvents} />

          <TradingBlotter
            activeTab={activeTab}
            onTabChange={setActiveTab}
            openOrders={currentOpenOrders}
            orderHistory={orderHistory}
            positions={currentPositions}
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
  unrealizedPnl,
}: {
  activeTab: TradingTab;
  onTabChange: (tab: TradingTab) => void;
  openOrders: FuturesOpenOrder[];
  orderHistory: FuturesOrderHistory[];
  positions: FuturesPosition[];
  unrealizedPnl: number;
}) {
  const tabs: Array<{ label: string; value: TradingTab; count: number }> = [
    { label: "포지션", value: "POSITIONS", count: positions.length },
    { label: "히스토리", value: "HISTORY", count: orderHistory.length },
    { label: "Open orders", value: "OPEN_ORDERS", count: openOrders.length },
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

      <div className="min-h-[220px] px-main-2 py-4">
        {activeTab === "POSITIONS" && <PositionsTable positions={positions} />}
        {activeTab === "HISTORY" && <HistoryTable orders={orderHistory} />}
        {activeTab === "OPEN_ORDERS" && <OpenOrdersTable orders={openOrders} />}
      </div>
    </section>
  );
}

function PositionsTable({ positions }: { positions: FuturesPosition[] }) {
  if (positions.length === 0) {
    return <EmptyPanelMessage message="현재 이 심볼에 열린 포지션이 없습니다." />;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[880px] text-left text-sm-custom">
        <thead className="text-xs-custom text-main-dark-gray/50">
          <tr className="border-b border-main-light-gray">
            <th className="py-3 font-semibold">Symbol</th>
            <th className="py-3 font-semibold">Side</th>
            <th className="py-3 font-semibold">Size</th>
            <th className="py-3 font-semibold">Entry</th>
            <th className="py-3 font-semibold">Mark</th>
            <th className="py-3 font-semibold">PnL</th>
            <th className="py-3 text-right font-semibold">Action</th>
          </tr>
        </thead>
        <tbody>
          {positions.map((position) => (
            <tr
              className="border-b border-main-light-gray/70 last:border-b-0"
              key={`${position.symbol}-${position.positionSide}-${position.marginMode}`}
            >
              <td className="py-3 font-semibold text-main-dark-gray">
                {position.symbol}
              </td>
              <td
                className={[
                  "py-3 font-semibold",
                  position.positionSide === "LONG"
                    ? "text-emerald-600"
                    : "text-rose-600",
                ].join(" ")}
              >
                {position.positionSide} · {position.marginMode} · {position.leverage}x
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {position.quantity.toFixed(3)}
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {formatUsd(position.entryPrice)}
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {formatUsd(position.markPrice)}
              </td>
              <td
                className={[
                  "py-3 font-semibold",
                  position.unrealizedPnl >= 0
                    ? "text-emerald-600"
                    : "text-rose-600",
                ].join(" ")}
              >
                {formatSignedUsd(position.unrealizedPnl)}
              </td>
              <td className="py-3 text-right">
                <ClosePositionButton
                  marginMode={position.marginMode}
                  positionSide={position.positionSide}
                  quantity={position.quantity}
                  symbol={position.symbol}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function HistoryTable({ orders }: { orders: FuturesOrderHistory[] }) {
  if (orders.length === 0) {
    return <EmptyPanelMessage message="아직 이 심볼의 주문 히스토리가 없습니다." />;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[820px] text-left text-sm-custom">
        <thead className="text-xs-custom text-main-dark-gray/50">
          <tr className="border-b border-main-light-gray">
            <th className="py-3 font-semibold">Order</th>
            <th className="py-3 font-semibold">Side</th>
            <th className="py-3 font-semibold">Type</th>
            <th className="py-3 font-semibold">Qty</th>
            <th className="py-3 font-semibold">Exec price</th>
            <th className="py-3 font-semibold">Fee</th>
            <th className="py-3 text-right font-semibold">Status</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr
              className="border-b border-main-light-gray/70 last:border-b-0"
              key={order.orderId}
            >
              <td className="py-3 font-semibold text-main-dark-gray">
                {order.orderId}
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
              <td className="py-3 text-main-dark-gray/70">
                {order.orderType} · {order.marginMode} · {order.leverage}x
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {order.quantity.toFixed(3)}
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {formatUsd(order.executionPrice)}
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {order.feeType} · {formatUsd(order.estimatedFee)}
              </td>
              <td className="py-3 text-right font-semibold text-main-dark-gray">
                {order.status}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function OpenOrdersTable({ orders }: { orders: FuturesOpenOrder[] }) {
  if (orders.length === 0) {
    return <EmptyPanelMessage message="현재 이 심볼에 열린 지정가 주문이 없습니다." />;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[820px] text-left text-sm-custom">
        <thead className="text-xs-custom text-main-dark-gray/50">
          <tr className="border-b border-main-light-gray">
            <th className="py-3 font-semibold">Order</th>
            <th className="py-3 font-semibold">Side</th>
            <th className="py-3 font-semibold">Limit</th>
            <th className="py-3 font-semibold">Qty</th>
            <th className="py-3 font-semibold">Fee</th>
            <th className="py-3 text-right font-semibold">Action</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr
              className="border-b border-main-light-gray/70 last:border-b-0"
              key={order.orderId}
            >
              <td className="py-3 font-semibold text-main-dark-gray">
                {order.orderId}
              </td>
              <td
                className={[
                  "py-3 font-semibold",
                  order.positionSide === "LONG"
                    ? "text-emerald-600"
                    : "text-rose-600",
                ].join(" ")}
              >
                {order.positionSide} · {order.marginMode} · {order.leverage}x
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {order.limitPrice ? formatUsd(order.limitPrice) : "-"}
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {order.quantity.toFixed(3)}
              </td>
              <td className="py-3 text-main-dark-gray/70">
                {order.feeType} · {formatUsd(order.estimatedFee)}
              </td>
              <td className="py-3 text-right">
                <CancelOrderButton orderId={order.orderId} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Stat({
  label,
  value,
  tone = "neutral",
}: {
  label: string;
  value: string;
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
