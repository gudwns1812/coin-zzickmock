"use client";

import CancelOrderButton from "@/components/futures/CancelOrderButton";
import ClosePositionButton from "@/components/futures/ClosePositionButton";
import FuturesPriceChart from "@/components/futures/FuturesPriceChart";
import OrderEntryPanel from "@/components/futures/OrderEntryPanel";
import type {
  FuturesOpenOrder,
  FuturesPosition,
  MarketApiResponse,
} from "@/lib/futures-api";
import {
  formatCompactUsd,
  formatPercent,
  formatSignedUsd,
  formatUsd,
  type MarketSnapshot,
} from "@/lib/markets";
import { useEffect, useMemo, useState } from "react";

type Props = {
  initialMarket: MarketSnapshot;
  currentOpenOrders: FuturesOpenOrder[];
  currentPositions: FuturesPosition[];
};

export default function MarketDetailRealtimeView({
  initialMarket,
  currentOpenOrders,
  currentPositions,
}: Props) {
  const [market, setMarket] = useState(initialMarket);

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
      } catch {
        // Keep the last known snapshot when the stream sends malformed data.
      }
    };

    return () => {
      stream.close();
    };
  }, [initialMarket.symbol]);

  const unrealizedPnl = useMemo(
    () =>
      currentPositions.reduce((sum, position) => sum + position.unrealizedPnl, 0),
    [currentPositions]
  );

  return (
    <div className="flex flex-col gap-8 px-main-2 pb-24">
      <section className="grid grid-cols-[1.55fr_0.95fr] gap-main-2 pt-4">
        <div className="flex flex-col gap-main-2">
          <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
            <div className="flex items-start justify-between gap-main">
              <div>
                <p className="text-xs-custom uppercase tracking-[0.2em] text-main-dark-gray/50">
                  {market.symbol}
                </p>
                <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
                  {market.displayName}
                </h1>
                <p className="mt-3 max-w-3xl text-sm-custom text-main-dark-gray/70 break-keep">
                  차트, 포지션, 대기 주문, 직접 LONG/SHORT 진입을 한 화면에 모은
                  메인 트레이딩 화면입니다. 상승은 녹색, 하락은 빨강 의미 체계로
                  정리했습니다.
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
                <p className="text-xs-custom font-semibold uppercase tracking-[0.16em]">
                  24H
                </p>
                <p className="mt-2 text-lg-custom font-bold">
                  {formatPercent(market.change24h)}
                </p>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-5 gap-main">
              <Stat label="최신 체결가" value={formatUsd(market.lastPrice)} tone={market.change24h >= 0 ? "positive" : "negative"} />
              <Stat label="Mark Price" value={formatUsd(market.markPrice)} />
              <Stat label="Index Price" value={formatUsd(market.indexPrice)} />
              <Stat label="Funding" value={formatPercent(market.fundingRate * 100)} tone={market.fundingRate >= 0 ? "positive" : "negative"} />
              <Stat label="24h 거래량" value={formatCompactUsd(market.volume24h)} />
            </div>
          </div>

          <FuturesPriceChart
            change24h={market.change24h}
            currentPrice={market.lastPrice}
            openOrders={currentOpenOrders}
            positions={currentPositions}
            symbol={market.symbol}
          />
        </div>

        <aside className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
          <div>
            <p className="text-sm-custom text-main-dark-gray/60">주문 패널</p>
            <h2 className="mt-2 text-2xl-custom font-bold text-main-dark-gray">
              Direct LONG / SHORT
            </h2>
            <p className="mt-2 text-sm-custom text-main-dark-gray/60 break-keep">
              미리보기 CTA를 제거하고, 입력 변경에 맞춰 비용과 위험을 즉시
              갱신합니다.
            </p>
          </div>

          <div className="mt-5">
            <OrderEntryPanel symbol={market.symbol} currentPrice={market.lastPrice} />
          </div>
        </aside>
      </section>

      <section className="grid grid-cols-[1fr_1fr_0.8fr] gap-main-2">
        <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
          <div className="flex items-center justify-between gap-main">
            <div>
              <p className="text-lg-custom font-semibold text-main-dark-gray">
                열린 포지션
              </p>
              <p className="mt-1 text-sm-custom text-main-dark-gray/60">
                총 미실현 손익 {formatSignedUsd(unrealizedPnl)}
              </p>
            </div>
          </div>

          <div className="mt-4 flex flex-col gap-3">
            {currentPositions.length === 0 ? (
              <EmptyPanelMessage message="현재 이 심볼에 열린 포지션이 없습니다." />
            ) : (
              currentPositions.map((position) => (
                <div
                  className="rounded-main border border-main-light-gray px-main py-4"
                  key={`${position.symbol}-${position.positionSide}-${position.marginMode}`}
                >
                  <div className="flex items-start justify-between gap-main">
                    <div>
                      <p
                        className={[
                          "text-sm-custom font-semibold",
                          position.positionSide === "LONG"
                            ? "text-emerald-600"
                            : "text-rose-600",
                        ].join(" ")}
                      >
                        {position.positionSide} · {position.marginMode}
                      </p>
                      <p className="mt-2 text-sm-custom text-main-dark-gray/60">
                        수량 {position.quantity.toFixed(3)} · 레버리지 {position.leverage}x
                      </p>
                      <p className="mt-1 text-sm-custom text-main-dark-gray/60">
                        진입가 {formatUsd(position.entryPrice)}
                      </p>
                      <p className="mt-1 text-sm-custom text-main-dark-gray/60">
                        현재 손익 {formatSignedUsd(position.unrealizedPnl)}
                      </p>
                    </div>
                    <ClosePositionButton
                      marginMode={position.marginMode}
                      positionSide={position.positionSide}
                      quantity={position.quantity}
                      symbol={position.symbol}
                    />
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
          <p className="text-lg-custom font-semibold text-main-dark-gray">
            열린 주문
          </p>
          <p className="mt-1 text-sm-custom text-main-dark-gray/60">
            대기 중인 LIMIT 주문은 차트에 가로선으로 함께 표시됩니다.
          </p>

          <div className="mt-4 flex flex-col gap-3">
            {currentOpenOrders.length === 0 ? (
              <EmptyPanelMessage message="현재 이 심볼에 열린 지정가 주문이 없습니다." />
            ) : (
              currentOpenOrders.map((order) => (
                <div
                  className="rounded-main border border-main-light-gray px-main py-4"
                  key={order.orderId}
                >
                  <div className="flex items-start justify-between gap-main">
                    <div>
                      <p
                        className={[
                          "text-sm-custom font-semibold",
                          order.positionSide === "LONG"
                            ? "text-emerald-600"
                            : "text-rose-600",
                        ].join(" ")}
                      >
                        {order.positionSide} LIMIT
                      </p>
                      <p className="mt-2 text-sm-custom text-main-dark-gray/60">
                        가격 {order.limitPrice ? formatUsd(order.limitPrice) : "-"}
                      </p>
                      <p className="mt-1 text-sm-custom text-main-dark-gray/60">
                        수량 {order.quantity.toFixed(3)} · {order.marginMode} · {order.leverage}x
                      </p>
                    </div>
                    <CancelOrderButton orderId={order.orderId} />
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
          <p className="text-lg-custom font-semibold text-main-dark-gray">
            거래 규칙
          </p>
          <div className="mt-4 flex flex-col gap-3">
            <RuleCard
              title="체결 규칙"
              description="LONG 지정가는 최신 체결가가 지정가 이하로 내려오면 체결되고, SHORT 지정가는 최신 체결가가 지정가 이상으로 올라오면 체결됩니다."
            />
            <RuleCard
              title="수수료"
              description="Maker 0.015%, Taker 0.05% 고정 규칙입니다. 실시간 risk summary가 preview API 기준으로 갱신됩니다."
            />
            <RuleCard
              title="차트 상태"
              description="캔들 히스토리가 있으면 timeframe별 캔들 차트를, 응답이 없으면 세션 실시간 가격선 셸을 보여줍니다."
            />
          </div>
        </div>
      </section>
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
    <div className="rounded-main border border-dashed border-main-light-gray px-main py-5 text-sm-custom text-main-dark-gray/60">
      {message}
    </div>
  );
}

function RuleCard({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="rounded-main border border-main-light-gray px-main py-4">
      <p className="text-sm-custom font-semibold text-main-dark-gray">{title}</p>
      <p className="mt-2 text-sm-custom text-main-dark-gray/60 break-keep">
        {description}
      </p>
    </div>
  );
}
