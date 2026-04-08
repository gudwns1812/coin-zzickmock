import { faker } from "@faker-js/faker";
import { http, HttpResponse } from "msw";
import { KOSDAQ } from "@/type/stocks/KOSDAQ";
import { KOSPI } from "@/type/stocks/KOSPI";
import { TestPopular } from "@/type/stocks/Popular";
import { StockSearchResult } from "@/type/stocks/StockSearchResult";
import { TestStockData } from "@/type/stocks/stockData";

faker.seed(20260406);

type InterestGroup = {
  groupId: string;
  groupName: string;
  groupSequence: number;
  main: boolean;
  memberId: string;
};

type InterestStock = {
  stockInfo: StockSearchResult;
  stockSequence: number;
};

type PortfolioStock = {
  stockName: string;
  stockImage: string;
  stockCode: string;
  stockCount: number;
  entryPrice: number;
  currentPrice: number;
  profitLoss: number;
  profitLossRate: number;
};

type PortfolioAssetPoint = {
  date: [number, number, number];
  asset: number;
  pnl: number;
};

const STOCK_LIBRARY: Array<
  StockSearchResult & {
    categoryName: string;
    marketName: string;
  }
> = [
  {
    stockCode: "005930",
    stockName: "삼성전자",
    currentPrice: "84200",
    sign: "2",
    changeAmount: "900",
    changeRate: "1.08",
    stockImage: "https://placehold.co/80x80/png?text=SE",
    categoryName: "전기·전자",
    marketName: "KOSPI",
  },
  {
    stockCode: "000660",
    stockName: "SK하이닉스",
    currentPrice: "216500",
    sign: "2",
    changeAmount: "4500",
    changeRate: "2.12",
    stockImage: "https://placehold.co/80x80/png?text=SK",
    categoryName: "전기·전자",
    marketName: "KOSPI",
  },
  {
    stockCode: "035420",
    stockName: "NAVER",
    currentPrice: "189800",
    sign: "4",
    changeAmount: "1200",
    changeRate: "-0.63",
    stockImage: "https://placehold.co/80x80/png?text=NV",
    categoryName: "IT 서비스",
    marketName: "KOSPI",
  },
  {
    stockCode: "051910",
    stockName: "LG화학",
    currentPrice: "327500",
    sign: "4",
    changeAmount: "3500",
    changeRate: "-1.06",
    stockImage: "https://placehold.co/80x80/png?text=LG",
    categoryName: "화학",
    marketName: "KOSPI",
  },
  {
    stockCode: "068270",
    stockName: "셀트리온",
    currentPrice: "184300",
    sign: "2",
    changeAmount: "2700",
    changeRate: "1.49",
    stockImage: "https://placehold.co/80x80/png?text=CT",
    categoryName: "제약",
    marketName: "KOSPI",
  },
  {
    stockCode: "005380",
    stockName: "현대차",
    currentPrice: "241500",
    sign: "2",
    changeAmount: "3000",
    changeRate: "1.26",
    stockImage: "https://placehold.co/80x80/png?text=HY",
    categoryName: "운송장비·부품",
    marketName: "KOSPI",
  },
  {
    stockCode: "105560",
    stockName: "KB금융",
    currentPrice: "81200",
    sign: "2",
    changeAmount: "500",
    changeRate: "0.62",
    stockImage: "https://placehold.co/80x80/png?text=KB",
    categoryName: "금융",
    marketName: "KOSPI",
  },
  {
    stockCode: "035720",
    stockName: "카카오",
    currentPrice: "42150",
    sign: "4",
    changeAmount: "550",
    changeRate: "-1.29",
    stockImage: "https://placehold.co/80x80/png?text=KK",
    categoryName: "IT 서비스",
    marketName: "KOSPI",
  },
  {
    stockCode: "028260",
    stockName: "삼성물산",
    currentPrice: "152900",
    sign: "3",
    changeAmount: "0",
    changeRate: "0.00",
    stockImage: "https://placehold.co/80x80/png?text=SM",
    categoryName: "건설",
    marketName: "KOSPI",
  },
  {
    stockCode: "207940",
    stockName: "삼성바이오로직스",
    currentPrice: "1007000",
    sign: "2",
    changeAmount: "17000",
    changeRate: "1.72",
    stockImage: "https://placehold.co/80x80/png?text=SB",
    categoryName: "제약",
    marketName: "KOSPI",
  },
  {
    stockCode: "373220",
    stockName: "LG에너지솔루션",
    currentPrice: "371000",
    sign: "4",
    changeAmount: "4000",
    changeRate: "-1.07",
    stockImage: "https://placehold.co/80x80/png?text=LE",
    categoryName: "전기·전자",
    marketName: "KOSPI",
  },
  {
    stockCode: "091990",
    stockName: "셀트리온헬스케어",
    currentPrice: "62100",
    sign: "2",
    changeAmount: "1300",
    changeRate: "2.14",
    stockImage: "https://placehold.co/80x80/png?text=CH",
    categoryName: "제약",
    marketName: "KOSDAQ",
  },
  {
    stockCode: "247540",
    stockName: "에코프로비엠",
    currentPrice: "188200",
    sign: "4",
    changeAmount: "1900",
    changeRate: "-1.00",
    stockImage: "https://placehold.co/80x80/png?text=EB",
    categoryName: "전기·전자",
    marketName: "KOSDAQ",
  },
  {
    stockCode: "086520",
    stockName: "에코프로",
    currentPrice: "93200",
    sign: "2",
    changeAmount: "2100",
    changeRate: "2.30",
    stockImage: "https://placehold.co/80x80/png?text=EP",
    categoryName: "전기·전자",
    marketName: "KOSDAQ",
  },
  {
    stockCode: "263750",
    stockName: "펄어비스",
    currentPrice: "40100",
    sign: "2",
    changeAmount: "650",
    changeRate: "1.65",
    stockImage: "https://placehold.co/80x80/png?text=PA",
    categoryName: "오락·문화",
    marketName: "KOSDAQ",
  },
  {
    stockCode: "196170",
    stockName: "알테오젠",
    currentPrice: "307500",
    sign: "2",
    changeAmount: "5200",
    changeRate: "1.72",
    stockImage: "https://placehold.co/80x80/png?text=AT",
    categoryName: "제약",
    marketName: "KOSDAQ",
  },
];

const STOCK_BY_CODE = new Map(
  STOCK_LIBRARY.map((stock) => [stock.stockCode, stock])
);

const CATEGORY_NAMES = Array.from(
  new Set(STOCK_LIBRARY.map((stock) => stock.categoryName))
);

const MEMBER_ID = "mock-member";

let interestGroups: InterestGroup[] = [
  {
    groupId: "group-main",
    groupName: "메인 관심",
    groupSequence: 1,
    main: true,
    memberId: MEMBER_ID,
  },
  {
    groupId: "group-growth",
    groupName: "성장주",
    groupSequence: 2,
    main: false,
    memberId: MEMBER_ID,
  },
];

let interestStocksByGroup: Record<string, InterestStock[]> = {
  "group-main": [
    { stockInfo: STOCK_LIBRARY[0], stockSequence: 1 },
    { stockInfo: STOCK_LIBRARY[1], stockSequence: 2 },
  ],
  "group-growth": [
    { stockInfo: STOCK_LIBRARY[11], stockSequence: 1 },
    { stockInfo: STOCK_LIBRARY[12], stockSequence: 2 },
  ],
};

let portfolioStocks: PortfolioStock[] = [
  {
    stockName: STOCK_LIBRARY[0].stockName,
    stockImage: STOCK_LIBRARY[0].stockImage,
    stockCode: STOCK_LIBRARY[0].stockCode,
    stockCount: 12,
    entryPrice: 78500,
    currentPrice: Number(STOCK_LIBRARY[0].currentPrice),
    profitLoss:
      (Number(STOCK_LIBRARY[0].currentPrice) - 78500) * 12,
    profitLossRate:
      ((Number(STOCK_LIBRARY[0].currentPrice) - 78500) / 78500) * 100,
  },
  {
    stockName: STOCK_LIBRARY[1].stockName,
    stockImage: STOCK_LIBRARY[1].stockImage,
    stockCode: STOCK_LIBRARY[1].stockCode,
    stockCount: 4,
    entryPrice: 198000,
    currentPrice: Number(STOCK_LIBRARY[1].currentPrice),
    profitLoss:
      (Number(STOCK_LIBRARY[1].currentPrice) - 198000) * 4,
    profitLossRate:
      ((Number(STOCK_LIBRARY[1].currentPrice) - 198000) / 198000) * 100,
  },
];

const getStockOrFallback = (stockCode: string) => {
  return (
    STOCK_BY_CODE.get(stockCode) ?? {
      stockCode,
      stockName: `샘플종목 ${stockCode}`,
      currentPrice: "50000",
      sign: "2",
      changeAmount: "400",
      changeRate: "0.80",
      stockImage: `https://placehold.co/80x80/png?text=${stockCode.slice(0, 2)}`,
      categoryName: "기타",
      marketName: "KOSPI",
    }
  );
};

const searchStocks = (keyword: string) => {
  const normalizedKeyword = keyword.trim().toLowerCase();

  if (!normalizedKeyword) {
    return STOCK_LIBRARY.slice(0, 6);
  }

  const matches = STOCK_LIBRARY.filter((stock) => {
    return (
      stock.stockName.toLowerCase().includes(normalizedKeyword) ||
      stock.stockCode.includes(normalizedKeyword) ||
      stock.categoryName.toLowerCase().includes(normalizedKeyword)
    );
  });

  return matches.length > 0
    ? matches
    : [getStockOrFallback(keyword.padStart(6, "0").slice(0, 6))];
};

const buildIndexData = (baseValue: number, sign: string): KOSPI => {
  const indices = Array.from({ length: 30 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - index);

    const dailyBase = baseValue + Math.round(Math.sin(index / 3) * 18) - index;
    const close = dailyBase.toFixed(2);

    return {
      bstp_nmix_hgpr: (dailyBase + 5).toFixed(2),
      bstp_nmix_lwpr: (dailyBase - 7).toFixed(2),
      bstp_nmix_prpr: close,
      stck_bsop_date: `${date.getFullYear()}${String(
        date.getMonth() + 1
      ).padStart(2, "0")}${String(date.getDate()).padStart(2, "0")}`,
    };
  });

  return {
    prev: indices[1]?.bstp_nmix_prpr ?? indices[0].bstp_nmix_prpr,
    sign,
    prev_rate: sign === "4" ? "-0.52" : "0.71",
    indices,
  };
};

const createChartData = (
  stockCode: string,
  period: string,
  startDate?: string,
  endDate?: string
) => {
  const start = startDate ? new Date(startDate) : new Date("2024-01-01");
  const end = endDate ? new Date(endDate) : new Date();
  const stepDays =
    period === "D" ? 1 : period === "W" ? 7 : period === "M" ? 30 : 365;
  const basePrice = Number(getStockOrFallback(stockCode).currentPrice);
  const points: TestStockData[] = [];

  for (
    let cursor = new Date(start), index = 0;
    cursor <= end;
    cursor.setDate(cursor.getDate() + stepDays), index += 1
  ) {
    const trend = Math.sin(index / 4) * 900;
    const open = Math.round(basePrice - 400 + trend);
    const close = Math.round(basePrice + trend);
    const high = Math.max(open, close) + 650;
    const low = Math.min(open, close) - 700;

    points.push({
      stockCode,
      date: [
        cursor.getFullYear(),
        cursor.getMonth() + 1,
        cursor.getDate(),
      ],
      type: null,
      open: String(open),
      high: String(high),
      low: String(low),
      close: String(close),
      volume: String(20000 + index * 350),
      volumeAmount: String((20000 + index * 350) * close),
      prevPrice: close - 250,
      openFromPrev: open - (close - 250),
      closeFromPrev: 250,
      highFromPrev: high - (close - 250),
      lowFromPrev: low - (close - 250),
    });
  }

  return points;
};

const createForexData = (symbol: string) => {
  const basePriceMap: Record<string, number> = {
    dollar: 1368.2,
    yen: 911.5,
    DOW: 39120.4,
    NQ: 18410.2,
    SP500: 5205.1,
    Nikkei: 39385.2,
    HangSeng: 17088.6,
    ShangHai: 3051.3,
    GOLD: 2294.1,
    SILVER: 27.7,
    WTI: 81.4,
    CORN: 442.8,
    COFFEE: 221.4,
    COTTON: 84.2,
    KRBONDS1: 3.12,
    KRBONDS3: 3.05,
    KRBONDS5: 3.11,
    KRBONDS10: 3.22,
    KRBONDS20: 3.28,
    KRBONDS30: 3.31,
    USBONDS1: 4.93,
    USBONDS10: 4.31,
  };

  const basePrice = basePriceMap[symbol] ?? 100;
  const pastInfo = Array.from({ length: 30 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - index);
    const current = basePrice - index * 0.2 + Math.sin(index / 2) * 1.5;

    return {
      stck_bsop_date: `${date.getFullYear()}${String(
        date.getMonth() + 1
      ).padStart(2, "0")}${String(date.getDate()).padStart(2, "0")}`,
      ovrs_nmix_prpr: current.toFixed(4),
      ovrs_nmix_oprc: (current - 0.4).toFixed(4),
      ovrs_nmix_hgpr: (current + 0.9).toFixed(4),
      ovrs_nmix_lwpr: (current - 1.1).toFixed(4),
    };
  });

  return {
    changePrice: "1.2000",
    changeSign: "2",
    changeRate: "0.92",
    prevPrice: (basePrice - 1.2).toFixed(4),
    highPrice: (basePrice + 1.5).toFixed(4),
    lowPrice: (basePrice - 2.1).toFixed(4),
    openPrice: (basePrice - 0.8).toFixed(4),
    currentPrice: basePrice.toFixed(4),
    pastInfo,
  };
};

const recalculatePortfolioStock = (
  stockCode: string,
  stockCount: number,
  entryPrice: number
): PortfolioStock => {
  const stock = getStockOrFallback(stockCode);
  const currentPrice = Number(stock.currentPrice);
  const profitLoss = (currentPrice - entryPrice) * stockCount;

  return {
    stockName: stock.stockName,
    stockImage: stock.stockImage,
    stockCode,
    stockCount,
    entryPrice,
    currentPrice,
    profitLoss,
    profitLossRate: entryPrice === 0 ? 0 : (profitLoss / (entryPrice * stockCount)) * 100,
  };
};

const createPortfolioHistory = (period: string) => {
  const daysByPeriod: Record<string, number> = {
    W: 7,
    M: 30,
    "3M": 90,
    Y: 365,
  };
  const length = daysByPeriod[period] ?? 30;
  const baseAsset = portfolioStocks.reduce(
    (sum, stock) => sum + stock.currentPrice * stock.stockCount,
    0
  );

  const pnlHistory: PortfolioAssetPoint[] = Array.from(
    { length: Math.min(length, 60) },
    (_, index) => {
      const date = new Date();
      date.setDate(date.getDate() - (Math.min(length, 60) - index));
      const pnl = Math.round(Math.sin(index / 4) * 120000);

      return {
        date: [date.getFullYear(), date.getMonth() + 1, date.getDate()],
        asset: baseAsset - 300000 + index * 20000,
        pnl,
      };
    }
  );

  return {
    periodAsset: baseAsset,
    pnlHistory,
  };
};

const createPortfolioCalendarHistory = () => {
  const baseAsset = portfolioStocks.reduce(
    (sum, stock) => sum + stock.currentPrice * stock.stockCount,
    0
  );

  return Array.from({ length: 60 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - index);
    const pnl = Math.round(Math.sin(index / 5) * 90000);

    return {
      createdDate: null,
      lastModifiedDate: date.toISOString(),
      id: index + 1,
      memberId: MEMBER_ID,
      date: date.toISOString(),
      asset: baseAsset - index * 8000,
      pnl,
    };
  });
};

const getMainGroupId = () => {
  return interestGroups.find((group) => group.main)?.groupId ?? interestGroups[0]?.groupId;
};

export const handlers = [
  http.get("*/proxy2/v2/stocks/search", ({ request }) => {
    const keyword = new URL(request.url).searchParams.get("keyword") ?? "";
    return HttpResponse.json({ data: searchStocks(keyword) });
  }),

  http.post("*/proxy2/v2/stocks/search", async () => {
    return HttpResponse.json({ data: true });
  }),

  http.post("*/proxy2/v2/stocks/active-sets", async () => {
    return HttpResponse.json({ data: true });
  }),

  http.get("*/proxy/v1/stocks/search", ({ request }) => {
    const keyword = new URL(request.url).searchParams.get("keyword") ?? "";
    return HttpResponse.json({ data: searchStocks(keyword) });
  }),

  http.post("*/proxy/v1/stocks/search", async () => {
    return HttpResponse.json({ data: true });
  }),

  http.get("*/proxy2/v2/stocks/indices/KOSPI", () => {
    return HttpResponse.json({ data: buildIndexData(2741.38, "2") });
  }),

  http.get("*/proxy2/v2/stocks/indices/KOSDAQ", () => {
    const data: KOSDAQ = buildIndexData(892.14, "4");
    return HttpResponse.json({ data });
  }),

  http.get("*/proxy2/v2/stocks/popular", () => {
    const data: TestPopular[] = STOCK_LIBRARY.slice(0, 6).map((stock, index) => ({
      stockName: stock.stockName,
      stockCode: stock.stockCode,
      rank: String(index + 1),
      price: stock.currentPrice,
      sign: stock.sign,
      changeAmount: stock.changeAmount,
      changeRate: stock.changeRate,
      stockImage: stock.stockImage,
    }));

    return HttpResponse.json({ data });
  }),

  http.get("*/proxy/v1/stocks/categories", () => {
    return HttpResponse.json({
      data: CATEGORY_NAMES.map((categoryName) => ({ categoryName })),
    });
  }),

  http.get("*/proxy2/v2/stocks/category", () => {
    return HttpResponse.json({
      data: CATEGORY_NAMES.map((categoryName) => ({ categoryName })),
    });
  }),

  http.get("*/proxy2/v2/stocks/category/:categoryName", ({ params, request }) => {
    const categoryName = decodeURIComponent((params.categoryName as string) ?? "");
    const page = Number(new URL(request.url).searchParams.get("page") ?? "1");
    const filteredStocks = STOCK_LIBRARY.filter(
      (stock) => stock.categoryName === categoryName
    );
    const pageSize = 6;
    const offset = (page - 1) * pageSize;

    return HttpResponse.json({
      data: {
        totalPages: Math.max(1, Math.ceil(filteredStocks.length / pageSize)),
        stocks: filteredStocks.slice(offset, offset + pageSize),
      },
    });
  }),

  http.get("*/proxy2/v2/stocks/FX", ({ request }) => {
    const symbol = new URL(request.url).searchParams.get("symbol") ?? "dollar";
    return HttpResponse.json({ data: createForexData(symbol) });
  }),

  http.get("*/proxy2/v2/stocks/info/:stockCode", ({ params }) => {
    const stockCode = params.stockCode as string;
    const stock = getStockOrFallback(stockCode);

    return HttpResponse.json({
      data: {
        stockCode: stock.stockCode,
        stockName: stock.stockName,
        categoryName: stock.categoryName,
        price: stock.currentPrice,
        openPrice: String(Number(stock.currentPrice) - 800),
        highPrice: String(Number(stock.currentPrice) + 1200),
        lowPrice: String(Number(stock.currentPrice) - 1500),
        marketName: stock.marketName,
        changeAmount: stock.changeAmount,
        sign: stock.sign,
        changeRate: stock.changeRate,
        volume: "521300",
        volumeValue: "34820000000",
        stockImage: stock.stockImage,
      },
    });
  }),

  http.get("*/proxy2/v2/stocks/:stockCode", ({ params, request }) => {
    const stockCode = params.stockCode as string;

    if (["search", "category", "indices", "info", "popular", "FX"].includes(stockCode)) {
      return HttpResponse.json({ data: [] }, { status: 404 });
    }

    const url = new URL(request.url);
    const period = url.searchParams.get("period") ?? "D";
    const startDate = url.searchParams.get("startDate") ?? undefined;
    const endDate = url.searchParams.get("endDate") ?? undefined;

    return HttpResponse.json({
      data: createChartData(stockCode, period, startDate, endDate),
    });
  }),

  http.get("*/proxy/v1/stocks/:stockCode", ({ params }) => {
    const stock = getStockOrFallback(params.stockCode as string);
    return HttpResponse.json({ data: stock.currentPrice });
  }),

  http.get("*/proxy/v1/portfolios/:memberId", () => {
    return HttpResponse.json({
      data: {
        portfolioStocks,
        totalAsset: portfolioStocks.reduce(
          (sum, stock) => sum + stock.currentPrice * stock.stockCount,
          0
        ),
      },
    });
  }),

  http.post("*/proxy/v1/portfolios/:memberId", async ({ request }) => {
    const body = (await request.json()) as {
      stock_code: string;
      stock_count: number;
      entry_price: number;
    };

    const updated = recalculatePortfolioStock(
      body.stock_code,
      body.stock_count,
      body.entry_price
    );
    portfolioStocks = [
      ...portfolioStocks.filter((stock) => stock.stockCode !== body.stock_code),
      updated,
    ];

    return HttpResponse.json({ data: updated }, { status: 201 });
  }),

  http.post("*/proxy/v1/portfolios/:memberId/:stockCode", async ({ params, request }) => {
    const stockCode = params.stockCode as string;
    const body = (await request.json()) as {
      stockCount: number;
      price: number;
      add: boolean;
    };
    const current =
      portfolioStocks.find((stock) => stock.stockCode === stockCode) ??
      recalculatePortfolioStock(stockCode, 0, body.price);

    const nextCount = body.add
      ? current.stockCount + body.stockCount
      : Math.max(0, current.stockCount - body.stockCount);

    if (nextCount === 0) {
      portfolioStocks = portfolioStocks.filter((stock) => stock.stockCode !== stockCode);
      return HttpResponse.json({ data: current });
    }

    const nextEntryPrice = body.add
      ? Math.round(
          (current.entryPrice * current.stockCount + body.price * body.stockCount) /
            nextCount
        )
      : current.entryPrice;

    const updated = recalculatePortfolioStock(stockCode, nextCount, nextEntryPrice);
    portfolioStocks = [
      ...portfolioStocks.filter((stock) => stock.stockCode !== stockCode),
      updated,
    ];

    return HttpResponse.json({ data: updated });
  }),

  http.get("*/proxy/v1/portfolios/asset/:memberId", ({ request }) => {
    const period = new URL(request.url).searchParams.get("period") ?? "M";

    if (period === "Y") {
      return HttpResponse.json({
        data: {
          ...createPortfolioHistory(period),
          pnlHistory: createPortfolioCalendarHistory(),
        },
      });
    }

    return HttpResponse.json({
      data: createPortfolioHistory(period),
    });
  }),

  http.get("*/proxy/v1/portfolios/asset/pnl/:memberId", ({ request }) => {
    const period = new URL(request.url).searchParams.get("period") ?? "Today";
    const pnlByPeriod: Record<string, number> = {
      Today: 128000,
      M: 642000,
      Total: 1835000,
    };

    return HttpResponse.json({
      data: {
        pnl: pnlByPeriod[period] ?? 0,
      },
    });
  }),

  http.get("*/proxy/favorite/:memberId", () => {
    return HttpResponse.json({ data: interestGroups });
  }),

  http.post("*/proxy/favorite/:memberId", async ({ params, request }) => {
    const body = (await request.json()) as { groupName?: string };
    const newGroup: InterestGroup = {
      groupId: faker.string.uuid(),
      groupName: body.groupName?.trim() || `새 그룹 ${interestGroups.length + 1}`,
      groupSequence: interestGroups.length + 1,
      main: interestGroups.length === 0,
      memberId: String(params.memberId ?? MEMBER_ID),
    };

    interestGroups = [...interestGroups, newGroup];
    interestStocksByGroup[newGroup.groupId] = [];

    return HttpResponse.json({ data: newGroup }, { status: 201 });
  }),

  http.get("*/proxy/favorite/:memberId/:groupId", ({ params }) => {
    const groupId = params.groupId as string;
    return HttpResponse.json(interestStocksByGroup[groupId] ?? []);
  }),

  http.put("*/proxy/favorite/:memberId/:groupId", async ({ params, request }) => {
    const groupId = params.groupId as string;
    const body = (await request.json()) as { groupName: string };

    interestGroups = interestGroups.map((group) =>
      group.groupId === groupId
        ? { ...group, groupName: body.groupName }
        : group
    );

    return HttpResponse.json({ data: true });
  }),

  http.delete("*/proxy/favorite/:memberId/:groupId", ({ params }) => {
    const groupId = params.groupId as string;
    interestGroups = interestGroups.filter((group) => group.groupId !== groupId);
    delete interestStocksByGroup[groupId];

    if (!interestGroups.some((group) => group.main) && interestGroups.length > 0) {
      interestGroups = interestGroups.map((group, index) => ({
        ...group,
        main: index === 0,
      }));
    }

    return HttpResponse.json({ data: true });
  }),

  http.put("*/proxy/favorite/:memberId/:groupId/main", ({ params }) => {
    const groupId = params.groupId as string;
    interestGroups = interestGroups.map((group) => ({
      ...group,
      main: group.groupId === groupId,
    }));

    return HttpResponse.json({ data: true });
  }),

  http.post("*/proxy/favorite/:memberId/:groupId", ({ params, request }) => {
    const url = new URL(request.url);
    const stockCode = url.searchParams.get("stockCode");
    const groupId = params.groupId as string;

    if (!stockCode) {
      return HttpResponse.json({ message: "stockCode is required" }, { status: 400 });
    }

    const stock = getStockOrFallback(stockCode);
    const nextStocks = interestStocksByGroup[groupId] ?? [];

    if (!nextStocks.some((item) => item.stockInfo.stockCode === stockCode)) {
      interestStocksByGroup[groupId] = [
        ...nextStocks,
        {
          stockInfo: stock,
          stockSequence: nextStocks.length + 1,
        },
      ];
    }

    return HttpResponse.json({ data: true }, { status: 201 });
  }),

  http.delete("*/proxy/favorite/:memberId/:groupId/stock", ({ params, request }) => {
    const groupId = params.groupId as string;
    const stockCode = new URL(request.url).searchParams.get("stockCode");

    interestStocksByGroup[groupId] = (interestStocksByGroup[groupId] ?? []).filter(
      (item) => item.stockInfo.stockCode !== stockCode
    );

    return HttpResponse.json({ data: true });
  }),

  http.post("*/proxy/auth/login", () => {
    return HttpResponse.json({ data: true });
  }),

  http.post("*/proxy/auth/register", () => {
    return HttpResponse.json({ data: true }, { status: 201 });
  }),

  http.post("*/proxy/auth/duplicate", async ({ request }) => {
    const body = (await request.json()) as { account?: string };
    const isAvailable = body.account !== "admin";
    return HttpResponse.json(
      { data: isAvailable },
      { status: isAvailable ? 200 : 409 }
    );
  }),

  http.get("*/proxy/auth/refresh", () => {
    return HttpResponse.json({ data: true });
  }),

  http.post("*/proxy/auth/invest", async () => {
    return HttpResponse.json({ data: true });
  }),

  http.delete("*/proxy/auth/withdraw", () => {
    portfolioStocks = [];
    const mainGroupId = getMainGroupId();
    interestStocksByGroup = mainGroupId ? { [mainGroupId]: [] } : {};

    return HttpResponse.json({ data: true });
  }),
];
