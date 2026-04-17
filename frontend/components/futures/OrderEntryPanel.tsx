"use client";

import Button from "@/components/ui/shared/Button";
import type {
  OrderExecutionResponse,
  OrderPreviewRequest,
  OrderPreviewResponse,
} from "@/lib/futures-api";
import { formatUsd, type MarketSymbol } from "@/lib/markets";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
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
  const [isPreviewPending, setIsPreviewPending] = useState(false);
  const [isSubmitPending, setIsSubmitPending] = useState(false);

  useEffect(() => {
    if (!isLimitPriceDirty) {
      setLimitPrice(currentPrice.toString());
    }
  }, [currentPrice, isLimitPriceDirty]);

  const orderPayload = buildOrderPayload({
    symbol,
    positionSide,
    orderType,
    marginMode,
    leverage,
    quantity,
    limitPrice,
  });

  const hasValidOrder = orderPayload !== null;

  async function handlePreview() {
    if (!orderPayload) {
      toast.error("수량과 가격을 다시 확인해주세요.");
      return;
    }

    setIsPreviewPending(true);

    try {
      const response = await fetch("/proxy-futures/orders/preview", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(orderPayload),
      });

      const payload =
        (await response.json()) as ClientApiResponse<OrderPreviewResponse>;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message ?? "주문 미리보기에 실패했습니다.");
      }

      setPreview(payload.data);
      toast.success("주문 미리보기를 불러왔습니다.");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "주문 미리보기에 실패했습니다."
      );
    } finally {
      setIsPreviewPending(false);
    }
  }

  async function handleSubmit() {
    if (!orderPayload) {
      toast.error("수량과 가격을 다시 확인해주세요.");
      return;
    }

    setIsSubmitPending(true);

    try {
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

      setPreview(null);
      toast.success(
        payload.data.status === "FILLED"
          ? `${symbol} 주문이 즉시 체결되었습니다.`
          : `${symbol} 지정가 주문이 대기열에 등록되었습니다.`
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
          onClick={() => setPositionSide("LONG")}
        >
          LONG
        </ToggleButton>
        <ToggleButton
          active={positionSide === "SHORT"}
          tone="danger"
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
            type="number"
            min="0.001"
            step="0.001"
            value={quantity}
            onChange={(event) => setQuantity(event.target.value)}
          />
        </FieldGroup>
        <FieldGroup label="지정가">
          <input
            className="w-full rounded-main border border-main-light-gray px-main py-3 text-sm-custom disabled:bg-main-light-gray/40"
            type="number"
            min="0"
            step="0.1"
            value={limitPrice}
            disabled={orderType === "MARKET"}
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

      {preview ? (
        <div className="grid grid-cols-2 gap-main">
          <PreviewField
            label="예상 진입가"
            value={formatUsd(preview.estimatedEntryPrice)}
          />
          <PreviewField label="수수료 타입" value={preview.feeType} />
          <PreviewField
            label="예상 수수료"
            value={formatUsd(preview.estimatedFee)}
          />
          <PreviewField
            label="필요 증거금"
            value={formatUsd(preview.estimatedInitialMargin)}
          />
          <PreviewField
            label="예상 청산가"
            value={
              preview.estimatedLiquidationPrice
                ? formatUsd(preview.estimatedLiquidationPrice)
                : "-"
            }
          />
          <PreviewField
            label="즉시 체결 여부"
            value={preview.executable ? "즉시 체결" : "대기 주문"}
          />
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-main">
          <PreviewField label="현재 기준가" value={formatUsd(currentPrice)} />
          <PreviewField
            label="수수료"
            value={orderType === "MARKET" ? "TAKER 0.05%" : "MAKER/TAKER 판정"}
          />
        </div>
      )}

      <div className="grid grid-cols-2 gap-main">
        <Button
          className="py-3"
          disabled={!hasValidOrder || isPreviewPending || isSubmitPending}
          onClick={handlePreview}
        >
          {isPreviewPending ? "미리보기 계산 중..." : "주문 미리보기"}
        </Button>
        <Button
          className="py-3"
          disabled={!hasValidOrder || isPreviewPending || isSubmitPending}
          onClick={handleSubmit}
          variant={positionSide === "SHORT" ? "danger" : "primary"}
        >
          {isSubmitPending ? "주문 전송 중..." : "주문 실행"}
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
  tone?: "primary" | "danger";
  children: string;
  onClick: () => void;
}) {
  const activeClassName =
    tone === "danger"
      ? "border-main-red bg-main-red/10 text-main-red"
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
