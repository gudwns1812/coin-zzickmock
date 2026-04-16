"use client";

import Button from "@/components/ui/shared/Button";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "react-toastify";

type Props = {
  symbol: string;
  positionSide: "LONG" | "SHORT";
  marginMode: "ISOLATED" | "CROSS";
  quantity: number;
};

type ClosePositionResponse = {
  symbol: string;
  closedQuantity: number;
  realizedPnl: number;
  grantedPoint: number;
};

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export default function ClosePositionButton({
  symbol,
  positionSide,
  marginMode,
  quantity,
}: Props) {
  const router = useRouter();
  const [isPending, setIsPending] = useState(false);

  async function handleClose() {
    setIsPending(true);

    try {
      const response = await fetch("/proxy-futures/positions/close", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          symbol,
          positionSide,
          marginMode,
          quantity,
        }),
      });

      const payload =
        (await response.json()) as ClientApiResponse<ClosePositionResponse>;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message ?? "포지션 종료에 실패했습니다.");
      }

      toast.success(
        `${payload.data.symbol} 포지션 종료 완료 · 손익 ${payload.data.realizedPnl.toFixed(2)} USDT`
      );
      router.refresh();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "포지션 종료에 실패했습니다."
      );
    } finally {
      setIsPending(false);
    }
  }

  return (
    <Button disabled={isPending} onClick={handleClose} variant="danger">
      {isPending ? "종료 중..." : "포지션 종료"}
    </Button>
  );
}
