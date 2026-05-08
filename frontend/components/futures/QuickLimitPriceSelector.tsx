"use client";

import { useEffect, useMemo, useState } from "react";

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
  price: number;
  tone: "upper" | "lower";
};

type UnitOption = {
  label: string;
  value: number;
};

const ROWS_PER_SIDE = 7;
const STORAGE_KEY_PREFIX = "coin-zzickmock.quick-limit-price-unit";
const SYMBOL_UNIT_OPTIONS: Record<MarketSymbol, UnitOption[]> = {
  BTCUSDT: [
    { label: "0.1", value: 0.1 },
    { label: "1", value: 1 },
    { label: "10", value: 10 },
    { label: "100", value: 100 },
    { label: "1000", value: 1000 },
  ],
  ETHUSDT: [
    { label: "10", value: 10 },
    { label: "1", value: 1 },
    { label: "0.1", value: 0.1 },
    { label: "0.01", value: 0.01 },
  ],
};
const DEFAULT_UNIT_BY_SYMBOL: Record<MarketSymbol, number> = {
  BTCUSDT: 0.1,
  ETHUSDT: 0.01,
};
const PRICE_PRECISION_BY_SYMBOL: Record<MarketSymbol, number> = {
  BTCUSDT: 1,
  ETHUSDT: 2,
};
const EMPTY_PRICE_ROWS = {
  upperRows: [] as PriceRow[],
  lowerRows: [] as PriceRow[],
  lastPrice: null,
  unit: 0,
};

export default function QuickLimitPriceSelector({
  symbol,
  lastPrice,
  markPrice,
  indexPrice,
  change24h,
  onSelectPrice,
}: Props) {
  const unitOptions = SYMBOL_UNIT_OPTIONS[symbol];
  const [selectedUnit, setSelectedUnit] = useState(
    () => DEFAULT_UNIT_BY_SYMBOL[symbol]
  );

  useEffect(() => {
    setSelectedUnit(readStoredUnit(symbol, unitOptions));
  }, [symbol, unitOptions]);

  const resolvedSelectedUnit = resolveUnit(symbol, selectedUnit);
  const rows = useMemo(
    () => buildQuickLimitPriceRows(lastPrice, symbol, resolvedSelectedUnit),
    [lastPrice, resolvedSelectedUnit, symbol]
  );
  const hasSelectablePrice = rows.lastPrice !== null;

  return (
    <aside
      aria-label={`${symbol} quick limit price selector`}
      className="sticky top-4 h-fit rounded-main border border-main-light-gray bg-white p-main shadow-sm"
    >
      <div className="mb-3 flex items-center justify-between gap-3">
        <p className="text-sm-custom font-bold text-main-dark-gray">
          Quick Limit
        </p>
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

      <label className="mb-3 block text-[11px] font-bold uppercase tracking-wide text-main-dark-gray/45">
        Unit
        <select
          aria-label={`${symbol} quick limit unit`}
          className="mt-1 w-full rounded-main border border-main-light-gray bg-white px-2.5 py-2 text-sm-custom font-bold text-main-dark-gray outline-none transition-colors hover:border-main-blue/40 focus-visible:border-main-blue focus-visible:outline focus-visible:outline-2 focus-visible:outline-main-blue/30"
          name="quickLimitPriceUnit"
          onChange={(event) => {
            const nextUnit = parseUnitOption(event.target.value, unitOptions);
            setSelectedUnit(nextUnit);
            rememberUnit(symbol, nextUnit);
          }}
          value={resolvedSelectedUnit}
        >
          {unitOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label} USDT
            </option>
          ))}
        </select>
      </label>

      {hasSelectablePrice ? (
        <div className="grid gap-1" aria-label="Quick limit price rows">
          {rows.upperRows.map((row) => (
            <PriceButton key={row.key} row={row} onSelectPrice={onSelectPrice} />
          ))}

          <button
            aria-label={`Select latest price ${formatPriceInput(rows.lastPrice, rows.unit, symbol)} USDT for ${symbol} limit order`}
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
            <span className="mt-0.5 block text-base-custom font-black text-main-dark-gray tabular-nums">
              {formatUsd(rows.lastPrice)}
            </span>
          </button>

          {rows.lowerRows.map((row) => (
            <PriceButton key={row.key} row={row} onSelectPrice={onSelectPrice} />
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

export function buildQuickLimitPriceRows(
  lastPrice: number,
  symbol: MarketSymbol,
  unit: number
) {
  const safeUnit = resolveUnit(symbol, unit);
  const safeLastPrice = normalizePrice(lastPrice, symbol, safeUnit);
  if (safeLastPrice === null) {
    return EMPTY_PRICE_ROWS;
  }

  const upperRows: PriceRow[] = [];
  const lowerRows: PriceRow[] = [];

  for (let index = ROWS_PER_SIDE; index >= 1; index -= 1) {
    const price = roundValidPrice(
      safeLastPrice + safeUnit * index,
      symbol,
      safeUnit
    );
    upperRows.push({
      key: `upper-${index}-${price}`,
      price,
      tone: "upper",
    });
  }

  for (let index = 1; index <= ROWS_PER_SIDE; index += 1) {
    const price = roundValidPrice(
      Math.max(safeUnit, safeLastPrice - safeUnit * index),
      symbol,
      safeUnit
    );
    lowerRows.push({
      key: `lower-${index}-${price}`,
      price,
      tone: "lower",
    });
  }

  return { upperRows, lowerRows, lastPrice: safeLastPrice, unit: safeUnit };
}

function PriceButton({
  row,
  onSelectPrice,
}: {
  row: PriceRow;
  onSelectPrice: (price: number) => void;
}) {
  const isUpper = row.tone === "upper";

  return (
    <button
      aria-label={`Select ${formatUsd(row.price)} as limit price`}
      className={[
        "rounded-main px-2.5 py-1.5 text-left text-base-custom font-black tabular-nums",
        "transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-main-blue/50",
        isUpper
          ? "bg-rose-50/55 text-rose-600 hover:bg-rose-100/75"
          : "bg-emerald-50/60 text-emerald-600 hover:bg-emerald-100/80",
      ].join(" ")}
      onClick={() => onSelectPrice(row.price)}
      type="button"
    >
      {formatUsd(row.price)}
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

function readStoredUnit(symbol: MarketSymbol, unitOptions: UnitOption[]) {
  if (typeof window === "undefined") {
    return DEFAULT_UNIT_BY_SYMBOL[symbol];
  }

  return parseUnitOption(
    window.localStorage.getItem(getStorageKey(symbol)),
    unitOptions,
    DEFAULT_UNIT_BY_SYMBOL[symbol]
  );
}

function rememberUnit(symbol: MarketSymbol, unit: number) {
  window.localStorage.setItem(getStorageKey(symbol), String(unit));
}

function getStorageKey(symbol: MarketSymbol) {
  return `${STORAGE_KEY_PREFIX}.${symbol}`;
}

function parseUnitOption(
  value: string | null,
  unitOptions: UnitOption[],
  fallbackUnit = unitOptions[0]?.value ?? 0.1
) {
  const parsedValue = Number(value);
  return unitOptions.some((option) => option.value === parsedValue)
    ? parsedValue
    : fallbackUnit;
}

function resolveUnit(symbol: MarketSymbol, unit: number) {
  return SYMBOL_UNIT_OPTIONS[symbol].some((option) => option.value === unit)
    ? unit
    : DEFAULT_UNIT_BY_SYMBOL[symbol];
}

function normalizePrice(price: number, symbol: MarketSymbol, unit: number) {
  if (!Number.isFinite(price) || price <= 0) {
    return null;
  }

  return roundValidPrice(price, symbol, unit);
}

function roundValidPrice(price: number, symbol: MarketSymbol, unit: number) {
  return Number(price.toFixed(getPricePrecision(symbol, unit)));
}

function getPricePrecision(symbol: MarketSymbol, unit: number) {
  return Math.max(PRICE_PRECISION_BY_SYMBOL[symbol], getUnitPrecision(unit));
}

function getUnitPrecision(unit: number) {
  const [, decimals = ""] = String(unit).split(".");
  return decimals.length;
}

function formatPriceInput(price: number, unit: number, symbol: MarketSymbol) {
  return price.toFixed(getPricePrecision(symbol, unit));
}
