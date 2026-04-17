"use client";

import OrderEntryPanel from "@/components/futures/OrderEntryPanel";
import type {FuturesPosition, MarketApiResponse} from "@/lib/futures-api";
import {formatPercent, formatUsd, type MarketSnapshot} from "@/lib/markets";
import {useEffect, useState} from "react";

type Props = {
    initialMarket: MarketSnapshot;
    currentPositions: FuturesPosition[];
};

export default function MarketDetailRealtimeView({
                                                     initialMarket,
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
                // Ignore malformed events and keep the last known snapshot.
            }
        };

        return () => {
            stream.close();
        };
    }, [initialMarket.symbol]);

    return (
        <div className="px-main-2 pb-24 flex flex-col gap-8">
            <section className="grid grid-cols-[1.5fr_0.9fr] gap-main-2 pt-4">
                <div className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray">
                    <p className="text-xs-custom uppercase tracking-[0.2em] text-main-dark-gray/50">
                        {market.symbol}
                    </p>
                    <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
                        {market.displayName}
                    </h1>
                    <p className="mt-3 text-sm-custom text-main-dark-gray/70 break-keep">
                        Bitget 기반 시세와 주문 미리보기 엔진을 연결한 마켓 상세입니다.
                        시장가와 지정가 주문, 마진 모드, 예상 청산가를 한 화면에서 확인할 수
                        있습니다.
                    </p>

                    <div className="mt-6 grid grid-cols-4 gap-main">
                        <Stat label="최신 체결가" value={formatUsd(market.lastPrice)}/>
                        <Stat label="Mark Price" value={formatUsd(market.markPrice)}/>
                        <Stat label="Index Price" value={formatUsd(market.indexPrice)}/>
                        <Stat label="Funding" value={formatPercent(market.fundingRate * 100)}/>
                    </div>

                    <div
                        className="mt-6 rounded-main bg-main-light-gray/40 p-main-2 min-h-[360px] flex flex-col justify-between">
                        <div>
                            <p className="text-sm-custom font-semibold text-main-dark-gray">
                                차트 영역
                            </p>
                            <p className="mt-2 text-sm-custom text-main-dark-gray/60 break-keep">
                                다음 단계에서 Bitget 캔들 데이터를 붙이고 `1m`, `5m`, `1h`,
                                `4h`, `1d` 전환을 제공합니다. 현재는 주문 흐름과 포지션 상태를
                                먼저 검증하는 단계입니다.
                            </p>
                        </div>
                        <div className="grid grid-cols-3 gap-main">
                            <PreviewBadge label="24h 변화율" value={formatPercent(market.change24h)}/>
                            <PreviewBadge label="24h 거래량" value={formatUsd(market.volume24h)}/>
                            <PreviewBadge label="시장 메모" value={market.openInterestLabel}/>
                        </div>
                    </div>
                </div>

                <aside
                    className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray flex flex-col gap-5">
                    <div>
                        <p className="text-sm-custom text-main-dark-gray/60">주문 패널</p>
                        <h2 className="mt-2 text-2xl-custom font-bold text-main-dark-gray">
                            LONG / SHORT 주문 입력
                        </h2>
                        <p className="mt-2 text-sm-custom text-main-dark-gray/60 break-keep">
                            지정가가 현재 체결 범위를 넘으면 taker, 대기 상태면 maker로
                            계산됩니다.
                        </p>
                    </div>

                    <OrderEntryPanel symbol={market.symbol} currentPrice={market.lastPrice}/>
                </aside>
            </section>

            <section className="grid grid-cols-3 gap-main-2">
                <Panel
                    title="열린 포지션"
                    description={
                        currentPositions.length > 0
                            ? currentPositions
                                .map(
                                    (position) =>
                                        `${position.positionSide} ${position.quantity} @ ${formatUsd(position.entryPrice)} · 미실현 ${formatUsd(position.unrealizedPnl)}`
                                )
                                .join(" / ")
                            : "현재 이 심볼에 열린 포지션이 없습니다."
                    }
                />
                <Panel
                    title="체결 규칙"
                    description="LONG 지정가는 최신 체결가가 지정가 이하로 내려오면 체결되고, SHORT 지정가는 최신 체결가가 지정가 이상으로 올라오면 체결됩니다."
                />
                <Panel
                    title="수수료 정책"
                    description="Maker 0.015%, Taker 0.05% 고정 규칙을 사용합니다. 시장가 주문은 항상 taker로 계산됩니다."
                />
            </section>
        </div>
    );
}

function Stat({label, value}: { label: string; value: string }) {
    return (
        <div className="rounded-main border border-main-light-gray px-main py-3">
            <p className="text-xs-custom text-main-dark-gray/60">{label}</p>
            <p className="mt-2 text-lg-custom font-semibold text-main-dark-gray">{value}</p>
        </div>
    );
}

function PreviewBadge({label, value}: { label: string; value: string }) {
    return (
        <div className="rounded-main bg-white px-main py-3 shadow-sm">
            <p className="text-xs-custom text-main-dark-gray/60">{label}</p>
            <p className="mt-2 text-sm-custom font-semibold text-main-dark-gray">{value}</p>
        </div>
    );
}

function Panel({
                   title,
                   description,
               }: {
    title: string;
    description: string;
}) {
    return (
        <div className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray min-h-[180px]">
            <p className="text-lg-custom font-semibold text-main-dark-gray">{title}</p>
            <p className="mt-3 text-sm-custom text-main-dark-gray/60 break-keep">
                {description}
            </p>
        </div>
    );
}
