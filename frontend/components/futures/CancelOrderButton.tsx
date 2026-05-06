"use client";

import Button from "@/components/ui/shared/Button";
import { useRouter } from "next/navigation";
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
  const router = useRouter();
  const [isPending, setIsPending] = useState(false);

  async function handleCancel() {
    setIsPending(true);

    try {
      const response = await fetch(`/proxy-futures/orders/${orderId}/cancel`, {
        method: "POST",
      });

      const payload =
        (await response.json()) as ClientApiResponse<CancelOrderResponse>;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message ?? "주문 취소에 실패했습니다.");
      }

      toast.success(`${payload.data.symbol} 대기 주문을 취소했습니다.`);
      router.refresh();
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
