"use client";

import type {
  OrderExecutionResponse,
  OrderPreviewRequest,
  OrderPreviewResponse,
} from "@/lib/futures-api";
import { formatUsd, type MarketSymbol } from "@/lib/markets";
import Modal from "@/components/ui/Modal";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";

type Props = {
  symbol: MarketSymbol;
  currentPrice: number;
  isAuthenticated: boolean;
};

type Side = "LONG" | "SHORT";
type OrderType = "MARKET" | "LIMIT";
type MarginMode = "ISOLATED" | "CROSS";
type TicketMode = "OPEN" | "CLOSE";

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

const AVAILABLE_BALANCE_USDT = 100_000;
const MIN_LEVERAGE = 1;
const MAX_LEVERAGE = 50;

export default function OrderEntryPanel({
  symbol,
  currentPrice,
  isAuthenticated,
}: Props) {
  const router = useRouter();
  const [positionSide, setPositionSide] = useState<Side>("LONG");
  const [ticketMode, setTicketMode] = useState<TicketMode>("OPEN");
  const [orderType, setOrderType] = useState<OrderType>("LIMIT");
  const [marginMode, setMarginMode] = useState<MarginMode>("ISOLATED");
  const [leverage, setLeverage] = useState(20);
  const [quantity, setQuantity] = useState("0.01");
  const [limitPrice, setLimitPrice] = useState(currentPrice.toFixed(1));
  const [isLimitPriceDirty, setIsLimitPriceDirty] = useState(false);
  const [isLeverageModalOpen, setIsLeverageModalOpen] = useState(false);
  const [preview, setPreview] = useState<OrderPreviewResponse | null>(null);
  const [isPreviewPending, setIsPreviewPending] = useState(false);
  const [isSubmitPending, setIsSubmitPending] = useState(false);

  useEffect(() => {
    if (!isLimitPriceDirty) {
      setLimitPrice(currentPrice.toFixed(1));
    }
  }, [currentPrice, isLimitPriceDirty]);

  const orderPayload = useMemo(
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
  const hasValidOrder = orderPayload !== null && ticketMode === "OPEN";
  const parsedQuantity = Number.parseFloat(quantity);
  const effectivePrice =
    orderType === "LIMIT" ? Number.parseFloat(limitPrice) : currentPrice;
  const orderNotional =
    Number.isFinite(parsedQuantity) && Number.isFinite(effectivePrice)
      ? parsedQuantity * effectivePrice
      : 0;
  const costEstimate =
    preview?.estimatedInitialMargin ?? (leverage > 0 ? orderNotional / leverage : 0);
  const baseAsset = symbol.replace("USDT", "");
  const maxQuantity =
    Number.isFinite(effectivePrice) && effectivePrice > 0
      ? (AVAILABLE_BALANCE_USDT * leverage) / effectivePrice
      : 0;
  const quantityPercent =
    maxQuantity > 0 && Number.isFinite(parsedQuantity)
      ? clamp(Math.round((parsedQuantity / maxQuantity) * 100), 0, 100)
      : 0;

  useEffect(() => {
    if (!isAuthenticated || !orderPayload || ticketMode !== "OPEN") {
      setPreview(null);
      return;
    }

    const controller = new AbortController();
    const timer = window.setTimeout(async () => {
      setIsPreviewPending(true);

      try {
        const response = await fetch("/proxy-futures/orders/preview", {
          body: JSON.stringify(orderPayload),
          headers: {
            "Content-Type": "application/json",
          },
          method: "POST",
          signal: controller.signal,
        });
        const payload =
          (await response.json()) as ClientApiResponse<OrderPreviewResponse>;

        if (!response.ok || !payload.success || !payload.data) {
          throw new Error(payload.message ?? "주문 미리보기에 실패했습니다.");
        }

        setPreview(payload.data);
      } catch (error) {
        if (!controller.signal.aborted) {
          setPreview(null);
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsPreviewPending(false);
        }
      }
    }, 350);

    return () => {
      controller.abort();
      window.clearTimeout(timer);
    };
  }, [isAuthenticated, orderPayload, ticketMode]);

  async function handleSubmit(nextSide: Side) {
    const payload = buildOrderPayload({
      symbol,
      positionSide: nextSide,
      orderType,
      marginMode,
      leverage,
      quantity,
      limitPrice,
    });

    if (!isAuthenticated || !payload || ticketMode !== "OPEN") {
      toast.error("수량과 가격을 다시 확인해주세요.");
      return;
    }

    setPositionSide(nextSide);
    setIsSubmitPending(true);

    try {
      const response = await fetch("/proxy-futures/orders", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const execution =
        (await response.json()) as ClientApiResponse<OrderExecutionResponse>;

      if (!response.ok || !execution.success || !execution.data) {
        throw new Error(execution.message ?? "주문 생성에 실패했습니다.");
      }

      setPreview(null);
      toast.success(
        execution.data.status === "FILLED"
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
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-3 gap-2">
        <SelectControl
          label="마진"
          value={marginMode}
          onChange={(value) => setMarginMode(value as MarginMode)}
          options={[
            { label: "Isolated", value: "ISOLATED" },
            { label: "Cross", value: "CROSS" },
          ]}
        />
        <LeverageButton
          label="레버리지"
          leverage={leverage}
          onClick={() => setIsLeverageModalOpen(true)}
        />
        <button
          className={[
            "rounded-main border border-main-light-gray bg-main-light-gray/35",
            "px-3 py-2 text-xs-custom font-semibold text-main-dark-gray",
          ].join(" ")}
          type="button"
        >
          {positionSide === "LONG" ? "L" : "S"}
        </button>
      </div>

      <SegmentedControl
        options={[
          { label: "Open", value: "OPEN" },
          { label: "Close", value: "CLOSE", disabled: true },
        ]}
        value={ticketMode}
        onChange={(value) => setTicketMode(value as TicketMode)}
      />

      <div className="flex items-center justify-between gap-3">
        <div className="flex gap-4">
          {(["LIMIT", "MARKET"] as OrderType[]).map((type) => (
            <button
              className={[
                "border-b-2 pb-1 text-sm-custom font-semibold transition-colors",
                orderType === type
                  ? "border-main-dark-gray text-main-dark-gray"
                  : "border-transparent text-main-dark-gray/45 hover:text-main-dark-gray",
              ].join(" ")}
              key={type}
              onClick={() => setOrderType(type)}
              type="button"
            >
              {type === "LIMIT" ? "Limit" : "Market"}
            </button>
          ))}
        </div>
        <span className="text-xs-custom font-semibold text-main-dark-gray/45">
          Post only
        </span>
      </div>

      <div className="flex items-center justify-between text-xs-custom text-main-dark-gray/60">
        <span>Available</span>
        <span className="font-semibold text-main-dark-gray">100,000 USDT</span>
      </div>

      <TicketField label="Price">
        <input
          className={[
            "w-full bg-transparent text-sm-custom font-semibold",
            "text-main-dark-gray outline-none disabled:text-main-dark-gray/45",
          ].join(" ")}
          disabled={orderType === "MARKET"}
          min="0"
          onChange={(event) => {
            setLimitPrice(event.target.value);
            setIsLimitPriceDirty(true);
          }}
          step="0.1"
          type="number"
          value={limitPrice}
        />
        <span className="text-xs-custom font-semibold text-main-dark-gray/50">
          USDT
        </span>
      </TicketField>

      <TicketField label="Quantity">
        <input
          className="w-full bg-transparent text-sm-custom font-semibold text-main-dark-gray outline-none"
          min="0.001"
          onChange={(event) => setQuantity(event.target.value)}
          step="0.001"
          type="number"
          value={quantity}
        />
        <span className="text-xs-custom font-semibold text-main-dark-gray/50">
          {baseAsset}
        </span>
      </TicketField>

      <input
        aria-label="Quantity percent"
        className="h-1.5 w-full cursor-pointer accent-main-blue"
        max="100"
        min="0"
        onChange={(event) => {
          const nextPercent = Number.parseInt(event.target.value, 10);
          const nextQuantity = (maxQuantity * nextPercent) / 100;
          setQuantity(formatQuantityInput(nextQuantity));
        }}
        onInput={(event) => {
          const nextPercent = Number.parseInt(event.currentTarget.value, 10);
          const nextQuantity = (maxQuantity * nextPercent) / 100;
          setQuantity(formatQuantityInput(nextQuantity));
        }}
        step="1"
        type="range"
        value={quantityPercent}
      />
      <div className="flex items-center justify-between text-[11px] font-semibold text-main-dark-gray/45">
        <span>0%</span>
        <span>{quantityPercent}%</span>
        <span>100%</span>
      </div>

      <div className="grid grid-cols-2 gap-2 text-xs-custom">
        <SummaryLine label="Cost" value={formatUsd(costEstimate)} />
        <SummaryLine
          label={isPreviewPending ? "Risk" : "Liq. price"}
          value={
            isPreviewPending
              ? "계산 중"
              : preview?.estimatedLiquidationPrice
                ? formatUsd(preview.estimatedLiquidationPrice)
                : "-"
          }
        />
      </div>

      <label className="flex items-center gap-2 text-xs-custom font-semibold text-main-dark-gray/70">
        <input className="accent-main-blue" disabled type="checkbox" />
        TP/SL
      </label>

      <div className="grid grid-cols-2 gap-2">
        {isAuthenticated ? (
          <>
            <button
              className={[
                "rounded-main bg-emerald-500 px-3 py-3 text-sm-custom font-bold",
                "text-white transition-colors hover:bg-emerald-600",
                "disabled:cursor-not-allowed disabled:opacity-55",
              ].join(" ")}
              disabled={!hasValidOrder || isSubmitPending}
              onClick={() => handleSubmit("LONG")}
              type="button"
            >
              {isSubmitPending && positionSide === "LONG"
                ? "Sending..."
                : "Open long"}
            </button>
            <button
              className={[
                "rounded-main bg-main-red px-3 py-3 text-sm-custom font-bold",
                "text-white transition-colors hover:bg-main-red/90",
                "disabled:cursor-not-allowed disabled:opacity-55",
              ].join(" ")}
              disabled={!hasValidOrder || isSubmitPending}
              onClick={() => handleSubmit("SHORT")}
              type="button"
            >
              {isSubmitPending && positionSide === "SHORT"
                ? "Sending..."
                : "Open short"}
            </button>
          </>
        ) : (
          <>
            <Link
              className={[
                "rounded-main bg-white px-3 py-3 text-center text-sm-custom",
                "font-bold text-main-dark-gray shadow-sm ring-1 ring-main-light-gray",
                "transition-colors hover:bg-main-light-gray/30",
              ].join(" ")}
              href="/signup"
            >
              Sign up
            </Link>
            <Link
              className={[
                "rounded-main bg-main-dark-gray px-3 py-3 text-center",
                "text-sm-custom font-bold text-white transition-colors",
                "hover:bg-main-dark-gray/85",
              ].join(" ")}
              href="/login"
            >
              Log in
            </Link>
          </>
        )}
      </div>

      <div className="border-t border-main-light-gray pt-4">
        <div className="flex items-center justify-between">
          <p className="text-sm-custom font-bold text-main-dark-gray">Account</p>
          <span className="text-xs-custom font-semibold text-main-blue">PnL</span>
        </div>
        <div className="mt-3 grid gap-2">
          <AccountLine
            label="Fee"
            value={preview?.estimatedFee ? formatUsd(preview.estimatedFee) : "-"}
          />
          <AccountLine
            label="Entry"
            value={
              preview ? formatUsd(preview.estimatedEntryPrice) : formatUsd(currentPrice)
            }
          />
          <AccountLine label="Fee type" value={preview?.feeType ?? "-"} />
          <AccountLine
            label="Status"
            value={preview?.executable ? "Taker fill" : "Maker wait"}
          />
        </div>
      </div>

      <LeverageModal
        leverage={leverage}
        onChange={setLeverage}
        onClose={() => setIsLeverageModalOpen(false)}
        open={isLeverageModalOpen}
      />
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

function SelectControl({
  label,
  options,
  value,
  onChange,
}: {
  label: string;
  options: Array<{ label: string; value: string }>;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="rounded-main border border-main-light-gray bg-main-light-gray/20 px-2 py-2">
      <span className="block text-[10px] font-semibold uppercase text-main-dark-gray/45">
        {label}
      </span>
      <select
        className="mt-1 w-full bg-transparent text-xs-custom font-bold text-main-dark-gray outline-none"
        onChange={(event) => onChange(event.target.value)}
        value={value}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function LeverageButton({
  label,
  leverage,
  onClick,
}: {
  label: string;
  leverage: number;
  onClick: () => void;
}) {
  return (
    <button
      className="rounded-main border border-main-light-gray bg-main-light-gray/20 px-2 py-2 text-left"
      onClick={onClick}
      type="button"
    >
      <span className="block text-[10px] font-semibold uppercase text-main-dark-gray/45">
        {label}
      </span>
      <span className="mt-1 block text-xs-custom font-bold text-main-dark-gray">
        {leverage}x
      </span>
    </button>
  );
}

function LeverageModal({
  leverage,
  open,
  onChange,
  onClose,
}: {
  leverage: number;
  open: boolean;
  onChange: (value: number) => void;
  onClose: () => void;
}) {
  function setNextLeverage(value: number) {
    onChange(clampLeverage(value));
  }

  function handleSliderValue(value: string) {
    setNextLeverage(Number.parseInt(value, 10));
  }

  return (
    <Modal
      hasBackdropBlur={false}
      isEscapeClose
      isOpen={open}
      onClose={onClose}
    >
      <div className="w-[min(440px,calc(100vw-48px))] pr-6">
        <p className="text-lg-custom font-bold text-main-dark-gray">
          레버리지 조절
        </p>
        <p className="mt-2 text-sm-custom text-main-dark-gray/60">
          1x부터 50x까지 정수 단위로 설정할 수 있습니다.
        </p>

        <label className="mt-6 block">
          <span className="text-xs-custom font-semibold text-main-dark-gray/60">
            Leverage
          </span>
          <div
            className={[
              "mt-2 flex items-center gap-2 rounded-main border",
              "border-main-light-gray px-main py-3",
            ].join(" ")}
          >
            <input
              className={[
                "min-w-0 flex-1 bg-transparent text-2xl-custom font-bold",
                "text-main-dark-gray outline-none",
              ].join(" ")}
              max={MAX_LEVERAGE}
              min={MIN_LEVERAGE}
              onChange={(event) =>
                setNextLeverage(Number.parseInt(event.target.value || "1", 10))
              }
              step={1}
              type="number"
              value={leverage}
            />
            <span className="text-lg-custom font-bold text-main-dark-gray/50">
              x
            </span>
          </div>
        </label>

        <input
          aria-label="Leverage slider"
          className="mt-6 w-full cursor-pointer accent-main-blue"
          max={MAX_LEVERAGE}
          min={MIN_LEVERAGE}
          onChange={(event) => handleSliderValue(event.target.value)}
          onInput={(event) => handleSliderValue(event.currentTarget.value)}
          step={1}
          type="range"
          value={leverage}
        />
        <div
          className={[
            "mt-2 flex items-center justify-between text-xs-custom",
            "font-semibold text-main-dark-gray/45",
          ].join(" ")}
        >
          <span>1x</span>
          <span>{leverage}x</span>
          <span>50x</span>
        </div>

        <div className="mt-5 grid grid-cols-4 gap-2">
          {[1, 10, 25, 50].map((value) => (
            <button
              className={[
                "rounded-main border px-3 py-2 text-sm-custom font-semibold",
                leverage === value
                  ? "border-main-blue bg-main-blue/10 text-main-blue"
                  : "border-main-light-gray text-main-dark-gray/70",
              ].join(" ")}
              key={value}
              onClick={() => setNextLeverage(value)}
              type="button"
            >
              {value}x
            </button>
          ))}
        </div>

        <button
          className={[
            "mt-6 w-full rounded-main bg-main-dark-gray px-main py-3",
            "text-sm-custom font-bold text-white",
          ].join(" ")}
          onClick={onClose}
          type="button"
        >
          적용
        </button>
      </div>
    </Modal>
  );
}

function SegmentedControl({
  options,
  value,
  onChange,
}: {
  options: Array<{ label: string; value: string; disabled?: boolean }>;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="grid grid-cols-2 rounded-main bg-main-light-gray/45 p-1">
      {options.map((option) => {
        const active = option.value === value;
        return (
          <button
            className={[
              "rounded-main px-3 py-2 text-sm-custom font-semibold transition-colors",
              active
                ? "bg-white text-main-dark-gray shadow-sm"
                : "text-main-dark-gray/45 hover:text-main-dark-gray",
              option.disabled ? "cursor-not-allowed opacity-50" : "",
            ].join(" ")}
            disabled={option.disabled}
            key={option.value}
            onClick={() => onChange(option.value)}
            type="button"
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}

function TicketField({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <label
      className={[
        "flex items-center gap-3 rounded-main border border-main-light-gray",
        "bg-main-light-gray/25 px-3 py-3",
      ].join(" ")}
    >
      <span className="w-16 text-xs-custom font-semibold text-main-dark-gray/55">
        {label}
      </span>
      {children}
    </label>
  );
}

function SummaryLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-main bg-main-light-gray/35 px-3 py-2">
      <p className="text-main-dark-gray/50">{label}</p>
      <p className="mt-1 font-bold text-main-dark-gray">{value}</p>
    </div>
  );
}

function AccountLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 text-xs-custom">
      <span className="text-main-dark-gray/55">{label}</span>
      <span className="text-right font-semibold text-main-dark-gray">{value}</span>
    </div>
  );
}

function clampLeverage(value: number): number {
  if (!Number.isFinite(value)) {
    return MIN_LEVERAGE;
  }

  return clamp(Math.round(value), MIN_LEVERAGE, MAX_LEVERAGE);
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function formatQuantityInput(value: number): string {
  if (!Number.isFinite(value) || value <= 0) {
    return "0";
  }

  return value.toFixed(3);
}
