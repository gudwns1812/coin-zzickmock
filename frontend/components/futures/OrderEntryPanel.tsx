"use client";

import Button from "@/components/ui/shared/Button";
import type {
  OrderExecutionResponse,
  OrderPreviewRequest,
  OrderPreviewResponse,
} from "@/lib/futures-api";
import { formatUsd, type MarketSymbol } from "@/lib/markets";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";

type Props = {
  symbol: MarketSymbol;
  currentPrice: number;
};

type Side = "LONG" | "SHORT";
type OrderType = "MARKET" | "LIMIT";
type MarginMode = "ISOLATED" | "CROSS";

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export default function OrderEntryPanel({ symbol, currentPrice }: Props) {
  const router = useRouter();
  const [positionSide, setPositionSide] = useState<Side>("LONG");
  const [orderType, setOrderType] = useState<OrderType>("MARKET");
  const [marginMode, setMarginMode] = useState<MarginMode>("ISOLATED");
  const [leverage, setLeverage] = useState(10);
  const [quantity, setQuantity] = useState("0.01");
  const [limitPrice, setLimitPrice] = useState(currentPrice.toString());
  const [isLimitPriceDirty, setIsLimitPriceDirty] = useState(false);
  const [preview, setPreview] = useState<OrderPreviewResponse | null>(null);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [isPreviewBlocked, setIsPreviewBlocked] = useState(false);
  const [isPreviewPending, setIsPreviewPending] = useState(false);
  const [isSubmitPending, setIsSubmitPending] = useState(false);

  useEffect(() => {
    if (orderType === "MARKET") {
      setLimitPrice(currentPrice.toString());
      setIsLimitPriceDirty(false);
      return;
    }

    if (!isLimitPriceDirty) {
      setLimitPrice(currentPrice.toString());
    }
  }, [currentPrice, isLimitPriceDirty, orderType]);

  const previewPayload = useMemo(
    () =>
      buildOrderPayload({
        symbol,
        positionSide,
        orderType,
        marginMode,
        leverage,
        quantity,
        limitPrice,
      }),
    [leverage, limitPrice, marginMode, orderType, positionSide, quantity, symbol]
  );

  const hasValidOrder = previewPayload !== null;

  async function requestPreview(
    payload: OrderPreviewRequest,
    side: Side,
    options?: { allowPreviewBlock?: boolean }
  ) {
    const response = await fetch("/proxy-futures/orders/preview", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    const parsedPayload =
      (await response.json()) as ClientApiResponse<OrderPreviewResponse>;

    if (!response.ok || !parsedPayload.success || !parsedPayload.data) {
      if (
        options?.allowPreviewBlock !== false &&
        (response.status === 401 || response.status === 403)
      ) {
        setIsPreviewBlocked(true);
      }
      throw new Error(parsedPayload.message ?? "주문 계산에 실패했습니다.");
    }

    setPositionSide(side);
    setPreview(parsedPayload.data);
    setPreviewError(null);
    return parsedPayload.data;
  }

  useEffect(() => {
    if (!previewPayload) {
      setPreview(null);
      setPreviewError(null);
      return;
    }

    if (isPreviewBlocked) {
      return;
    }

    let cancelled = false;
    const timeout = window.setTimeout(async () => {
      setIsPreviewPending(true);
      setPreviewError(null);

      try {
        if (!cancelled) {
          await requestPreview(previewPayload, positionSide);
        }
      } catch (error) {
        if (!cancelled) {
          setPreview(null);
          setPreviewError(
            error instanceof Error ? error.message : "주문 계산에 실패했습니다."
          );
        }
      } finally {
        if (!cancelled) {
          setIsPreviewPending(false);
        }
      }
    }, 250);

    return () => {
      cancelled = true;
      window.clearTimeout(timeout);
    };
  }, [isPreviewBlocked, previewPayload]);

  async function handleSubmit(side: Side) {
    const orderPayload = buildOrderPayload({
      symbol,
      positionSide: side,
      orderType,
      marginMode,
      leverage,
      quantity,
      limitPrice,
    });

    if (!orderPayload) {
      toast.error("수량과 가격을 다시 확인해주세요.");
      return;
    }

    setPositionSide(side);
    setIsSubmitPending(true);

    try {
      await requestPreview(orderPayload, side, { allowPreviewBlock: false });

      const response = await fetch("/proxy-futures/orders", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(orderPayload),
      });

      const payload =
        (await response.json()) as ClientApiResponse<OrderExecutionResponse>;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message ?? "주문 생성에 실패했습니다.");
      }

      toast.success(
        payload.data.status === "FILLED"
          ? `${symbol} ${side} 주문이 즉시 체결되었습니다.`
          : `${symbol} ${side} 지정가 주문이 대기열에 등록되었습니다.`
      );
      router.refresh();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "주문 생성에 실패했습니다."
      );
    } finally {
      setIsSubmitPending(false);
    }
  }

  return (
    <div className="flex flex-col gap-5">
      <div className="grid grid-cols-2 gap-main">
        <ToggleButton
          active={positionSide === "LONG"}
          tone="positive"
          onClick={() => setPositionSide("LONG")}
        >
          LONG
        </ToggleButton>
        <ToggleButton
          active={positionSide === "SHORT"}
          tone="negative"
          onClick={() => setPositionSide("SHORT")}
        >
          SHORT
        </ToggleButton>
      </div>

      <div className="grid grid-cols-2 gap-main">
        <ToggleButton
          active={orderType === "MARKET"}
          onClick={() => setOrderType("MARKET")}
        >
          MARKET
        </ToggleButton>
        <ToggleButton
          active={orderType === "LIMIT"}
          onClick={() => setOrderType("LIMIT")}
        >
          LIMIT
        </ToggleButton>
      </div>

      <div className="grid grid-cols-2 gap-main">
        <FieldGroup label="마진 모드">
          <select
            className="w-full rounded-main border border-main-light-gray px-main py-3 text-sm-custom"
            value={marginMode}
            onChange={(event) => setMarginMode(event.target.value as MarginMode)}
          >
            <option value="ISOLATED">ISOLATED</option>
            <option value="CROSS">CROSS</option>
          </select>
        </FieldGroup>
        <FieldGroup label="레버리지">
          <input
            className="w-full rounded-main border border-main-light-gray px-main py-3 text-sm-custom"
            type="number"
            min={1}
            max={50}
            value={leverage}
            onChange={(event) =>
              setLeverage(Number.parseInt(event.target.value || "1", 10))
            }
          />
        </FieldGroup>
      </div>

      <div className="grid grid-cols-2 gap-main">
        <FieldGroup label="수량">
          <input
            className="w-full rounded-main border border-main-light-gray px-main py-3 text-sm-custom"
            min="0.001"
            step="0.001"
            type="number"
            value={quantity}
            onChange={(event) => setQuantity(event.target.value)}
          />
        </FieldGroup>
        <FieldGroup label="지정가">
          <input
            className="w-full rounded-main border border-main-light-gray px-main py-3 text-sm-custom disabled:bg-main-light-gray/40"
            disabled={orderType === "MARKET"}
            min="0"
            step="0.1"
            type="number"
            value={limitPrice}
            onChange={(event) => {
              setLimitPrice(event.target.value);
              setIsLimitPriceDirty(true);
            }}
          />
        </FieldGroup>
      </div>

      <div className="rounded-main bg-main-light-gray/40 px-main py-4">
        <p className="text-xs-custom text-main-dark-gray/60">실행 규칙</p>
        <p className="mt-2 text-sm-custom text-main-dark-gray/80 break-keep">
          시장가 주문과 즉시 체결 가능한 지정가 주문은 taker 수수료가 적용되고,
          대기 상태로 남는 지정가 주문은 maker 수수료 기준으로 계산됩니다.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-main">
        <PreviewField label="현재 기준가" value={formatUsd(currentPrice)} />
        <PreviewField
          label="계산 상태"
          value={
            isPreviewPending
              ? "계산 중"
              : previewError
                ? "계산 실패"
                : preview
                  ? "실시간 갱신"
                  : "입력 대기"
          }
        />
        <PreviewField
          label="예상 진입가"
          value={
            preview ? formatUsd(preview.estimatedEntryPrice) : formatUsd(currentPrice)
          }
        />
        <PreviewField
          label="수수료 타입"
          value={
            preview
              ? preview.feeType
              : orderType === "MARKET"
                ? "TAKER"
                : "MAKER/TAKER"
          }
        />
        <PreviewField
          label="예상 수수료"
          value={preview ? formatUsd(preview.estimatedFee) : "-"}
        />
        <PreviewField
          label="필요 증거금"
          value={preview ? formatUsd(preview.estimatedInitialMargin) : "-"}
        />
        <PreviewField
          label="예상 청산가"
          value={
            preview?.estimatedLiquidationPrice
              ? formatUsd(preview.estimatedLiquidationPrice)
              : "-"
          }
        />
        <PreviewField
          label="체결 상태"
          value={
            preview
              ? preview.executable
                ? "즉시 체결"
                : "대기 주문"
              : "계산 대기"
          }
        />
      </div>

      {previewError && (
        <div className="rounded-main border border-rose-200 bg-rose-50 px-main py-3 text-sm-custom text-rose-600">
          {previewError}
        </div>
      )}

      <div className="grid grid-cols-2 gap-main">
        <Button
          className="bg-emerald-500 py-3 text-white hover:bg-emerald-600"
          disabled={!hasValidOrder || isPreviewPending || isSubmitPending}
          onClick={() => handleSubmit("LONG")}
        >
          {isSubmitPending && positionSide === "LONG" ? "LONG 전송 중..." : "LONG 진입"}
        </Button>
        <Button
          className="bg-rose-500 py-3 text-white hover:bg-rose-600"
          disabled={!hasValidOrder || isPreviewPending || isSubmitPending}
          onClick={() => handleSubmit("SHORT")}
        >
          {isSubmitPending && positionSide === "SHORT"
            ? "SHORT 전송 중..."
            : "SHORT 진입"}
        </Button>
      </div>
    </div>
  );
}

function buildOrderPayload({
  symbol,
  positionSide,
  orderType,
  marginMode,
  leverage,
  quantity,
  limitPrice,
}: {
  symbol: MarketSymbol;
  positionSide: Side;
  orderType: OrderType;
  marginMode: MarginMode;
  leverage: number;
  quantity: string;
  limitPrice: string;
}): OrderPreviewRequest | null {
  const parsedQuantity = Number.parseFloat(quantity);
  const parsedLimitPrice = Number.parseFloat(limitPrice);

  if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) {
    return null;
  }

  if (!Number.isInteger(leverage) || leverage < 1 || leverage > 50) {
    return null;
  }

  if (orderType === "LIMIT") {
    if (!Number.isFinite(parsedLimitPrice) || parsedLimitPrice <= 0) {
      return null;
    }
  }

  return {
    symbol,
    positionSide,
    orderType,
    marginMode,
    leverage,
    quantity: parsedQuantity,
    limitPrice: orderType === "LIMIT" ? parsedLimitPrice : null,
  };
}

function ToggleButton({
  active,
  tone = "primary",
  children,
  onClick,
}: {
  active: boolean;
  tone?: "primary" | "positive" | "negative";
  children: string;
  onClick: () => void;
}) {
  const activeClassName =
    tone === "negative"
      ? "border-rose-400 bg-rose-50 text-rose-600"
      : tone === "positive"
        ? "border-emerald-400 bg-emerald-50 text-emerald-600"
        : "border-main-blue bg-main-blue/10 text-main-blue";

  return (
    <button
      className={[
        "rounded-main border px-main py-3 text-sm-custom font-semibold transition-colors",
        active
          ? activeClassName
          : "border-main-light-gray text-main-dark-gray/60",
      ].join(" ")}
      onClick={onClick}
      type="button"
    >
      {children}
    </button>
  );
}

function FieldGroup({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-xs-custom text-main-dark-gray/60">{label}</span>
      {children}
    </label>
  );
}

function PreviewField({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-main border border-main-light-gray px-main py-3">
      <p className="text-xs-custom text-main-dark-gray/60">{label}</p>
      <p className="mt-2 text-sm-custom font-semibold text-main-dark-gray">
        {value}
      </p>
    </div>
  );
}
