"use client";

import { recordStockSearchSelection } from "@/api/stocks";
import clsx from "clsx";
import { ChevronRight, Search, SearchIcon } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import React, { useEffect, useState } from "react";
import Image from "next/image";
import Modal from "../../Modal";
import { useDebounce } from "@/hooks/useDebounce";
import UpPrice from "../UpPrice";
import DownPrice from "../DownPrice";
import { useActiveStockSetStore } from "@/store/useActiveStockSetStore";

type StockSearchResult = {
  changeAmount: string;
  changeRate: string;
  currentPrice: string;
  sign: string;
  stockCode: string;
  stockName: string;
  stockImage: string;
};

const SearchModal = Modal;

const Navigation = () => {
  const [isOpenSearchModal, setIsOpenSearchModal] = useState(false);
  const [stockSearch, setStockSearch] = useState("");
  const [stockSearchResult, setStockSearchResult] = useState<
    StockSearchResult[]
  >([]);
  const [isMac, setIsMac] = useState(false);
  const [isClient, setIsClient] = useState(false);
  const { setSourceStocks, clearSourceStocks } = useActiveStockSetStore();

  const debouncedStockSearch = useDebounce(stockSearch, 500);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    setIsClient(true);
    if (typeof window !== "undefined") {
      setIsMac(/Mac/.test(navigator.platform));
    }
  }, []);

  const modKey = isClient ? (isMac ? "⌘" : "Ctrl") : "Ctrl";

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const isModKeyPressed = isMac ? event.metaKey : event.ctrlKey;

      if (event.key.toLowerCase() === "k" && isModKeyPressed) {
        event.preventDefault();
        setIsOpenSearchModal(true);
      }
    };

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isMac]);

  useEffect(() => {
    if (debouncedStockSearch) {
      searchStocks(debouncedStockSearch);
    }
  }, [debouncedStockSearch]);

  useEffect(() => {
    if (stockSearch === "") {
      setStockSearchResult([]);
    }
  }, [stockSearch]);

  useEffect(() => {
    if (isOpenSearchModal) {
      setStockSearch("");
      setStockSearchResult([]);
    }
  }, [isOpenSearchModal]);

  useEffect(() => {
    if (!isOpenSearchModal || !stockSearch || stockSearchResult.length === 0) {
      setSourceStocks("search-results", []);
      return;
    }

    setSourceStocks(
      "search-results",
      stockSearchResult.map((stock) => stock.stockCode)
    );
  }, [
    isOpenSearchModal,
    setSourceStocks,
    stockSearch,
    stockSearchResult,
  ]);

  useEffect(() => {
    return () => {
      clearSourceStocks("search-results");
    };
  }, [clearSourceStocks]);

  if (!pathname || pathname === "/") {
    return null;
  }

  const searchStocks = async (query: string) => {
    const res = await fetch(`/proxy2/v2/stocks/search?keyword=${query}`);
    if (!res.ok) {
      setStockSearchResult([]);
      return;
    }

    const json = await res.json();
    setStockSearchResult(json.data);
  };

  const handleClickSearchResult = (stockCode: string) => {
    void recordStockSearchSelection(stockCode).catch(() => undefined);
    setIsOpenSearchModal(false);
    router.push(`/stock/${stockCode}`);
  };

  const handleCloseSearchModal = () => {
    setStockSearch("");
    setStockSearchResult([]);
    setIsOpenSearchModal(false);
  };

  return (
    <>
      <nav className="flex items-center gap-5 absolute left-1/2 -translate-x-1/2 top-1/2 -translate-y-1/2 min-w-[550px]">
        <Link
          href="/stock"
          className={clsx(
            "text-base-custom",
            pathname.startsWith("/stock")
              ? "text-black font-semibold"
              : "text-sub"
          )}
        >
          증권
        </Link>
        <Link
          href="/portfolio"
          className={clsx(
            "text-base-custom",
            pathname.startsWith("/portfolio")
              ? "text-black font-semibold"
              : "text-sub"
          )}
        >
          포트폴리오
        </Link>

        <button
          className="text-sub flex items-center gap-main bg-main-light-gray/70 rounded-main px-2 py-1"
          onClick={() => setIsOpenSearchModal(true)}
        >
          <input
            placeholder="종목 검색"
            type="text"
            className="text-sm-custom pointer-events-none w-[140px]"
          />

          <div className="flex items-center gap-1 pointer-events-none">
            <kbd className="px-1.5 py-0.5 rounded border-[0.5px] border-main-dark-gray/70 text-xs-custom font-mono flex items-center justify-center">
              <span className="text-main-dark-gray/70">{modKey}</span>
            </kbd>
            <span className="text-main-dark-gray/70">+</span>
            <kbd className="px-1.5 py-0.5 rounded border-[0.5px] border-main-dark-gray/70 text-xs-custom font-mono flex items-center justify-center">
              <span className="text-main-dark-gray/70">K</span>
            </kbd>
          </div>
          <SearchIcon className="size-4" />
        </button>
      </nav>

      <SearchModal
        isOpen={isOpenSearchModal}
        onClose={handleCloseSearchModal}
        isEscapeClose
      >
        <div className="flex flex-col gap-main min-w-[500px]">
          <div className="flex items-center justify-between gap-main">
            <h2 className="text-lg-custom font-semibold text-main-blue">
              종목 검색
            </h2>
            <span className="text-main-dark-gray/60 text-xs-custom">
              종목명 또는 코드를 입력해 주세요.
            </span>
          </div>

          <div className="relative w-full">
            <input
              type="text"
              autoFocus
              placeholder="종목명 또는 코드 검색"
              className="w-full outline-none border border-main-dark-gray/30 rounded-main px-main py-1"
              value={stockSearch}
              onChange={(e) => setStockSearch(e.target.value)}
            />
            <Search
              className="absolute right-main top-1/2 -translate-y-1/2 text-main-dark-gray"
              size={16}
            />
          </div>

          {stockSearch &&
            stockSearchResult.map((result, idx) => (
              <button
                className="w-full flex flex-col justify-around hover:bg-main-blue/10 rounded-main transition-colors duration-200 ease-in-out p-main gap-[5px] group relative"
                key={`search-stock-${result.stockCode}-${idx}`}
                onClick={() => handleClickSearchResult(result.stockCode)}
              >
                <div className="flex items-center gap-2 w-full">
                  <div className="relative flex items-center justify-center size-[40px] shrink-0">
                    {result.stockImage ? (
                      <Image
                        src={result.stockImage}
                        alt={result.stockName}
                        fill
                        className="rounded-full"
                        sizes="40px"
                      />
                    ) : (
                      <div className="bg-main-blue/10 rounded-full size-[40px] shrink-0 flex items-center justify-center">
                        <span className="text-main-blue font-semibold">
                          {result.stockName[0]}
                        </span>
                      </div>
                    )}
                  </div>
                  <div className="flex flex-col flex-1 truncate text-sm-custom">
                    <p className="flex items-center gap-main text-gray-800 truncate w-full">
                      <span className="font-bold">{result.stockName}</span>
                      <span className="text-gray-400">{result.stockCode}</span>
                    </p>
                    <div className="flex items-center gap-main">
                      <span className="text-main-dark-gray">
                        {Number(result.currentPrice).toLocaleString()}원
                      </span>
                      <div className="flex justify-between h-fit">
                        {(result.sign === "1" || result.sign === "2") && (
                          <UpPrice
                            change={Number(result.changeAmount)}
                            changeRate={Number(result.changeRate)}
                          />
                        )}
                        {result.sign === "3" && (
                          <span className="text-gray-400 font-medium">
                            {Number(result.changeAmount)} (
                            {Number(result.changeRate)}%)
                          </span>
                        )}
                        {(result.sign === "4" || result.sign === "5") && (
                          <DownPrice
                            change={Number(result.changeAmount)}
                            changeRate={Number(result.changeRate)}
                          />
                        )}
                      </div>
                    </div>
                  </div>
                </div>
                <div className="absolute top-1/2 -translate-y-1/2 right-main hidden group-hover:block cursor-pointer">
                  <ChevronRight
                    className="text-main-blue hover:bg-main-blue/30 rounded-full p-1 box-content transition-colors duration-200 ease-in-out"
                    size={20}
                  />
                </div>
              </button>
            ))}

          {!stockSearch && (
            <div className="flex flex-col gap-main">
              <p className="text-sm-custom text-main-dark-gray my-10 text-center">
                종목명 또는 코드를 입력해주세요.
              </p>
            </div>
          )}

          {stockSearch && stockSearchResult.length === 0 && (
            <div className="flex flex-col gap-main">
              <p className="text-sm-custom text-main-dark-gray my-10 text-center">
                검색 결과가 없습니다.
              </p>
            </div>
          )}
        </div>
      </SearchModal>
    </>
  );
};

export default Navigation;
