"use client";

import Button from "@/components/ui/shared/Button";
import {
  invalidateRewardAndShopQueries,
  invalidateTradingQueries,
} from "@/lib/futures-query-invalidation";
import { fetchFuturesBackendApi } from "@/lib/futures-api-request";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "react-toastify";

type Props = {
  orderId: string;
};

type CancelOrderResponse = {
  orderId: string;
  symbol: string;
  status: string;
};

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export default function CancelOrderButton({ orderId }: Props) {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  async function handleCancel() {
    setIsPending(true);

    try {
      const response = await fetchFuturesBackendApi(`/orders/${orderId}/cancel`, {
        method: "POST",
        credentials: "include",
      });

      const payload =
        (await response.json()) as ClientApiResponse<CancelOrderResponse>;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message ?? "주문 취소에 실패했습니다.");
      }

      toast.success(`${payload.data.symbol} 대기 주문을 취소했습니다.`);
      void Promise.all([
        invalidateTradingQueries(queryClient),
        invalidateRewardAndShopQueries(queryClient),
      ]);
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "주문 취소에 실패했습니다."
      );
    } finally {
      setIsPending(false);
    }
  }

  return (
    <Button
      className="h-9 min-w-[92px] px-3 py-0 text-sm-custom font-semibold leading-none"
      disabled={isPending}
      onClick={handleCancel}
      type="button"
      variant="danger"
    >
      {isPending ? "취소 중..." : "주문 취소"}
    </Button>
  );
}
