"use client";

import type {
  FuturesAccountSummary,
  FuturesPosition,
  OrderExecutionResponse,
  OrderPreviewRequest,
  OrderPreviewResponse,
} from "@/lib/futures-api";
import { formatPercent, formatUsd, type MarketSymbol } from "@/lib/markets";
import {
  calculateMaxOpenMarketQuantity,
  formatFlooredQuantity,
} from "@/lib/order-entry-quantity";
import Modal from "@/components/ui/Modal";
import { CircleHelp } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import { toast } from "react-toastify";

type Props = {
  symbol: MarketSymbol;
  currentPrice: number;
  isAuthenticated: boolean;
  accountSummary: FuturesAccountSummary | null;
  positions?: FuturesPosition[];
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

type ApiErrorPayload = {
  code?: string | null;
  message?: string | null;
  success?: boolean;
};

type ClosePositionResponse = {
  symbol: MarketSymbol;
  closedQuantity: number;
  realizedPnl: number;
  grantedPoint: number;
};

const MIN_LEVERAGE = 1;
const MAX_LEVERAGE = 50;
const DEFAULT_MARGIN_MODE: MarginMode = "CROSS";
const DEFAULT_LEVERAGE = 10;
const NO_CLOSE_POSITION_MESSAGE = "종료할 포지션이 없습니다.";

export default function OrderEntryPanel({
  symbol,
  currentPrice,
  isAuthenticated,
  accountSummary,
  positions = [],
}: Props) {
  const router = useRouter();
  const [positionSide, setPositionSide] = useState<Side>("LONG");
  const [ticketMode, setTicketMode] = useState<TicketMode>("OPEN");
  const [orderType, setOrderType] = useState<OrderType>("LIMIT");
  const [marginMode, setMarginMode] = useState<MarginMode>(DEFAULT_MARGIN_MODE);
  const [leverage, setLeverage] = useState(DEFAULT_LEVERAGE);
  const [quantity, setQuantity] = useState("0.01");
  const [limitPrice, setLimitPrice] = useState(currentPrice.toFixed(1));
  const [isLimitPriceDirty, setIsLimitPriceDirty] = useState(false);
  const priceSnapshotSymbolRef = useRef(symbol);
  const [isLeverageModalOpen, setIsLeverageModalOpen] = useState(false);
  const [preview, setPreview] = useState<OrderPreviewResponse | null>(null);
  const [isPreviewPending, setIsPreviewPending] = useState(false);
  const [isSubmitPending, setIsSubmitPending] = useState(false);
  const [isLeverageApplyPending, setIsLeverageApplyPending] = useState(false);
  const [inlineErrorMessage, setInlineErrorMessage] = useState<string | null>(
    null
  );

  useEffect(() => {
    if (priceSnapshotSymbolRef.current !== symbol) {
      priceSnapshotSymbolRef.current = symbol;
      setLimitPrice(currentPrice.toFixed(1));
      setIsLimitPriceDirty(false);
    }
  }, [currentPrice, symbol]);

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
  const hasValidOrder = orderPayload !== null;
  const parsedQuantity = Number.parseFloat(quantity);
  const effectivePrice =
    orderType === "LIMIT" ? Number.parseFloat(limitPrice) : currentPrice;
  const orderNotional =
    Number.isFinite(parsedQuantity) && Number.isFinite(effectivePrice)
      ? parsedQuantity * effectivePrice
      : 0;
  const costEstimate =
    preview?.estimatedInitialMargin ??
    (leverage > 0 ? orderNotional / leverage : 0);
  const baseAsset = symbol.replace("USDT", "");
  const availableBalance = accountSummary?.available ?? 0;
  const selectedSidePosition = useMemo(
    () => findPositionForSide(positions, symbol, positionSide),
    [positionSide, positions, symbol]
  );
  const matchingPosition = useMemo(
    () => findMatchingPosition(positions, symbol, positionSide, marginMode),
    [marginMode, positionSide, positions, symbol]
  );
  const isMarginModeLocked = selectedSidePosition !== null;
  const maxOpenQuantity =
    Number.isFinite(effectivePrice) && effectivePrice > 0
      ? orderType === "MARKET"
        ? calculateMaxOpenMarketQuantity(
            availableBalance,
            leverage,
            effectivePrice
          )
        : (availableBalance * leverage) / effectivePrice
      : 0;
  const maxCloseQuantity = matchingPosition?.quantity ?? 0;
  const quantityControlMax =
    ticketMode === "CLOSE" ? maxCloseQuantity : maxOpenQuantity;
  const quantityPercent =
    quantityControlMax > 0 && Number.isFinite(parsedQuantity)
      ? clamp(Math.round((parsedQuantity / quantityControlMax) * 100), 0, 100)
      : 0;
  const closePositionLabel = matchingPosition
    ? `${formatQuantityInput(matchingPosition.quantity)} ${baseAsset}`
    : "-";
  const isCloseMode = ticketMode === "CLOSE";
  const longButtonLabel = isCloseMode ? "Close long" : "Open long";
  const shortButtonLabel = isCloseMode ? "Close short" : "Open short";

  useEffect(() => {
    if (selectedSidePosition) {
      setMarginMode(selectedSidePosition.marginMode);
      setLeverage(selectedSidePosition.leverage);
      return;
    }

    setMarginMode(DEFAULT_MARGIN_MODE);
    setLeverage(DEFAULT_LEVERAGE);
  }, [selectedSidePosition]);

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
    const nextSidePosition = findPositionForSide(positions, symbol, nextSide);
    const payload = buildOrderPayload({
      symbol,
      positionSide: nextSide,
      orderType,
      marginMode: nextSidePosition?.marginMode ?? marginMode,
      leverage: nextSidePosition?.leverage ?? leverage,
      quantity,
      limitPrice,
    });

    if (!isAuthenticated || !payload) {
      toast.error("수량과 가격을 다시 확인해주세요.");
      return;
    }

    setInlineErrorMessage(null);
    setPositionSide(nextSide);

    if (ticketMode === "CLOSE") {
      const closePosition = findMatchingPosition(
        positions,
        symbol,
        nextSide,
        payload.marginMode
      );

      if (closePosition && payload.quantity > closePosition.quantity) {
        setInlineErrorMessage("보유 수량 이하로 입력해주세요.");
        return;
      }
    }

    setIsSubmitPending(true);

    try {
      if (ticketMode === "CLOSE") {
        await submitCloseOrder(payload);
      } else {
        await submitOpenOrder(payload);
      }
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "주문 생성에 실패했습니다.";

      if (ticketMode === "CLOSE") {
        setInlineErrorMessage(message);
      } else {
        toast.error(message);
      }
    } finally {
      setIsSubmitPending(false);
    }
  }

  async function submitOpenOrder(payload: OrderPreviewRequest) {
    const response = await fetch("/proxy-futures/orders", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    const execution =
      (await readJson<ClientApiResponse<OrderExecutionResponse>>(response)) ??
      null;

    if (!response.ok || !execution?.success || !execution.data) {
      throw new Error(execution?.message ?? "주문 생성에 실패했습니다.");
    }

    setPreview(null);
    toast.success(
      execution.data.status === "FILLED"
        ? `${symbol} 주문이 즉시 체결되었습니다.`
        : `${symbol} 지정가 주문이 대기열에 등록되었습니다.`
    );
    router.refresh();
  }

  async function submitCloseOrder(payload: OrderPreviewRequest) {
    const response = await fetch("/proxy-futures/positions/close", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(toClosePositionRequest(payload)),
    });

    const result =
      (await readJson<
        ClientApiResponse<ClosePositionResponse> & ApiErrorPayload
      >(response)) ?? null;

    if (!response.ok || !result?.success || !result.data) {
      if (isPositionNotFound(result)) {
        throw new Error(NO_CLOSE_POSITION_MESSAGE);
      }

      throw new Error(result?.message ?? "포지션 종료 주문에 실패했습니다.");
    }

    setPreview(null);
    toast.success(
      result.data.closedQuantity > 0
        ? `${symbol} 포지션 종료가 완료되었습니다.`
        : `${symbol} 종료 지정가 주문이 등록되었습니다.`
    );
    router.refresh();
  }

  async function handleApplyLeverage(nextLeverage: number) {
    const safeLeverage = clampLeverage(nextLeverage);

    if (!selectedSidePosition) {
      setLeverage(safeLeverage);
      setIsLeverageModalOpen(false);
      setInlineErrorMessage(null);
      return;
    }

    setIsLeverageApplyPending(true);
    setInlineErrorMessage(null);

    try {
      const response = await fetch("/proxy-futures/positions/leverage", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          symbol,
          positionSide,
          marginMode: selectedSidePosition.marginMode,
          leverage: safeLeverage,
        }),
      });
      const result =
        (await readJson<ClientApiResponse<FuturesPosition>>(response)) ?? null;

      if (!response.ok || !result?.success || !result.data) {
        throw new Error(result?.message ?? "레버리지 변경에 실패했습니다.");
      }

      setMarginMode(result.data.marginMode);
      setLeverage(result.data.leverage);
      setIsLeverageModalOpen(false);
      toast.success(
        `${symbol} ${positionSide} 레버리지를 ${result.data.leverage}x로 적용했습니다.`
      );
      router.refresh();
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "레버리지 변경에 실패했습니다.";
      setInlineErrorMessage(message);
    } finally {
      setIsLeverageApplyPending(false);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-3 gap-2">
        <SelectControl
          disabled={isMarginModeLocked}
          label="마진"
          value={marginMode}
          onChange={(value) => {
            setMarginMode(value as MarginMode);
            setInlineErrorMessage(null);
          }}
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
        <SideToggle
          onChange={(side) => {
            setPositionSide(side);
            setInlineErrorMessage(null);
          }}
          value={positionSide}
        />
      </div>

      <SegmentedControl
        options={[
          { label: "Open", value: "OPEN" },
          { label: "Close", value: "CLOSE" },
        ]}
        value={ticketMode}
        onChange={(value) => {
          setTicketMode(value as TicketMode);
          setInlineErrorMessage(null);
          setPreview(null);
        }}
      />

      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-4">
          {(["LIMIT", "MARKET"] as OrderType[]).map((type) => (
            <button
              className={[
                "border-b-2 pb-1 text-sm-custom font-semibold transition-colors",
                orderType === type
                  ? "border-main-dark-gray text-main-dark-gray"
                  : "border-transparent text-main-dark-gray/45 hover:text-main-dark-gray",
              ].join(" ")}
              key={type}
              onClick={() => {
                setOrderType(type);
                setInlineErrorMessage(null);
              }}
              type="button"
            >
              {type === "LIMIT" ? "Limit" : "Market"}
            </button>
          ))}
          <OrderHelpTooltip />
        </div>
        <span className="text-xs-custom font-semibold text-main-dark-gray/45">
          Post only
        </span>
      </div>

      <div className="flex items-center justify-between text-xs-custom text-main-dark-gray/60">
        <span>{isCloseMode ? "Position" : "Available"}</span>
        <span className="font-semibold text-main-dark-gray">
          {isCloseMode ? closePositionLabel : formatUsd(availableBalance)}
        </span>
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
            setInlineErrorMessage(null);
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
          onChange={(event) => {
            setQuantity(event.target.value);
            setInlineErrorMessage(null);
          }}
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
          const nextQuantity = (quantityControlMax * nextPercent) / 100;
          setQuantity(formatQuantityInput(nextQuantity));
          setInlineErrorMessage(null);
        }}
        onInput={(event) => {
          const nextPercent = Number.parseInt(event.currentTarget.value, 10);
          const nextQuantity = (quantityControlMax * nextPercent) / 100;
          setQuantity(formatQuantityInput(nextQuantity));
          setInlineErrorMessage(null);
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
        <SummaryLine
          label={isCloseMode ? "Close value" : "Cost"}
          value={formatUsd(isCloseMode ? orderNotional : costEstimate)}
        />
        <SummaryLine
          label={
            isCloseMode ? "Held size" : isPreviewPending ? "Risk" : "Liq. price"
          }
          value={
            isCloseMode
              ? closePositionLabel
              : isPreviewPending
              ? "계산 중"
              : formatLiquidationPrice(
                  preview?.estimatedLiquidationPrice,
                  preview?.estimatedLiquidationPriceType
                )
          }
        />
      </div>

      {inlineErrorMessage ? (
        <p className="rounded-main bg-rose-50 px-3 py-2 text-xs-custom font-semibold text-main-red">
          {inlineErrorMessage}
        </p>
      ) : null}

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
                : longButtonLabel}
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
                : shortButtonLabel}
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
          <span className="text-xs-custom font-semibold text-main-blue">USDT</span>
        </div>
        <div className="mt-3 grid gap-2">
          <AccountLine
            label="USDT balance"
            value={accountSummary ? formatUsd(accountSummary.usdtBalance) : "-"}
          />
          <AccountLine
            label="Wallet balance"
            value={accountSummary ? formatUsd(accountSummary.walletBalance) : "-"}
          />
          <AccountLine
            label="Available"
            value={accountSummary ? formatUsd(accountSummary.available) : "-"}
          />
          <AccountLine
            label="Unrealized PnL"
            value={
              accountSummary
                ? formatUsd(accountSummary.totalUnrealizedPnl)
                : "-"
            }
          />
          <AccountLine
            label="ROI"
            value={accountSummary ? formatPercent(accountSummary.roi * 100) : "-"}
          />
        </div>
      </div>

      <LeverageModal
        isApplying={isLeverageApplyPending}
        leverage={leverage}
        onClose={() => setIsLeverageModalOpen(false)}
        onApply={handleApplyLeverage}
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

function toClosePositionRequest(payload: OrderPreviewRequest) {
  return {
    symbol: payload.symbol,
    positionSide: payload.positionSide,
    marginMode: payload.marginMode,
    quantity: payload.quantity,
    orderType: payload.orderType,
    limitPrice: payload.limitPrice,
  };
}

function findMatchingPosition(
  positions: FuturesPosition[],
  symbol: MarketSymbol,
  positionSide: Side,
  marginMode: MarginMode
): FuturesPosition | null {
  return (
    positions.find(
      (position) =>
        position.symbol === symbol &&
        position.positionSide === positionSide &&
        position.marginMode === marginMode
    ) ?? null
  );
}

function findPositionForSide(
  positions: FuturesPosition[],
  symbol: MarketSymbol,
  positionSide: Side
): FuturesPosition | null {
  return (
    positions.find(
      (position) =>
        position.symbol === symbol && position.positionSide === positionSide
    ) ?? null
  );
}

async function readJson<T>(response: Response): Promise<T | null> {
  try {
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

function isPositionNotFound(payload: ApiErrorPayload | null): boolean {
  const message = payload?.message ?? "";

  return (
    payload?.code === "POSITION_NOT_FOUND" ||
    message === NO_CLOSE_POSITION_MESSAGE ||
    (message.includes("포지션") &&
      (message.includes("없") || message.includes("찾을 수")))
  );
}

function SelectControl({
  disabled = false,
  label,
  options,
  value,
  onChange,
}: {
  disabled?: boolean;
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
        className="mt-1 w-full bg-transparent text-xs-custom font-bold text-main-dark-gray outline-none disabled:text-main-dark-gray/45"
        disabled={disabled}
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

function SideToggle({
  value,
  onChange,
}: {
  value: Side;
  onChange: (value: Side) => void;
}) {
  return (
    <div
      aria-label="Position side"
      className="grid grid-cols-2 rounded-main border border-main-light-gray bg-main-light-gray/20 p-1"
    >
      {(["LONG", "SHORT"] as Side[]).map((side) => {
        const active = value === side;
        return (
          <button
            className={[
              "rounded-main px-2 py-2 text-xs-custom font-bold transition-colors",
              active
                ? "bg-white text-main-dark-gray shadow-sm"
                : "text-main-dark-gray/45 hover:text-main-dark-gray",
            ].join(" ")}
            key={side}
            onClick={() => onChange(side)}
            type="button"
          >
            {side === "LONG" ? "L" : "S"}
          </button>
        );
      })}
    </div>
  );
}

function LeverageModal({
  isApplying,
  leverage,
  open,
  onApply,
  onClose,
}: {
  isApplying: boolean;
  leverage: number;
  open: boolean;
  onApply: (value: number) => void;
  onClose: () => void;
}) {
  const [draftLeverage, setDraftLeverage] = useState(leverage);

  useEffect(() => {
    if (open) {
      setDraftLeverage(leverage);
    }
  }, [leverage, open]);

  function setNextLeverage(value: number) {
    setDraftLeverage(clampLeverage(value));
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
              value={draftLeverage}
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
          value={draftLeverage}
        />
        <div
          className={[
            "mt-2 flex items-center justify-between text-xs-custom",
            "font-semibold text-main-dark-gray/45",
          ].join(" ")}
        >
          <span>1x</span>
          <span>{draftLeverage}x</span>
          <span>50x</span>
        </div>

        <div className="mt-5 grid grid-cols-4 gap-2">
          {[1, 10, 25, 50].map((value) => (
            <button
              className={[
                "rounded-main border px-3 py-2 text-sm-custom font-semibold",
                draftLeverage === value
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
            "text-sm-custom font-bold text-white disabled:cursor-not-allowed disabled:opacity-60",
          ].join(" ")}
          disabled={isApplying}
          onClick={() => onApply(draftLeverage)}
          type="button"
        >
          {isApplying ? "적용 중..." : "적용"}
        </button>
      </div>
    </Modal>
  );
}

function OrderHelpTooltip() {
  return (
    <div className="group relative inline-flex">
      <button
        aria-label="주문 도움말"
        className={[
          "inline-flex h-6 w-6 items-center justify-center rounded-full",
          "border border-main-light-gray text-main-dark-gray/55",
          "transition-colors hover:text-main-dark-gray focus:outline-none",
          "focus:ring-2 focus:ring-main-blue/30",
        ].join(" ")}
        type="button"
      >
        <CircleHelp size={14} aria-hidden="true" />
      </button>
      <div
        className={[
          "pointer-events-none absolute left-1/2 top-8 z-50 w-72",
          "max-w-[calc(100vw-2rem)] -translate-x-1/2 rounded-main",
          "border border-main-light-gray bg-white p-3 text-[11px]",
          "font-medium leading-5 text-main-dark-gray/70 opacity-0 shadow-lg",
          "transition-opacity group-focus-within:opacity-100 group-hover:opacity-100",
        ].join(" ")}
      >
        <p className="font-bold text-main-dark-gray">주문 도움말</p>
        <p className="mt-2">
          Cross는 계정 가용 증거금을 함께 쓰고, Isolated는 포지션 단위로
          증거금을 분리합니다. 기존 포지션이 있으면 마진 모드는 바꿀 수
          없습니다.
        </p>
        <p className="mt-2">
          레버리지는 포지션 증거금과 청산가에 영향을 줍니다. 기존 포지션의
          레버리지는 적용 버튼을 누르는 순간 포지션에 반영됩니다.
        </p>
        <p className="mt-2">
          Limit은 지정 가격에 대기하고, Market은 현재 시장가로 즉시
          체결합니다. Long은 상승 방향, Short은 하락 방향 포지션입니다.
        </p>
        <p className="mt-2">
          Cost는 예상 증거금, Liq. price는 예상 청산가, Fee는 주문 체결
          수수료입니다. Cross 청산가는 계정 단위 평가값이며 여러 심볼 포지션이
          있으면 현재 다른 심볼 가격을 고정한 추정값으로 표시됩니다.
        </p>
      </div>
    </div>
  );
}

function formatLiquidationPrice(
  price: number | null | undefined,
  type: "EXACT" | "ESTIMATED" | "UNAVAILABLE" | undefined
) {
  if (!price || type === "UNAVAILABLE") {
    return "-";
  }
  return type === "ESTIMATED" ? `${formatUsd(price)} (Est.)` : formatUsd(price);
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
  return formatFlooredQuantity(value);
}
