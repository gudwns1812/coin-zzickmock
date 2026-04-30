"use client";

import React, { useEffect, useState } from "react";
import { holdings as dummyHoldings } from "./dummyHoldings";
import { Plus } from "lucide-react";
import Modal from "@/components/ui/Modal";
import Input from "@/components/ui/shared/Input";
import { PortfolioData } from "@/type/portfolio";
import SearchStock from "@/components/ui/SearchStock";
import { JwtToken } from "@/type/jwt";
import { toast } from "react-toastify";
import clsx from "clsx";
import Image from "next/image";
import { Portfolio, usePortfolioStore } from "@/store/usePortfolio";
import Button from "@/components/ui/shared/Button";

const SettlementModal = Modal;
const AddHoldingModal = Modal;

interface SearchResult {
  changeAmount: string;
  changeRate: string;
  currentPrice: string;
  sign: string;
  stockCode: string;
  stockName: string;
  stockImage: string;
  stockCount: number;
  entryPrice: number;
}

const Holidings = ({ token }: { token: JwtToken | null }) => {
  const { portfolio, setPortfolio } = usePortfolioStore();
  // const [holdings, setHoldings] = useState<Holding[]>([]);
  const [isOpenAddHoldingModal, setIsOpenAddHoldingModal] = useState(false);
  const [selectedHoldings, setSelectedHoldings] = useState<
    | (Portfolio & {
        changeType: "buy" | "sell";
        changePrice: number;
        changeCount: number;
      })
    | null
  >(null);
  const [searchStockResult, setSearchStockResult] =
    useState<SearchResult | null>(null);
  const [isOpenSettlementModal, setIsOpenSettlementModal] = useState(false);

  const openSettlementModal = (h: Portfolio, changeType: "buy" | "sell") => {
    setSelectedHoldings({
      ...h,
      changeType,
      changePrice: h.entryPrice,
      changeCount: 1,
    });
    setIsOpenSettlementModal(true);
  };

  const handleHoldingSettlement = async () => {
    if (!token) return;
    if (!selectedHoldings) return;

    if (selectedHoldings.changeCount < 1) {
      toast.error("수량을 입력해주세요.");
      return;
    }

    if (selectedHoldings.changePrice < 1) {
      toast.error("가격을 입력해주세요.");
      return;
    }

    if (selectedHoldings.changeType === "sell") {
      if (selectedHoldings.stockCount < selectedHoldings.changeCount) {
        toast.error("보유 수량만큼 청산할 수 있습니다.");
        return;
      }
    }

    const requestData = {
      stockCount: selectedHoldings.changeCount,
      price: selectedHoldings.changePrice,
      add: selectedHoldings.changeType === "buy",
    };

    const res = await fetch(
      `/proxy/v1/portfolios/${token.account}/${selectedHoldings.stockCode}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(requestData),
      }
    );

    if (!res.ok) {
      console.error("Failed to settle holding", res);
      selectedHoldings.changeType === "buy"
        ? toast.error(`${selectedHoldings.stockName} 매수에 실패했습니다.`)
        : toast.error(`${selectedHoldings.stockName} 청산에 실패했습니다.`);

      setSelectedHoldings(null);
      setIsOpenSettlementModal(false);
      return;
    }

    const json: { data: Portfolio } = await res.json();

    const currentPortfolioIndex = portfolio.findIndex(
      (p) => p.stockCode === selectedHoldings.stockCode
    );
    const filteredPortfolio = portfolio.filter(
      (p) => p.stockCode !== selectedHoldings.stockCode
    );

    if (
      selectedHoldings.changeType === "sell" &&
      selectedHoldings.changeCount === selectedHoldings.stockCount
    ) {
      // 완전 청산 - 포트폴리오에서 제거
      setPortfolio(filteredPortfolio);
    } else {
      // 부분 청산 또는 매수 - 업데이트된 데이터로 교체
      setPortfolio([
        ...filteredPortfolio.slice(0, currentPortfolioIndex),
        json.data,
        ...filteredPortfolio.slice(currentPortfolioIndex),
      ]);
    }

    toast.success(
      selectedHoldings.changeType === "buy"
        ? `${selectedHoldings.stockName} 매수에 성공했습니다.`
        : `${selectedHoldings.stockName} 청산에 성공했습니다.`
    );

    setSelectedHoldings(null);
    setIsOpenSettlementModal(false);
  };

  const handleAddHolding = async () => {
    if (!token) return;
    if (!searchStockResult) return;

    const res = await fetch(`/proxy/v1/portfolios/${token.account}`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        stock_code: searchStockResult.stockCode,
        stock_count: searchStockResult.stockCount,
        entry_price: searchStockResult.entryPrice,
      }),
    });

    if (!res.ok) {
      console.error("Failed to add holding", res);
      toast.error("보유 종목 추가에 실패했습니다.");
      return;
    }

    const json: { data: Portfolio } = await res.json();

    const filteredPortfolio = portfolio.filter(
      (p) => p.stockCode !== searchStockResult.stockCode
    );

    setPortfolio([...filteredPortfolio, json.data]);

    toast.success("보유 종목 추가에 성공했습니다.");
    setSearchStockResult(null);
    setIsOpenAddHoldingModal(false);
  };

  if (!portfolio || portfolio.length === 0) {
    return (
      <>
        <div className="size-full flex flex-col gap-main">
          <div className="w-full flex justify-between items-center">
            <h2 className="text-2xl-custom font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent">
              보유 종목
            </h2>
            <Button
              id="add-holding"
              variant="ghost"
              className="!rounded-full flex items-center gap-main-1/2"
              onClick={() => {
                setIsOpenAddHoldingModal(true);
              }}
            >
              <Plus size={16} /> <span>보유 종목 추가</span>
            </Button>
          </div>
          <div className="relative size-full">
            <div className="absolute inset-0 bg-white/50 z-20 size-full flex items-center justify-center">
              <span className="text-main-dark-gray font-semibold">
                보유 종목이 존재하지 않습니다.
              </span>
            </div>

            <div className="grid grid-cols-1 overflow-y-auto flex-1 p-main gap-main blur-xs">
              {dummyHoldings.slice(0, 3).map((h, index) => (
                <div
                  key={`dummy-${index}`}
                  className="rounded-main p-4 bg-white flex flex-col gap-main hover:scale-102 hover:border-main-blue/20 border border-transparent duration-200 ease-in-out"
                >
                  <div className="flex justify-between items-center">
                    <div className="flex gap-[5px] items-baseline">
                      <p className="text-lg-custom font-bold text-gray-800">
                        {h.name}
                      </p>
                      <p className="text-sm-custom text-gray-500">{h.code}</p>
                    </div>

                    <div className="flex gap-main">
                      <button
                        className="px-3 py-1 text-sm-custom rounded-full bg-main-blue/20 text-main-blue hover:bg-main-blue/30 font-semibold"
                        // onClick={() => openSettlementModal(h)}
                      >
                        매수
                      </button>
                      <button
                        className="px-3 py-1 text-sm-custom rounded-full bg-main-red/20 text-main-red hover:bg-main-red/30 font-semibold"
                        // onClick={() => openSettlementModal(h)}
                      >
                        청산
                      </button>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-x-main-2 text-sm-custom text-gray-700">
                    <div className="flex justify-between gap-main">
                      <span className="text-main-dark-gray">투자금</span>
                      <span className="font-medium">
                        {h.capital.toLocaleString()}원
                      </span>
                    </div>
                    <div className="flex justify-between gap-main">
                      <span className="text-main-dark-gray">수량</span>
                      <span className="font-medium">{h.quantity}주</span>
                    </div>
                    <div className="flex justify-between gap-main">
                      <span className="text-main-dark-gray">수익</span>
                      <span className="font-medium text-main-red">
                        {h.profit.toLocaleString()}원
                      </span>
                    </div>
                    <div className="flex justify-between gap-main">
                      <span className="text-main-dark-gray">수익률</span>
                      <span className="font-medium text-main-red">
                        {h.profitRate.toLocaleString()}%
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* 보유 종목 추가 모달 */}
        <AddHoldingModal
          isOpen={isOpenAddHoldingModal}
          onClose={() => {
            setIsOpenAddHoldingModal(false);
            setSearchStockResult(null);
          }}
        >
          <div className="flex flex-col gap-main min-w-[600px]">
            <div className="flex items-center justify-between">
              <h2 className="text-xl-custom font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent">
                보유 종목 추가
              </h2>
            </div>

            {!searchStockResult && (
              <SearchStock
                onSelect={(stock) => {
                  setSearchStockResult({
                    ...stock,
                    entryPrice: Number(stock.currentPrice),
                    stockCount: 1,
                  });
                }}
              />
            )}

            {!searchStockResult && (
              <div className="flex flex-col items-center justify-center py-[60px] bg-gradient-to-br from-gray-50 to-gray-100/50 rounded-main border-2 border-dashed border-gray-200">
                <div className="w-16 h-16 bg-gradient-to-br from-main-blue/10 to-purple-100/20 rounded-full flex items-center justify-center mb-main">
                  <span className="text-2xl">📈</span>
                </div>
                <span className="text-main-dark-gray font-medium mb-1">
                  종목을 검색하여 추가해주세요
                </span>
                <span className="text-sm text-gray-500">
                  검색창에서 원하는 종목을 찾아보세요
                </span>
              </div>
            )}

            {searchStockResult && (
              <div className="bg-white rounded-main border border-gray-200 shadow-sm overflow-hidden">
                {/* 헤더 */}
                <div className="bg-gradient-to-r from-main-blue/5 to-purple-50/30 px-main-2 py-main border-b border-gray-100">
                  <div className="grid grid-cols-[2fr_1.5fr_1fr] gap-main-2">
                    <span className="text-sm font-semibold text-gray-700">
                      종목 정보
                    </span>
                    <span className="text-sm font-semibold text-gray-700 text-center">
                      구매가 (원)
                    </span>
                    <span className="text-sm font-semibold text-gray-700 text-center">
                      수량 (주)
                    </span>
                  </div>
                </div>

                {/* 내용 */}
                <div className="p-main-2">
                  <div className="grid grid-cols-[2fr_1.5fr_1fr] gap-main-2 items-center">
                    {/* 종목 정보 */}
                    <div className="flex items-center gap-main">
                      <div className="relative flex items-center justify-center size-[48px] shrink-0">
                        {searchStockResult.stockImage ? (
                          <Image
                            src={searchStockResult.stockImage}
                            alt={searchStockResult.stockName}
                            fill
                            className="rounded-full shadow-sm"
                          />
                        ) : (
                          <div className="bg-gradient-to-br from-main-blue/10 to-purple-100/20 rounded-full size-[48px] shrink-0 flex items-center justify-center shadow-sm">
                            <span className="text-main-blue font-bold text-lg">
                              {searchStockResult.stockName[0]}
                            </span>
                          </div>
                        )}
                      </div>
                      <div className="flex flex-col gap-1">
                        <span className="font-semibold text-main-dark-gray">
                          {searchStockResult.stockName}
                        </span>
                        <span className="text-xs text-gray-500 w-fit">
                          {searchStockResult.stockCode}
                        </span>
                      </div>
                    </div>

                    {/* 구매가 입력 */}
                    <div className="relative">
                      <Input
                        type="numeric"
                        value={searchStockResult.entryPrice}
                        min={1}
                        max={99999999999}
                        className="text-center pr-8 border-gray-200 focus:border-main-blue transition-colors"
                        onChange={(e) => {
                          const value = e.target.value;
                          if (/^\d*$/.test(value)) {
                            setSearchStockResult((prev) => {
                              if (!prev) return null;
                              return { ...prev, entryPrice: Number(value) };
                            });
                          }
                        }}
                      />
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">
                        원
                      </span>
                    </div>

                    {/* 수량 입력 */}
                    <div className="relative">
                      <Input
                        type="numeric"
                        value={searchStockResult.stockCount}
                        min={1}
                        max={1000000}
                        className="text-center pr-8 border-gray-200 focus:border-main-blue transition-colors"
                        onChange={(e) => {
                          const value = e.target.value;
                          if (/^\d*$/.test(value)) {
                            setSearchStockResult((prev) => {
                              if (!prev) return null;
                              return { ...prev, stockCount: Number(value) };
                            });
                          }
                        }}
                      />
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">
                        주
                      </span>
                    </div>
                  </div>

                  {/* 투자 금액 표시 */}
                  <div className="mt-main-2 pt-main-2 border-t border-gray-100">
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-gray-600">
                        총 투자 금액
                      </span>
                      <span className="text-lg font-bold text-main-blue">
                        {(
                          searchStockResult.entryPrice *
                          searchStockResult.stockCount
                        ).toLocaleString()}
                        원
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {searchStockResult && (
              <div className="flex gap-main justify-end pt-main">
                <Button
                  variant="ghost"
                  onClick={() => setSearchStockResult(null)}
                  className="hover:bg-gray-100"
                >
                  취소
                </Button>
                <Button
                  variant="primary"
                  onClick={handleAddHolding}
                  className="bg-gradient-to-r from-main-blue to-purple-600 hover:from-main-blue/90 hover:to-purple-600/90 shadow-md"
                >
                  포트폴리오에 추가
                </Button>
              </div>
            )}
          </div>
        </AddHoldingModal>
      </>
    );
  }

  return (
    <>
      <div className="size-full flex flex-col gap-main">
        <div className="w-full flex justify-between items-center">
          <h2 className="text-2xl-custom font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent">
            보유 종목
          </h2>
          <Button
            id="add-holding"
            variant="ghost"
            className="!rounded-full flex items-center gap-main-1/2"
            onClick={() => setIsOpenAddHoldingModal(true)}
          >
            <Plus size={16} /> <span>보유 종목 추가</span>
          </Button>
        </div>
        <div className="flex flex-col gap-main overflow-y-auto flex-1 p-main">
          {portfolio.map((stock, index) => (
            <div
              key={`my-portfolio-${stock.stockCode}`}
              className="rounded-main h-fit p-4 bg-white flex flex-col gap-main hover:scale-102 hover:border-main-blue/20 border border-transparent duration-200 ease-in-out"
            >
              <div className="flex justify-between items-center">
                <div className="flex gap-main items-center">
                  <div className="relative size-[40px] shrink-0">
                    <Image
                      src={stock.stockImage}
                      alt={stock.stockName}
                      fill
                      className="rounded-full shadow-sm"
                    />
                  </div>
                  <div>
                    <p className="text-lg-custom font-bold text-gray-800">
                      {stock.stockName}
                    </p>
                    <p className="text-sm-custom text-gray-500">
                      {stock.stockCode}
                    </p>
                  </div>
                </div>

                <div className="flex gap-main">
                  <button
                    className="px-3 py-1 text-sm-custom rounded-full bg-main-blue/20 text-main-blue hover:bg-main-blue/30 font-semibold transition-colors duration-300 ease-in-out"
                    onClick={() => openSettlementModal(stock, "buy")}
                  >
                    매수
                  </button>
                  <button
                    className="px-3 py-1 text-sm-custom rounded-full bg-main-red/20 text-main-red hover:bg-main-red/30 font-semibold transition-colors duration-300 ease-in-out"
                    onClick={() => openSettlementModal(stock, "sell")}
                  >
                    청산
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-x-main-2 text-sm-custom text-gray-700">
                <div className="flex justify-between gap-main">
                  <span className="text-main-dark-gray">현재가</span>
                  <span className="font-medium">
                    {stock.currentPrice.toLocaleString()}원
                  </span>
                </div>
                <div className="flex justify-between gap-main">
                  <span className="text-main-dark-gray">매수평균가</span>
                  <span className="font-medium">
                    {stock.entryPrice.toLocaleString()}원
                  </span>
                </div>
                <div className="flex justify-between gap-main">
                  <span className="text-main-dark-gray">보유수량</span>
                  <span className="font-medium">
                    {stock.stockCount.toLocaleString()}주
                  </span>
                </div>
                <div className="flex justify-between gap-main">
                  <span className="text-main-dark-gray">평가금액</span>
                  <span className="font-medium">
                    {(stock.stockCount * stock.entryPrice).toLocaleString()}원
                  </span>
                </div>
                <div className="flex justify-between gap-main">
                  <span className="text-main-dark-gray">수익</span>
                  <span
                    className={clsx(
                      "font-medium",
                      stock.profitLoss > 0 ? "text-main-red" : "text-main-blue"
                    )}
                  >
                    {stock.profitLoss > 0 && "+"}
                    {stock.profitLoss.toLocaleString()}원
                  </span>
                </div>
                <div className="flex justify-between gap-main">
                  <span className="text-main-dark-gray">수익률</span>
                  <span
                    className={clsx(
                      "font-medium",
                      stock.profitLossRate > 0
                        ? "text-main-red"
                        : "text-main-blue"
                    )}
                  >
                    {stock.profitLoss > 0 && "+"}
                    {stock.profitLossRate.toLocaleString()}%
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <SettlementModal
        isOpen={isOpenSettlementModal}
        onClose={() => {
          setIsOpenSettlementModal(false);
          setSelectedHoldings(null);
        }}
        hasCloseButton={false}
        isClickOutsideClose={false}
      >
        {selectedHoldings && (
          <div className="flex flex-col gap-main-2 min-w-[500px]">
            {/* 헤더 */}
            <div className="flex items-center justify-between pb-main border-b border-gray-100">
              <div className="flex items-center gap-main">
                <div className="flex gap-main items-center">
                  <div className="relative size-[40px] shrink-0">
                    <Image
                      src={selectedHoldings.stockImage}
                      alt={selectedHoldings.stockName}
                      fill
                      className="rounded-full shadow-sm"
                    />
                  </div>
                  <div>
                    <p className="text-lg-custom font-bold text-gray-800">
                      {selectedHoldings.stockName}
                    </p>
                    <p className="text-sm-custom text-gray-500">
                      {selectedHoldings.stockCode}
                    </p>
                  </div>
                </div>
              </div>
              <div
                className={clsx(
                  "px-4 py-2 rounded-full text-sm font-semibold",
                  selectedHoldings.changeType === "buy"
                    ? "bg-blue-100 text-blue-600"
                    : "bg-red-100 text-red-600"
                )}
              >
                {selectedHoldings.changeType === "buy"
                  ? "📈 추가 매수"
                  : "📉 청산"}
              </div>
            </div>

            {/* 현재 보유 정보 */}
            <div className="bg-gradient-to-br from-gray-50 to-slate-50 rounded-main p-main border border-gray-100">
              <h3 className="text-sm font-semibold text-gray-600 mb-2">
                현재 보유 현황
              </h3>
              <div className="grid grid-cols-3 gap-main text-center">
                <div>
                  <div className="text-xs text-gray-500">보유수량</div>
                  <div className="text-base font-bold text-gray-800">
                    {selectedHoldings.stockCount.toLocaleString()}주
                  </div>
                </div>
                <div>
                  <div className="text-xs text-gray-500">평균단가</div>
                  <div className="text-base font-bold text-gray-800">
                    {selectedHoldings.entryPrice.toLocaleString()}원
                  </div>
                </div>
                <div>
                  <div className="text-xs text-gray-500">현재가</div>
                  <div className="text-base font-bold text-gray-800">
                    {selectedHoldings.currentPrice.toLocaleString()}원
                  </div>
                </div>
              </div>
            </div>

            {/* 거래 입력 */}
            <div className="grid grid-cols-2 gap-main-2">
              {/* 가격 입력 */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  {selectedHoldings.changeType === "buy"
                    ? "매수가격"
                    : "매도가격"}
                </label>
                <div className="relative">
                  <Input
                    type="numeric"
                    value={selectedHoldings.changePrice}
                    min={1}
                    className="pr-8 text-lg font-medium"
                    onChange={(e) => {
                      const value = e.target.value;
                      if (/^\d*$/.test(value)) {
                        setSelectedHoldings((prev) => {
                          if (!prev) return null;
                          return { ...prev, changePrice: Number(value) };
                        });
                      }
                    }}
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">
                    원
                  </span>
                </div>
              </div>

              {/* 수량 입력 */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  {selectedHoldings.changeType === "buy"
                    ? "매수수량"
                    : "매도수량"}
                </label>
                <div className="relative">
                  <Input
                    type="numeric"
                    placeholder={
                      selectedHoldings.changeType === "buy"
                        ? "매수할 수량"
                        : "매도할 수량"
                    }
                    value={selectedHoldings.changeCount}
                    min={1}
                    max={
                      selectedHoldings.changeType === "buy"
                        ? Infinity
                        : selectedHoldings.stockCount
                    }
                    className="pr-8 text-lg font-medium"
                    onChange={(e) => {
                      const value = e.target.value;
                      if (/^\d*$/.test(value)) {
                        setSelectedHoldings((prev) => {
                          if (!prev) return null;
                          return {
                            ...prev,
                            changeCount: Number(value),
                          };
                        });
                      }
                    }}
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">
                    주
                  </span>
                </div>
              </div>
            </div>

            {/* 수량 선택 버튼 */}
            <div>
              <h3 className="text-sm font-semibold text-gray-700 mb-3">
                빠른 수량 선택
              </h3>
              <div
                className={clsx(
                  "grid gap-2",
                  selectedHoldings.changeType === "sell"
                    ? "grid-cols-5"
                    : "grid-cols-4"
                )}
              >
                {(selectedHoldings.changeType === "buy"
                  ? [1, 10, 50, 100]
                  : [1, 10, 50, 100]
                ).map((n) => (
                  <button
                    key={n}
                    className={clsx(
                      "py-2 px-3 rounded-main text-sm font-semibold transition-all duration-200",
                      selectedHoldings.changeType === "buy"
                        ? "bg-blue-50 text-blue-600 hover:bg-blue-100 border border-blue-200"
                        : "bg-red-50 text-red-600 hover:bg-red-100 border border-red-200"
                    )}
                    onClick={() => {
                      if (
                        selectedHoldings.changeType === "sell" &&
                        selectedHoldings.stockCount <
                          selectedHoldings.changeCount + n
                      ) {
                        setSelectedHoldings((prev) => {
                          if (!prev) return null;
                          return {
                            ...prev,
                            changeCount: prev.stockCount,
                          };
                        });
                        toast.error("최대 보유 수량만큼 청산할 수 있습니다.");
                        return;
                      }
                      setSelectedHoldings((prev) => {
                        if (!prev) return null;
                        return {
                          ...prev,
                          changeCount: prev.changeCount + n,
                        };
                      });
                    }}
                  >
                    +{n}주
                  </button>
                ))}
                {selectedHoldings.changeType === "sell" && (
                  <button
                    className="py-2 px-3 rounded-main text-sm font-semibold transition-all duration-200 hover:scale-105 bg-red-100 text-red-700 hover:bg-red-200 border border-red-300"
                    onClick={() => {
                      setSelectedHoldings((prev) => {
                        if (!prev) return null;
                        return {
                          ...prev,
                          changeCount: prev.stockCount,
                        };
                      });
                    }}
                  >
                    전체
                  </button>
                )}
              </div>
            </div>

            {/* 예상 결과 */}
            {selectedHoldings.changeCount > 0 &&
              selectedHoldings.changePrice > 0 && (
                <div
                  className={clsx(
                    "rounded-main p-main border",
                    selectedHoldings.changeType === "buy"
                      ? "bg-main-blue/10 border-main-blue/20"
                      : "bg-main-red/10 border-main-red/20"
                  )}
                >
                  <h3 className="text-sm font-semibold text-gray-700 mb-3">
                    거래 예상 결과
                  </h3>
                  <div className="grid grid-cols-2 gap-main">
                    <div>
                      <div className="text-xs text-gray-500">거래 금액</div>
                      <div className="text-lg font-bold text-gray-800">
                        {(
                          selectedHoldings.changeCount *
                          selectedHoldings.changePrice
                        ).toLocaleString()}
                        원
                      </div>
                    </div>
                    <div>
                      <div className="text-xs text-gray-500">
                        {selectedHoldings.changeType === "buy"
                          ? "거래 후 보유"
                          : "거래 후 잔여"}
                      </div>
                      <div className="text-lg font-bold text-gray-800">
                        {selectedHoldings.changeType === "buy"
                          ? (
                              selectedHoldings.stockCount +
                              selectedHoldings.changeCount
                            ).toLocaleString()
                          : (
                              selectedHoldings.stockCount -
                              selectedHoldings.changeCount
                            ).toLocaleString()}
                        주
                      </div>
                    </div>
                  </div>
                </div>
              )}

            {/* 액션 버튼 */}
            <div className="flex gap-3 pt-4">
              <Button
                variant="ghost"
                className="!rounded-main flex-1 !py-2"
                onClick={() => {
                  setIsOpenSettlementModal(false);
                  setSelectedHoldings(null);
                }}
              >
                취소
              </Button>
              <Button
                variant={
                  selectedHoldings.changeType === "buy" ? "primary" : "danger"
                }
                className="!rounded-main flex-1 !py-2"
                onClick={handleHoldingSettlement}
              >
                {selectedHoldings.changeType === "buy"
                  ? "매수 확정"
                  : "청산 확정"}
              </Button>
            </div>
          </div>
        )}
      </SettlementModal>

      <AddHoldingModal
        isOpen={isOpenAddHoldingModal}
        onClose={() => {
          setIsOpenAddHoldingModal(false);
          setSearchStockResult(null);
        }}
      >
        <div className="flex flex-col gap-main min-w-[600px]">
          <div className="flex items-center justify-between">
            <h2 className="text-xl-custom font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent">
              보유 종목 추가
            </h2>
          </div>

          {!searchStockResult && (
            <SearchStock
              onSelect={(stock) => {
                setSearchStockResult({
                  ...stock,
                  entryPrice: Number(stock.currentPrice),
                  stockCount: 1,
                });
              }}
            />
          )}

          {!searchStockResult && (
            <div className="flex flex-col items-center justify-center py-[60px] bg-gradient-to-br from-gray-50 to-gray-100/50 rounded-main border-2 border-dashed border-gray-200">
              <div className="w-16 h-16 bg-gradient-to-br from-main-blue/10 to-purple-100/20 rounded-full flex items-center justify-center mb-main">
                <span className="text-2xl">📈</span>
              </div>
              <span className="text-main-dark-gray font-medium mb-1">
                종목을 검색하여 추가해주세요
              </span>
              <span className="text-sm text-gray-500">
                검색창에서 원하는 종목을 찾아보세요
              </span>
            </div>
          )}

          {searchStockResult && (
            <div className="bg-white rounded-main border border-gray-200 shadow-sm overflow-hidden">
              {/* 헤더 */}
              <div className="bg-gradient-to-r from-main-blue/5 to-purple-50/30 px-main-2 py-main border-b border-gray-100">
                <div className="grid grid-cols-[2fr_1.5fr_1fr] gap-main-2">
                  <span className="text-sm font-semibold text-gray-700">
                    종목 정보
                  </span>
                  <span className="text-sm font-semibold text-gray-700 text-center">
                    구매가 (원)
                  </span>
                  <span className="text-sm font-semibold text-gray-700 text-center">
                    수량 (주)
                  </span>
                </div>
              </div>

              {/* 내용 */}
              <div className="p-main-2">
                <div className="grid grid-cols-[2fr_1.5fr_1fr] gap-main-2 items-center">
                  {/* 종목 정보 */}
                  <div className="flex items-center gap-main">
                    <div className="relative flex items-center justify-center size-[48px] shrink-0">
                      {searchStockResult.stockImage ? (
                        <Image
                          src={searchStockResult.stockImage}
                          alt={searchStockResult.stockName}
                          fill
                          className="rounded-full shadow-sm"
                        />
                      ) : (
                        <div className="bg-gradient-to-br from-main-blue/10 to-purple-100/20 rounded-full size-[48px] shrink-0 flex items-center justify-center shadow-sm">
                          <span className="text-main-blue font-bold text-lg">
                            {searchStockResult.stockName[0]}
                          </span>
                        </div>
                      )}
                    </div>
                    <div className="flex flex-col gap-1">
                      <span className="font-semibold text-main-dark-gray">
                        {searchStockResult.stockName}
                      </span>
                      <span className="text-xs text-gray-500 w-fit">
                        {searchStockResult.stockCode}
                      </span>
                    </div>
                  </div>

                  {/* 구매가 입력 */}
                  <div className="relative">
                    <Input
                      type="numeric"
                      value={searchStockResult.entryPrice}
                      min={1}
                      max={99999999999}
                      className="text-center pr-8 border-gray-200 focus:border-main-blue transition-colors"
                      onChange={(e) => {
                        const value = e.target.value;
                        if (/^\d*$/.test(value)) {
                          setSearchStockResult((prev) => {
                            if (!prev) return null;
                            return { ...prev, entryPrice: Number(value) };
                          });
                        }
                      }}
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">
                      원
                    </span>
                  </div>

                  {/* 수량 입력 */}
                  <div className="relative">
                    <Input
                      type="numeric"
                      value={searchStockResult.stockCount}
                      min={1}
                      max={1000000}
                      className="text-center pr-8 border-gray-200 focus:border-main-blue transition-colors"
                      onChange={(e) => {
                        const value = e.target.value;
                        if (/^\d*$/.test(value)) {
                          setSearchStockResult((prev) => {
                            if (!prev) return null;
                            return { ...prev, stockCount: Number(value) };
                          });
                        }
                      }}
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">
                      주
                    </span>
                  </div>
                </div>

                {/* 투자 금액 표시 */}
                <div className="mt-main-2 pt-main-2 border-t border-gray-100">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-600">총 투자 금액</span>
                    <span className="text-lg font-bold text-main-blue">
                      {(
                        searchStockResult.entryPrice *
                        searchStockResult.stockCount
                      ).toLocaleString()}
                      원
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {searchStockResult && (
            <div className="flex gap-main justify-end pt-main">
              <Button
                variant="ghost"
                onClick={() => setSearchStockResult(null)}
                className="hover:bg-gray-100"
              >
                취소
              </Button>
              <Button
                variant="primary"
                onClick={handleAddHolding}
                className="bg-gradient-to-r from-main-blue to-purple-600 hover:from-main-blue/90 hover:to-purple-600/90 shadow-md"
              >
                포트폴리오에 추가
              </Button>
            </div>
          )}
        </div>
      </AddHoldingModal>
    </>
  );
};

export default Holidings;
