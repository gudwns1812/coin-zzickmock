"use client";

import clsx from "clsx";
import { ChevronDown, ChevronLeft, ChevronRight } from "lucide-react";
import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Scrab from "@/components/ui/shared/Scrab";
import { JwtToken } from "@/type/jwt";
import UpPrice from "@/components/ui/shared/UpPrice";
import DownPrice from "@/components/ui/shared/DownPrice";
import Image from "next/image";
import Button from "@/components/ui/shared/Button";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
  Accordion,
  AccordionItem,
  AccordionButton,
  AccordionPanel,
} from "@/components/animate-ui/headless/accordion";
import ErrorComponent from "@/components/ui/shared/ErrorComponent";

interface StockData {
  stockName: string;
  stockCode: string;
  sign: string;
  currentPrice: string;
  changeRate: string;
  changeAmount: string;
  stockImage: string;
}

const CATEGORY_GROUPS = {
  제조업: [
    "건설",
    "금속",
    "제조",
    "기계·장비",
    "의료·정밀기기",
    "화학",
    "비금속",
    "기타제조",
    "종이·목재",
    "전기·전자",
    "출판·매체복제",
  ],
  서비스업: [
    "일반서비스",
    "IT 서비스",
    "오락·문화",
    "리츠",
    "부동산",
    "외국증권",
  ],
  금융업: ["금융", "보험", "증권"],
  "유통 및 소비재": ["유통", "음식료·담배", "섬유·의류"],
  "에너지·인프라": [
    "전기·가스",
    "인프라투용",
    "운송·창고",
    "운송장비·부품",
    "통신",
  ],
  "바이오·제약": ["제약"],
  "ETF 상품": [
    "ETF",
    "ETF(실물복제&수익증권)",
    "ETF(합성복제&수익증권)",
    "ETF(Active&수익증권)",
  ],
  기타: ["기타"],
};

// 카테고리 스켈레톤 컴포넌트
const CategorySkeleton = () => {
  return (
    <div className="p-main flex flex-col gap-main">
      <div className="h-7 w-20 bg-gray-200 rounded animate-pulse"></div>
      <div className="grid grid-cols-3 gap-main">
        <div className="col-span-1">
          <div className="space-y-4">
            {Array.from({ length: 8 }).map((_, index) => (
              <div key={`category-skeleton-${index}`} className="space-y-2">
                <div className="h-8 w-24 bg-gray-200 rounded animate-pulse"></div>
                <div className="pl-4 space-y-2">
                  <div className="h-6 w-16 bg-gray-200 rounded animate-pulse"></div>
                  <div className="h-6 w-20 bg-gray-200 rounded animate-pulse"></div>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="col-span-2 pl-main">
          <div className="h-8 w-32 bg-gray-200 rounded animate-pulse mb-4"></div>
          <div className="grid grid-cols-2 grid-rows-3 gap-y-main">
            {Array.from({ length: 6 }).map((_, index) => (
              <div
                key={`stock-skeleton-${index}`}
                className="border border-gray-100 rounded-main px-main-2 py-main relative"
              >
                <div className="flex gap-main w-full">
                  <div className="size-[40px] bg-gray-200 rounded-full animate-pulse shrink-0"></div>
                  <div className="flex flex-col flex-1 truncate gap-1">
                    <div className="flex items-baseline gap-1">
                      <div className="h-4 w-20 bg-gray-200 rounded animate-pulse"></div>
                      <div className="h-3 w-12 bg-gray-200 rounded animate-pulse"></div>
                    </div>
                    <div className="flex gap-main items-center">
                      <div className="h-4 w-16 bg-gray-200 rounded animate-pulse"></div>
                      <div className="h-3 w-12 bg-gray-200 rounded animate-pulse"></div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

// 종목 스켈레톤 컴포넌트
const StocksSkeleton = () => {
  return (
    <>
      <div className="h-8 w-32 bg-gray-200 rounded animate-pulse mb-4"></div>
      <div className="grid grid-cols-2 grid-rows-3 gap-y-main">
        {Array.from({ length: 6 }).map((_, index) => (
          <div
            key={`stocks-skeleton-${index}`}
            className="border border-gray-100 rounded-main px-main-2 py-main relative"
          >
            <div className="flex gap-main w-full">
              <div className="size-[40px] bg-gray-200 rounded-full animate-pulse shrink-0"></div>
              <div className="flex flex-col flex-1 truncate gap-1">
                <div className="flex items-baseline gap-1">
                  <div className="h-4 w-20 bg-gray-200 rounded animate-pulse"></div>
                  <div className="h-3 w-12 bg-gray-200 rounded animate-pulse"></div>
                </div>
                <div className="flex gap-main items-center">
                  <div className="h-4 w-16 bg-gray-200 rounded animate-pulse"></div>
                  <div className="h-3 w-12 bg-gray-200 rounded animate-pulse"></div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </>
  );
};

const CategoryStock = ({ token }: { token: JwtToken | null }) => {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const router = useRouter();

  const {
    data: categoryData = [],
    isLoading: isCategoryLoading,
    error: categoryError,
  } = useQuery({
    queryKey: ["categories"],
    queryFn: async () => {
      const res = await fetch(`/proxy/v1/stocks/categories?page=1`);
      if (!res.ok)
        throw new Error("카테고리 데이터를 불러오는데 실패했습니다.");
      const json = await res.json();
      return (
        json.data?.map((item: { categoryName: string }) => item.categoryName) ||
        []
      );
    },
    staleTime: 10 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    retry: 1,
  });

  const {
    data: categoryStocks = { totalPages: 0, stocks: [] },
    isLoading: isStocksLoading,
    error: stocksError,
  } = useQuery({
    queryKey: ["categoryStocks", selectedCategory, page],
    queryFn: async () => {
      if (!selectedCategory) return { totalPages: 0, stocks: [] };
      const response = await fetch(
        `/proxy2/v2/stocks/category/${encodeURIComponent(
          selectedCategory
        )}?page=${page}`
      );
      if (!response.ok)
        throw new Error("주식 데이터를 불러오는데 실패했습니다.");
      const data = await response.json();
      return data.data;
    },
    enabled: !!selectedCategory,
    placeholderData: keepPreviousData,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    retry: 1,
  });

  const totalPage = categoryStocks?.totalPages || 1;

  useEffect(() => {
    if (categoryData.length > 0 && !selectedCategory) {
      setSelectedCategory(categoryData[0]);
    }
  }, [categoryData, selectedCategory]);

  const handlePageChange = (newPage: number) => {
    if (newPage < 1 || newPage > totalPage) return;
    setPage(newPage);
  };

  const handleCategoryClick = (category: string) => {
    setSelectedCategory(category);
    setPage(1);
  };

  function getPagination(current: number, total: number) {
    const delta = 1;
    const range = [];
    const rangeWithDots = [];
    let l: number | undefined = undefined;

    for (let i = 1; i <= total; i++) {
      if (
        i === 1 ||
        i === total ||
        (i >= current - delta && i <= current + delta)
      ) {
        range.push(i);
      }
    }

    for (let i of range) {
      if (l !== undefined) {
        if (i - l !== 1) {
          rangeWithDots.push("...");
        }
      }
      rangeWithDots.push(i);
      l = i;
    }

    return rangeWithDots;
  }

  function getGroupedCategories(categoryData: string[]) {
    const grouped: Record<string, string[]> = {};
    const used = new Set<string>();

    Object.entries(CATEGORY_GROUPS).forEach(([group, cats]) => {
      grouped[group] = cats.filter((cat) => categoryData.includes(cat));
      cats.forEach((cat) => used.add(cat));
    });

    const etc = categoryData.filter((cat) => !used.has(cat));
    if (etc.length > 0) {
      grouped["기타"] = etc;
    }

    return grouped;
  }

  function getCategoryGroup(category: string | null) {
    if (!category) return "";
    return (
      Object.entries(CATEGORY_GROUPS).find(([group, cats]) =>
        cats.includes(category)
      )?.[0] || ""
    );
  }

  const handleClickSearchResult = (code: string) => {
    router.push(`/stock/${code}`);
  };

  // 카테고리 데이터가 로딩 중일 때 전체 스켈레톤 표시
  if (isCategoryLoading) {
    return <CategorySkeleton />;
  }

  // 카테고리 데이터 로딩 에러
  if (categoryError) {
    return (
      <div className="p-main flex flex-col gap-main">
        <h2 className="text-xl font-bold">카테고리</h2>
        <ErrorComponent message="카테고리 데이터를 불러올 수 없습니다." />
      </div>
    );
  }

  return (
    <div className="p-main flex flex-col gap-main">
      <h2 className="text-xl font-bold">카테고리</h2>
      <div className="grid grid-cols-3 gap-main">
        <div className="col-span-1">
          <Accordion>
            {Object.entries(getGroupedCategories(categoryData)).map(
              ([group, cats]) => (
                <AccordionItem key={group}>
                  <AccordionButton className="w-full flex justify-between items-center font-semibold py-main px-main rounded-main">
                    <span className="text-main-dark-gray hover:text-main-blue transition-colors duration-300">
                      {group}
                    </span>
                  </AccordionButton>
                  <AccordionPanel className="px-main pb-main">
                    <div className="flex flex-wrap gap-main">
                      {cats.map((category) => (
                        <Button
                          key={category}
                          variant={
                            selectedCategory === category ? "primary" : "ghost"
                          }
                          onClick={() => handleCategoryClick(category)}
                          className="py-2"
                        >
                          {category}
                        </Button>
                      ))}
                    </div>
                  </AccordionPanel>
                </AccordionItem>
              )
            )}
          </Accordion>
        </div>
        <div className="col-span-2 pl-main rounded-main size-full relative flex flex-col gap-main">
          <div className="text-main-dark-gray flex items-center gap-1 px-main py-2 border-b border-main-light-gray">
            <span>{getCategoryGroup(selectedCategory)}</span>
            <ChevronRight size={14} />
            <span className="text-main-blue font-semibold">
              {selectedCategory}
            </span>
          </div>

          {/* 종목 데이터가 로딩 중일 때 종목 스켈레톤 표시 */}
          {isStocksLoading ? (
            <StocksSkeleton />
          ) : stocksError ? (
            <ErrorComponent message="종목 데이터를 불러올 수 없습니다." />
          ) : (
            <>
              <div className="grid grid-cols-2 grid-rows-3 gap-y-main">
                {categoryStocks &&
                  categoryStocks.stocks.map((stock: StockData) => (
                    <div
                      className="w-full flex flex-col justify-around border border-transparent hover:border-main-blue/20 hover:scale-102 cursor-pointer rounded-main transition-all duration-200 ease-in-out px-main-2 py-main gap-[5px] relative group"
                      key={selectedCategory + stock.stockCode}
                      onClick={() => handleClickSearchResult(stock.stockCode)}
                    >
                      <Scrab
                        stockCode={stock.stockCode}
                        stockName={stock.stockName}
                        stockInfo={{
                          stockImage: stock.stockImage,
                          currentPrice: stock.currentPrice,
                          changeAmount: stock.changeAmount,
                          changeRate: stock.changeRate,
                          sign: stock.sign,
                        }}
                        token={token}
                      />

                      <div className="flex gap-main w-full">
                        <div className="relative flex items-center justify-center size-[40px] shrink-0">
                          {stock.stockImage ? (
                            <Image
                              src={stock.stockImage}
                              alt={stock.stockName}
                              fill
                              className="rounded-full"
                              sizes="40px"
                            />
                          ) : (
                            <div className="bg-main-blue/10 rounded-full size-[40px] shrink-0 flex items-center justify-center">
                              <span className="text-main-blue font-semibold">
                                {stock.stockName[0]}
                              </span>
                            </div>
                          )}
                        </div>
                        <div className="flex flex-col flex-1 truncate">
                          <div className="text-gray-800 truncate w-full flex items-baseline gap-1">
                            <span className="font-bold">{stock.stockName}</span>
                            <span className="text-gray-500 text-xs-custom">
                              {stock.stockCode}
                            </span>
                          </div>
                          <div className="text-sm-custom flex gap-main items-center">
                            <span
                              className={clsx(
                                "text-gray-500 text-sm font-semibold",
                                (stock.sign === "1" || stock.sign === "2") &&
                                  "text-main-red",
                                (stock.sign === "4" || stock.sign === "5") &&
                                  "text-main-blue",
                                stock.sign === "3" && "text-gray-500"
                              )}
                            >
                              {Number(stock.currentPrice).toLocaleString()}
                            </span>

                            <div className="flex justify-between h-fit">
                              {(stock.sign === "1" || stock.sign === "2") && (
                                <UpPrice
                                  change={Number(stock.changeAmount)}
                                  changeRate={Number(stock.changeRate)}
                                />
                              )}
                              {stock.sign === "3" && (
                                <span className="text-gray-400 font-medium">
                                  {Number(stock.changeAmount)} (
                                  {Number(stock.changeRate)}%)
                                </span>
                              )}
                              {(stock.sign === "4" || stock.sign === "5") && (
                                <DownPrice
                                  change={Number(stock.changeAmount)}
                                  changeRate={Number(stock.changeRate)}
                                />
                              )}
                            </div>
                          </div>
                        </div>
                        <ChevronRight
                          className="hidden group-hover:block text-main-blue absolute top-1/2 -translate-y-1/2 right-main"
                          size={20}
                        />
                      </div>
                    </div>
                  ))}
              </div>

              <div className="flex justify-center items-center gap-2 mt-4">
                <button
                  className="size-[30px] rounded-full flex items-center justify-center bg-gray-200 text-gray-700 disabled:opacity-50"
                  onClick={() => handlePageChange(page - 1)}
                  disabled={page === 1 || isStocksLoading}
                >
                  <ChevronLeft size={20} />
                </button>
                {getPagination(page, totalPage).map((item, idx) =>
                  item === "..." ? (
                    <span
                      key={`ellipsis-${idx}`}
                      className="px-2 text-gray-400"
                    >
                      ...
                    </span>
                  ) : (
                    <button
                      key={`page-${item}`}
                      className={`size-[30px] rounded-full flex items-center justify-center ${
                        page === item
                          ? "bg-main-blue text-white"
                          : "bg-gray-100 text-gray-700"
                      } ${
                        isStocksLoading ? "opacity-50 cursor-not-allowed" : ""
                      }`}
                      onClick={() => handlePageChange(Number(item))}
                      disabled={isStocksLoading}
                    >
                      {item}
                    </button>
                  )
                )}
                <button
                  className="size-[30px] rounded-full flex items-center justify-center bg-gray-200 text-gray-700 disabled:opacity-50"
                  onClick={() => handlePageChange(page + 1)}
                  disabled={page === totalPage || isStocksLoading}
                >
                  <ChevronRight size={20} />
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default CategoryStock;
