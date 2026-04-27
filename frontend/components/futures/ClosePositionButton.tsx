"use client";

import Button from "@/components/ui/shared/Button";
import Modal from "@/components/ui/Modal";
import { formatUsd } from "@/lib/markets";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { toast } from "react-toastify";

type Props = {
  symbol: string;
  positionSide: "LONG" | "SHORT";
  marginMode: "ISOLATED" | "CROSS";
  quantity: number;
  markPrice: number;
  disabled?: boolean;
};

type CloseOrderType = "MARKET" | "LIMIT";

type ClosePositionResponse = {
  symbol: string;
  closedQuantity: number;
  realizedPnl: number;
  grantedPoint: number;
};

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export default function ClosePositionButton({
  symbol,
  positionSide,
  marginMode,
  quantity,
  markPrice,
  disabled = false,
}: Props) {
  const router = useRouter();
  const [isPending, setIsPending] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [orderType, setOrderType] = useState<CloseOrderType>("MARKET");
  const [closeQuantity, setCloseQuantity] = useState(quantity.toString());
  const [limitPrice, setLimitPrice] = useState(markPrice.toFixed(1));
  const [snapshotMarkPrice, setSnapshotMarkPrice] = useState(markPrice);
  const wasOpenRef = useRef(false);

  useEffect(() => {
    if (isOpen && !wasOpenRef.current) {
      setSnapshotMarkPrice(markPrice);
      setCloseQuantity(quantity.toString());
      setLimitPrice(markPrice.toFixed(1));
    }
    wasOpenRef.current = isOpen;
  }, [isOpen, quantity, markPrice]);

  async function handleClose() {
    const parsedQuantity = Number.parseFloat(closeQuantity);
    const parsedLimitPrice = Number.parseFloat(limitPrice);

    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
      toast.error("종료 수량을 다시 확인해주세요.");
      return;
    }

    if (parsedQuantity > quantity) {
      toast.error("보유 수량보다 많이 종료할 수 없습니다.");
      return;
    }

    if (
      orderType === "LIMIT" &&
      (!Number.isFinite(parsedLimitPrice) || parsedLimitPrice <= 0)
    ) {
      toast.error("지정가를 다시 확인해주세요.");
      return;
    }

    setIsPending(true);

    try {
      const response = await fetch("/proxy-futures/positions/close", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          symbol,
          positionSide,
          marginMode,
          quantity: parsedQuantity,
          orderType,
          limitPrice: orderType === "LIMIT" ? parsedLimitPrice : null,
        }),
      });

      const payload =
        (await response.json()) as ClientApiResponse<ClosePositionResponse>;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message ?? "포지션 종료에 실패했습니다.");
      }

      toast.success(
        orderType === "LIMIT"
          ? `${symbol} 종료 지정가 주문이 등록되었습니다.`
          : `${payload.data.symbol} 포지션 종료 완료 · 손익 ${payload.data.realizedPnl.toFixed(2)} USDT`
      );
      setIsOpen(false);
      router.refresh();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "포지션 종료에 실패했습니다."
      );
    } finally {
      setIsPending(false);
    }
  }

  return (
    <>
      <Button
        disabled={disabled || isPending}
        onClick={() => setIsOpen(true)}
        variant="danger"
      >
        {isPending ? "종료 중..." : disabled ? "종료 불가" : "포지션 종료"}
      </Button>

      <Modal
        hasBackdropBlur={false}
        isEscapeClose
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
      >
        <div className="w-[min(440px,calc(100vw-48px))] pr-6">
          <p className="text-lg-custom font-bold text-main-dark-gray">
            포지션 종료
          </p>
          <p className="mt-2 text-sm-custom text-main-dark-gray/60">
            {symbol} · {positionSide} · {marginMode} · 보유 {quantity.toFixed(3)}
          </p>

          <div className="mt-5 grid grid-cols-2 rounded-main bg-main-light-gray/45 p-1">
            {(["MARKET", "LIMIT"] as CloseOrderType[]).map((type) => {
              const active = orderType === type;
              return (
                <button
                  className={[
                    "rounded-main px-3 py-2 text-sm-custom font-semibold transition-colors",
                    active
                      ? "bg-white text-main-dark-gray shadow-sm"
                      : "text-main-dark-gray/45 hover:text-main-dark-gray",
                  ].join(" ")}
                  key={type}
                  onClick={() => setOrderType(type)}
                  type="button"
                >
                  {type === "MARKET" ? "Market" : "Limit"}
                </button>
              );
            })}
          </div>

          {orderType === "LIMIT" && (
            <label className="mt-5 block">
              <span className="text-xs-custom font-semibold text-main-dark-gray/60">
                Limit price
              </span>
              <div className="mt-2 flex items-center gap-2 rounded-main border border-main-light-gray px-main py-3">
                <input
                  className="min-w-0 flex-1 bg-transparent text-sm-custom font-bold text-main-dark-gray outline-none"
                  min="0"
                  onChange={(event) => setLimitPrice(event.target.value)}
                  step="0.1"
                  type="number"
                  value={limitPrice}
                />
                <span className="text-xs-custom font-semibold text-main-dark-gray/50">
                  USDT
                </span>
              </div>
            </label>
          )}

          <label className="mt-5 block">
            <span className="text-xs-custom font-semibold text-main-dark-gray/60">
              Quantity
            </span>
            <div className="mt-2 flex items-center gap-2 rounded-main border border-main-light-gray px-main py-3">
              <input
                className="min-w-0 flex-1 bg-transparent text-sm-custom font-bold text-main-dark-gray outline-none"
                max={quantity}
                min="0.001"
                onChange={(event) => setCloseQuantity(event.target.value)}
                step="0.001"
                type="number"
                value={closeQuantity}
              />
              <span className="text-xs-custom font-semibold text-main-dark-gray/50">
                {symbol.replace("USDT", "")}
              </span>
            </div>
          </label>

          <div className="mt-4 grid grid-cols-4 gap-2">
            {[25, 50, 75, 100].map((percent) => (
              <button
                className="rounded-main border border-main-light-gray px-3 py-2 text-xs-custom font-semibold text-main-dark-gray/70"
                key={percent}
                onClick={() =>
                  setCloseQuantity(((quantity * percent) / 100).toFixed(3))
                }
                type="button"
              >
                {percent}%
              </button>
            ))}
          </div>

          <div className="mt-5 rounded-main bg-main-light-gray/30 px-main py-3 text-xs-custom text-main-dark-gray/65">
            {orderType === "MARKET"
              ? `시장가로 즉시 종료합니다. 기준가 ${formatUsd(snapshotMarkPrice)}`
              : "지정가 종료 주문은 체결 전까지 Open orders에서 취소할 수 있습니다."}
          </div>

          <button
            className={[
              "mt-5 w-full rounded-main bg-main-dark-gray px-main py-3",
              "text-sm-custom font-bold text-white disabled:cursor-not-allowed",
              "disabled:opacity-55",
            ].join(" ")}
            disabled={isPending}
            onClick={handleClose}
            type="button"
          >
            {isPending ? "처리 중..." : "확인"}
          </button>
        </div>
      </Modal>
    </>
  );
}
