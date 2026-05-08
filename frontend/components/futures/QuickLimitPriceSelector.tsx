"use client";

import { formatUsd, type MarketSymbol } from "@/lib/markets";

type Props = {
  symbol: MarketSymbol;
  lastPrice: number;
  markPrice: number;
  indexPrice: number;
  change24h: number;
  onSelectPrice: (price: number) => void;
};

type PriceRow = {
  key: string;
  label: string;
  price: number;
  tone: "ask" | "bid";
};

const ROWS_PER_SIDE = 5;
const EMPTY_PRICE_ROWS = {
  asks: [] as PriceRow[],
  bids: [] as PriceRow[],
  lastPrice: null,
  step: 0,
};

export default function QuickLimitPriceSelector({
  symbol,
  lastPrice,
  markPrice,
  indexPrice,
  change24h,
  onSelectPrice,
}: Props) {
  const rows = buildQuickLimitPriceRows(lastPrice);
  const hasSelectablePrice = rows.lastPrice !== null;

  return (
    <aside
      aria-label={`${symbol} quick limit price selector`}
      className="sticky top-4 h-fit rounded-main border border-main-light-gray bg-white p-main shadow-sm"
    >
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <p className="text-sm-custom font-bold text-main-dark-gray">
            Quick Limit
          </p>
          <p className="mt-1 text-xs-custom leading-relaxed text-main-dark-gray/50">
            Order-book inspired price picks, without depth.
          </p>
        </div>
        <span
          className={[
            "rounded-full px-2 py-1 text-[11px] font-bold",
            change24h >= 0
              ? "bg-emerald-50 text-emerald-600"
              : "bg-rose-50 text-rose-600",
          ].join(" ")}
        >
          LIMIT
        </span>
      </div>

      {hasSelectablePrice ? (
        <div className="grid gap-1" aria-label="Quick limit price rows">
          {rows.asks.map((row) => (
            <PriceButton
              key={row.key}
              row={row}
              onSelectPrice={onSelectPrice}
            />
          ))}

          <button
            aria-label={`Select latest price ${formatPriceInput(rows.lastPrice)} USDT for ${symbol} limit order`}
            className={[
              "my-1 rounded-main border px-2.5 py-2 text-left transition-colors",
              "border-main-light-gray bg-main-light-gray/25 hover:border-main-blue/40 hover:bg-main-blue/5",
              "focus-visible:outline focus-visible:outline-2 focus-visible:outline-main-blue/50",
            ].join(" ")}
            onClick={() => onSelectPrice(rows.lastPrice)}
            type="button"
          >
            <span className="block text-[11px] font-semibold uppercase tracking-wide text-main-dark-gray/45">
              Last price
            </span>
            <span className="mt-0.5 block text-base-custom font-black text-main-dark-gray">
              {formatUsd(rows.lastPrice)}
            </span>
          </button>

          {rows.bids.map((row) => (
            <PriceButton
              key={row.key}
              row={row}
              onSelectPrice={onSelectPrice}
            />
          ))}
        </div>
      ) : (
        <div
          aria-live="polite"
          className="rounded-main border border-dashed border-main-light-gray bg-main-light-gray/20 px-3 py-4 text-xs-custom leading-relaxed text-main-dark-gray/55"
        >
          Price unavailable. Quick limit picks appear after a valid market
          snapshot arrives.
        </div>
      )}

      <div className="mt-3 grid gap-1.5 border-t border-main-light-gray pt-3 text-[11px] font-semibold text-main-dark-gray/55">
        <ReferenceLine label="Mark" value={markPrice} />
        <ReferenceLine label="Index" value={indexPrice} />
      </div>
    </aside>
  );
}

export function buildQuickLimitPriceRows(lastPrice: number) {
  const safeLastPrice = normalizePrice(lastPrice);
  if (safeLastPrice === null) {
    return EMPTY_PRICE_ROWS;
  }

  const step = getPriceStep(safeLastPrice);
  const asks: PriceRow[] = [];
  const bids: PriceRow[] = [];

  for (let index = ROWS_PER_SIDE; index >= 1; index -= 1) {
    const price = roundValidPrice(safeLastPrice + step * index);
    asks.push({
      key: `ask-${index}-${price}`,
      label: `Ask +${index}`,
      price,
      tone: "ask",
    });
  }

  for (let index = 1; index <= ROWS_PER_SIDE; index += 1) {
    const price = roundValidPrice(Math.max(step, safeLastPrice - step * index));
    bids.push({
      key: `bid-${index}-${price}`,
      label: `Bid -${index}`,
      price,
      tone: "bid",
    });
  }

  return { asks, bids, lastPrice: safeLastPrice, step };
}

function PriceButton({
  row,
  onSelectPrice,
}: {
  row: PriceRow;
  onSelectPrice: (price: number) => void;
}) {
  const isAsk = row.tone === "ask";

  return (
    <button
      aria-label={`Select ${formatPriceInput(row.price)} USDT ${row.label} as limit price`}
      className={[
        "group flex items-center justify-between gap-2 rounded-main px-2.5 py-1.5",
        "text-left transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-main-blue/50",
        isAsk
          ? "bg-rose-50/55 text-rose-600 hover:bg-rose-100/75"
          : "bg-emerald-50/60 text-emerald-600 hover:bg-emerald-100/80",
      ].join(" ")}
      onClick={() => onSelectPrice(row.price)}
      type="button"
    >
      <span className="text-[11px] font-bold uppercase tracking-wide opacity-65">
        {row.label}
      </span>
      <span className="font-mono text-sm-custom font-black tabular-nums">
        {formatUsd(row.price)}
      </span>
    </button>
  );
}

function ReferenceLine({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex items-center justify-between gap-2">
      <span>{label}</span>
      <span className="font-mono text-main-dark-gray tabular-nums">
        {formatUsd(value)}
      </span>
    </div>
  );
}

function getPriceStep(price: number) {
  if (price >= 10_000) {
    return 10;
  }

  if (price >= 1_000) {
    return 1;
  }

  if (price >= 100) {
    return 0.1;
  }

  return 0.01;
}

function normalizePrice(price: number) {
  if (!Number.isFinite(price) || price <= 0) {
    return null;
  }

  return roundValidPrice(price);
}

function roundValidPrice(price: number) {
  return Number(price.toFixed(1));
}

function formatPriceInput(price: number) {
  return price.toFixed(1);
}
