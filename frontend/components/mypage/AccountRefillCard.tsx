"use client";

import Modal from "@/components/ui/Modal";
import { refillFuturesAccount } from "@/lib/futures-client-api";
import type {
  AccountRefillStatus,
  FuturesAccountSummary,
} from "@/lib/futures-api";
import { formatUsd } from "@/lib/markets";
import { RotateCcw, Loader2, WalletCards } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "react-toastify";

type Props = {
  account: FuturesAccountSummary;
  refillStatus: AccountRefillStatus;
};

export default function AccountRefillCard({ account, refillStatus }: Props) {
  const router = useRouter();
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const disabled = !refillStatus.refillable || isSubmitting;

  const openConfirm = () => {
    if (!refillStatus.refillable) {
      toast.error(refillStatus.disabledReason ?? "지금은 리필할 수 없습니다.");
      return;
    }
    setIsConfirmOpen(true);
  };

  const closeConfirm = () => {
    if (isSubmitting) return;
    setIsConfirmOpen(false);
  };

  const submitRefill = async () => {
    setIsSubmitting(true);
    try {
      const result = await refillFuturesAccount();
      toast.success(
        `지갑 잔고와 사용 가능 금액을 ${formatUsd(result.walletBalance)}로 리필했습니다.`
      );
      setIsConfirmOpen(false);
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "리필에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <div className="rounded-main border border-main-light-gray bg-white p-main shadow-sm">
        <div className="flex items-start justify-between gap-main">
          <div>
            <p className="text-xs-custom text-main-dark-gray/55">리필 가능</p>
            <p className="mt-2 text-xl-custom font-bold text-main-dark-gray">
              {refillStatus.remainingCount.toLocaleString("ko-KR")}회
            </p>
          </div>
          <div className="flex size-10 items-center justify-center rounded-main bg-main-blue/10 text-main-blue">
            <WalletCards size={20} />
          </div>
        </div>
        <button
          className={[
            "mt-main flex w-full items-center justify-center gap-2 rounded-main px-main py-2 text-sm-custom font-semibold transition-colors",
            disabled
              ? "bg-main-light-gray text-main-dark-gray/40"
              : "bg-main-blue text-white hover:bg-main-blue/90",
          ].join(" ")}
          disabled={disabled}
          onClick={openConfirm}
          type="button"
        >
          {isSubmitting ? (
            <Loader2 size={16} className="animate-spin" />
          ) : (
            <RotateCcw size={16} />
          )}
          리필
        </button>
        {refillStatus.disabledReason && (
          <p className="mt-2 min-h-8 text-xs-custom font-semibold leading-4 text-main-dark-gray/50">
            {refillStatus.disabledReason}
          </p>
        )}
      </div>

      <Modal
        hasBackdropBlur={false}
        isEscapeClose
        isOpen={isConfirmOpen}
        onClose={closeConfirm}
      >
        <div className="w-[min(460px,calc(100vw-48px))] pr-6">
          <p className="text-lg-custom font-bold text-main-dark-gray">
            지갑 잔고 리필
          </p>
          <div className="mt-5 grid grid-cols-2 gap-2 text-sm-custom">
            <Amount label="현재 지갑" value={formatUsd(account.walletBalance)} />
            <Amount label="현재 사용 가능" value={formatUsd(account.available)} />
            <Amount label="리필 후 지갑" value={formatUsd(refillStatus.targetWalletBalance)} />
            <Amount label="리필 후 사용 가능" value={formatUsd(refillStatus.targetAvailableMargin)} />
          </div>
          <p className="mt-4 text-sm-custom leading-6 text-main-dark-gray/65 break-keep">
            열린 포지션과 대기 주문이 없는 상태에서 지갑 잔고와 사용 가능 금액을 100,000 USDT로 회복합니다.
          </p>
          <div className="mt-6 flex justify-end gap-2">
            <button
              className="rounded-main border border-main-light-gray px-main py-2 text-sm-custom font-semibold text-main-dark-gray/70"
              disabled={isSubmitting}
              onClick={closeConfirm}
              type="button"
            >
              닫기
            </button>
            <button
              className="flex items-center gap-2 rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white disabled:bg-main-light-gray disabled:text-main-dark-gray/40"
              disabled={isSubmitting}
              onClick={submitRefill}
              type="button"
            >
              {isSubmitting && <Loader2 size={16} className="animate-spin" />}
              리필하기
            </button>
          </div>
        </div>
      </Modal>
    </>
  );
}

function Amount({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-main bg-main-light-gray/35 p-main">
      <p className="text-xs-custom font-semibold text-main-dark-gray/50">
        {label}
      </p>
      <p className="mt-2 text-sm-custom font-bold text-main-dark-gray">
        {value}
      </p>
    </div>
  );
}
