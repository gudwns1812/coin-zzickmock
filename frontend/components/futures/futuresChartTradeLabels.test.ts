import assert from "node:assert/strict";
import test from "node:test";

const tradeLabelModule: typeof import("./futuresChartTradeLabels") =
  await import(new URL("./futuresChartTradeLabels.ts", import.meta.url).href);

const {
  TRADE_LABEL_COLORS,
  getOrderPriceLineColor,
  getOrderPriceLineTitle,
  getPositionPriceLineTitle,
} = tradeLabelModule;

test("order labels color sell-side close/open direction red or green", () => {
  assert.equal(
    getOrderPriceLineColor({
      orderPurpose: "CLOSE_POSITION",
      positionSide: "LONG",
    }),
    TRADE_LABEL_COLORS.red
  );
  assert.equal(
    getOrderPriceLineColor({
      orderPurpose: "OPEN_POSITION",
      positionSide: "SHORT",
    }),
    TRADE_LABEL_COLORS.red
  );
  assert.equal(
    getOrderPriceLineColor({
      orderPurpose: "OPEN_POSITION",
      positionSide: "LONG",
    }),
    TRADE_LABEL_COLORS.green
  );
  assert.equal(
    getOrderPriceLineColor({
      orderPurpose: "CLOSE_POSITION",
      positionSide: "SHORT",
    }),
    TRADE_LABEL_COLORS.green
  );
});

test("position labels omit margin mode and order labels include open close intent", () => {
  assert.equal(
    getPositionPriceLineTitle(
      {
        entryPrice: 100,
        positionSide: "LONG",
      },
      "$100.00"
    ),
    "Long $100.00"
  );
  assert.equal(
    getOrderPriceLineTitle(
      {
        orderPurpose: "CLOSE_POSITION",
        positionSide: "LONG",
      },
      "$110.00"
    ),
    "Close Long $110.00"
  );
});

test("conditional TP/SL close orders use trigger labels", () => {
  assert.equal(
    getOrderPriceLineTitle(
      {
        orderPurpose: "CLOSE_POSITION",
        positionSide: "LONG",
        triggerType: "TAKE_PROFIT",
      },
      "$110.00"
    ),
    "TP Close Long $110.00"
  );
  assert.equal(
    getOrderPriceLineTitle(
      {
        orderPurpose: "CLOSE_POSITION",
        positionSide: "SHORT",
        triggerType: "STOP_LOSS",
      },
      "$105.00"
    ),
    "SL Close Short $105.00"
  );
});
