"use client";

import { JwtToken } from "@/type/jwt";
import clsx from "clsx";
import { Heart } from "lucide-react";
import React, { useState, useEffect } from "react";
import { IconButton } from "@/components/animate-ui/buttons/icon";
import { useInterestStore } from "@/store/useInterestStore";
import { toast } from "react-toastify";

const Scrab = ({
  className,
  stockCode,
  stockName,
  stockInfo,
  token,
  onClick,
}: {
  className?: string;
  stockCode: string;
  stockName: string;
  stockInfo?: {
    stockImage?: string;
    currentPrice?: string;
    changeAmount?: string;
    changeRate?: string;
    sign?: string;
  };
  token: JwtToken | null;
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void;
}) => {
  const [isScrab, setIsScrab] = useState(false);

  const {
    interestGroups,
    interestStocks,
    selectedGroupId,
    fetchGroups,
    fetchStocks,
    addStock,
    deleteStock,
  } = useInterestStore();

  // 메인 그룹 찾기
  const mainGroup = interestGroups.find((group) => group.main);

  // 메인 그룹에서 현재 종목이 있는지 확인
  useEffect(() => {
    if (!token || !mainGroup) {
      setIsScrab(false);
      return;
    }

    if (mainGroup.groupId === selectedGroupId) {
      // 메인 그룹이 현재 선택된 그룹과 같으면 전역 상태에서 확인
      const isInMainGroup = interestStocks.some(
        (stock) => stock.stockInfo.stockCode === stockCode
      );
      setIsScrab(isInMainGroup);
    } else {
      // 메인 그룹이 다른 그룹이면 별도로 확인
      const checkMainGroupStock = async () => {
        try {
          const res = await fetch(
            `/proxy/favorite/${token.memberId}/${mainGroup.groupId}`
          );
          const data = await res.json();

          if (res.ok) {
            const isInMainGroup = data.some(
              (stock: any) => stock.stockInfo.stockCode === stockCode
            );
            setIsScrab(isInMainGroup);
          } else {
            setIsScrab(false);
          }
        } catch (error) {
          console.error("메인 그룹 종목 확인 실패:", error);
          setIsScrab(false);
        }
      };

      checkMainGroupStock();
    }
  }, [token, mainGroup, stockCode, selectedGroupId, interestStocks]);

  // 초기 그룹 데이터 로드
  useEffect(() => {
    if (!token) return;
    fetchGroups(token);
  }, [token, fetchGroups]);

  const handleToggleScrab = async (e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();

    if (!token) {
      toast.error("로그인이 필요합니다.");
      return;
    }

    if (!mainGroup) {
      toast.error("메인 그룹이 설정되지 않았습니다.");
      return;
    }

    try {
      if (isScrab) {
        // 관심종목에서 제거
        await deleteStock(token, mainGroup.groupId, stockCode);
        setIsScrab(false);
        // toast.success(`${stockName} 종목이 관심종목에서 제거되었습니다.`);
      } else {
        // 관심종목에 추가 - 전달받은 정보 또는 기본값 사용
        const stockData = {
          stockCode,
          stockName,
          changeAmount: stockInfo?.changeAmount || "0",
          changeRate: stockInfo?.changeRate || "0",
          currentPrice: stockInfo?.currentPrice || "0",
          sign: stockInfo?.sign || "3",
          stockImage: stockInfo?.stockImage || "",
        };

        await addStock(token, mainGroup.groupId, stockData);
        setIsScrab(true);
        // toast.success(`${stockName} 종목이 관심종목에 추가되었습니다.`);
      }
    } catch (error) {
      console.error("관심종목 토글 실패:", error);
      toast.error("관심종목 변경에 실패했습니다.");
    }

    if (onClick) {
      onClick(e);
    }
  };

  if (!token) return null;

  return (
    <div
      className={clsx(
        "transition-all duration-400 absolute left-1 bottom-0 z-10",
        isScrab ? "opacity-100" : "opacity-0 group-hover:opacity-100",
        className
      )}
    >
      <IconButton
        icon={Heart}
        active={isScrab}
        size="sm"
        onClick={handleToggleScrab}
      />
    </div>
  );
};

export default Scrab;
