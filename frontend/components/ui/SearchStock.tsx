"use client";

import Modal from "@/components/ui/Modal";
import Input from "@/components/ui/shared/Input";
import { useDebounce } from "@/hooks/useDebounce";
import clsx from "clsx";
import { Plus, Search } from "lucide-react";
import React, { useEffect, useState } from "react";
import UpPrice from "./shared/UpPrice";
import DownPrice from "./shared/DownPrice";
import Image from "next/image";
import { SearchResult } from "@/store/useInterestStore";

interface SearchStockProps {
  onSelect?: (stock: SearchResult) => void;
}

const SearchStock = ({ onSelect }: SearchStockProps) => {
  const [stockSearch, setStockSearch] = useState("");
  const debouncedSearch = useDebounce(stockSearch, 500);
  const [isOpen, setIsOpen] = useState(false);
  const [searchResult, setSearchResult] = useState<SearchResult[]>([]);

  useEffect(() => {
    if (debouncedSearch) {
      searchStocks(debouncedSearch);
    }
  }, [debouncedSearch]);

  useEffect(() => {
    if (stockSearch === "") {
      setSearchResult([]);
    }
  }, [stockSearch]);

  const searchStocks = async (query: string) => {
    if (!query) return;
    const res = await fetch(`/proxy2/v2/stocks/search?keyword=${query}`);
    const json = await res.json();
    setSearchResult(json.data);
  };
  return (
    <>
      <button
        className="w-fit flex items-center gap-[5px] bg-main-blue/10 text-main-blue rounded-main px-3 py-1 hover:bg-main-blue/20 transition-colors duration-200 ease-in-out"
        onClick={() => setIsOpen(true)}
      >
        <Search size={16} />
        <span>종목 검색</span>
      </button>

      <Modal
        isOpen={isOpen}
        onClose={() => {
          setIsOpen(false);
          setStockSearch("");
          setSearchResult([]);
        }}
        hasCloseButton={false}
      >
        <div className="w-[500px] flex flex-col gap-main">
          <div className="w-full shadow-color rounded-main relative">
            <Input
              ref={(el) => {
                if (el && isOpen) {
                  el.focus();
                }
              }}
              value={stockSearch}
              onChange={(e) => setStockSearch(e.target.value)}
              placeholder="종목명 또는 코드 검색"
              width={400}
              onClick={() => setIsOpen(true)}
            />
            <Search className="absolute right-main top-[50%] translate-y-[-50%]" />
          </div>

          {stockSearch && searchResult.length === 0 ? (
            <div className="w-full h-[200px] flex items-center justify-center">
              <p className="text-main-dark-gray">검색 결과가 없습니다.</p>
            </div>
          ) : (
            <>
              {searchResult.map((result, idx) => (
                <div
                  className="w-full flex cursor-pointer flex-col justify-around hover:bg-main-blue/10 rounded-main transition-colors duration-200 ease-in-out p-main gap-[5px] group relative"
                  key={`search-stock-${result.stockCode}-${idx}`}
                  onClick={() => {
                    setIsOpen(false);
                    setStockSearch("");
                    setSearchResult([]);
                    if (onSelect) onSelect(result);
                  }}
                >
                  <div className="flex gap-main w-full">
                    <div className="relative flex items-center justify-center size-[40px] shrink-0">
                      {result.stockImage ? (
                        <Image
                          src={result.stockImage}
                          alt={result.stockName}
                          fill
                          className="rounded-full"
                        />
                      ) : (
                        <div className="bg-main-blue/10 rounded-full size-[40px] shrink-0 flex items-center justify-center">
                          <span className="text-main-blue font-semibold">
                            {result.stockName[0]}
                          </span>
                        </div>
                      )}
                    </div>
                    <div className="flex flex-col flex-1 truncate">
                      <div className="text-gray-800 truncate w-full flex items-baseline gap-1">
                        <span className="font-bold">{result.stockName}</span>
                        <span className="text-gray-500 text-xs-custom">
                          {result.stockCode}
                        </span>
                      </div>
                      <div className="text-sm-custom flex gap-main items-center">
                        <span
                          className={clsx(
                            "text-gray-500 text-sm-custom font-semibold",
                            (result.sign === "1" || result.sign === "2") &&
                              "text-main-red",
                            (result.sign === "4" || result.sign === "5") &&
                              "text-main-blue",
                            result.sign === "3" && "text-gray-500"
                          )}
                        >
                          {Number(result.currentPrice).toLocaleString()}
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
                    <Plus
                      className="hidden group-hover:block text-main-blue absolute top-1/2 -translate-y-1/2 right-main"
                      size={20}
                    />
                  </div>
                </div>
              ))}
            </>
          )}
        </div>
      </Modal>
    </>
  );
};

export default SearchStock;
