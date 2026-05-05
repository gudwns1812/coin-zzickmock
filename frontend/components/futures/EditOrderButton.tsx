"use client";

import Modal from "@/components/ui/Modal";
import Button from "@/components/ui/shared/Button";
import Input from "@/components/ui/shared/Input";
import { modifyFuturesOrderPrice } from "@/lib/futures-client-api";
import { submitOrderPriceEdit } from "@/lib/futures-order-edit";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "react-toastify";

type Props = {
  orderId: string;
  symbol: string;
  currentLimitPrice: number;
};

export default function EditOrderButton({
  orderId,
  symbol,
  currentLimitPrice,
}: Props) {
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const [limitPrice, setLimitPrice] = useState(() => String(currentLimitPrice));
  const [isPending, setIsPending] = useState(false);

  function openModal() {
    setLimitPrice(String(currentLimitPrice));
    setIsOpen(true);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setIsPending(true);
    try {
      await submitOrderPriceEdit({
        orderId,
        limitPrice,
        modifyOrderPrice: modifyFuturesOrderPrice,
        refresh: () => router.refresh(),
        closeModal: () => setIsOpen(false),
        showSuccess: toast.success,
        showError: toast.error,
      });
    } finally {
      setIsPending(false);
    }
  }

  return (
    <>
      <Button
        aria-label="Edit Open Order Price"
        className="py-2"
        disabled={isPending}
        onClick={openModal}
        variant="ghost"
      >
        주문 수정
      </Button>
      <Modal
        hasBackdropBlur={false}
        isEscapeClose
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
      >
        <form className="w-[360px] space-y-5" onSubmit={handleSubmit}>
          <div className="space-y-2 pr-7">
            <p className="text-lg-custom font-semibold text-main-dark-gray">
              주문 가격 수정
            </p>
            <p className="text-sm-custom text-main-dark-gray/60">
              {symbol} 대기 지정가 주문의 가격만 변경합니다. 즉시 체결 가능한
              가격은 저장되지 않습니다.
            </p>
          </div>
          <label className="block space-y-2 text-sm-custom font-semibold text-main-dark-gray">
            <span>Limit price</span>
            <Input
              aria-label="Open order limit price"
              className="text-right text-base-custom font-semibold"
              inputMode="decimal"
              min="0"
              onChange={(event) => setLimitPrice(event.target.value)}
              step="any"
              type="number"
              value={limitPrice}
            />
          </label>
          <div className="flex justify-end gap-2">
            <Button
              disabled={isPending}
              onClick={() => setIsOpen(false)}
              type="button"
              variant="ghost"
            >
              닫기
            </Button>
            <Button disabled={isPending} type="submit">
              {isPending ? "수정 중..." : "주문 수정"}
            </Button>
          </div>
        </form>
      </Modal>
    </>
  );
}
