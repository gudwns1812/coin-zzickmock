"use client";

import {
  approveRewardRedemption,
  rejectRewardRedemption,
} from "@/lib/futures-client-api";
import type {
  RewardRedemption,
  RewardRedemptionStatus,
} from "@/lib/futures-api";
import clsx from "clsx";
import { ArrowLeft, Check, Loader2, RotateCcw } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useState } from "react";
import { toast } from "react-toastify";

const STATUS_TABS: { label: string; value: RewardRedemptionStatus }[] = [
  { label: "대기", value: "PENDING" },
  { label: "승인 완료", value: "APPROVED" },
  { label: "반려", value: "REJECTED" },
  { label: "취소", value: "CANCELLED" },
];

type Props = {
  redemptions: RewardRedemption[];
  status: RewardRedemptionStatus;
  unavailable: boolean;
  message: string | null;
};

export default function AdminRewardRedemptionsClient({
  redemptions,
  status,
  unavailable,
  message,
}: Props) {
  const router = useRouter();
  const [memoByRequestId, setMemoByRequestId] = useState<Record<string, string>>(
    {}
  );
  const [pendingAction, setPendingAction] = useState<string | null>(null);

  const runAction = async (
    request: RewardRedemption,
    action: "send" | "cancel"
  ) => {
    const actionKey = `${request.requestId}:${action}`;
    setPendingAction(actionKey);

    try {
      const memo = memoByRequestId[request.requestId] ?? "";
      if (action === "send") {
        await approveRewardRedemption(request.requestId, memo);
        toast.success("교환권을 승인 완료 처리했습니다.");
      } else {
        await rejectRewardRedemption(request.requestId, memo);
        toast.success("교환권 신청을 반려하고 포인트를 환불했습니다.");
      }

      router.refresh();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "관리자 처리를 실패했습니다."
      );
    } finally {
      setPendingAction(null);
    }
  };

  return (
    <div className="px-main-2 pb-24 pt-4">
      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-start justify-between gap-main-2">
          <div>
            <p className="text-sm-custom text-main-dark-gray/60">Admin</p>
            <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
              교환권 신청 관리
            </h1>
            <p className="mt-3 text-sm-custom text-main-dark-gray/70 break-keep">
              대기 중인 커피 교환권 신청을 승인 완료하거나 반려 처리합니다.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Link
              className="flex items-center gap-2 rounded-main bg-main-light-gray px-main py-2 text-sm-custom font-semibold text-main-dark-gray/70 hover:text-main-blue"
              href="/admin"
            >
              <ArrowLeft size={15} />
              관리자 홈
            </Link>
            <Link
              className="rounded-main bg-main-light-gray px-main py-2 text-sm-custom font-semibold text-main-dark-gray/70 hover:text-main-blue"
              href="/admin/shop-items"
            >
              상품 관리
            </Link>
          </div>
        </div>
      </section>

      <div className="mt-main-2 flex items-center gap-2">
        {STATUS_TABS.map((tab) => {
          const active = tab.value === status;
          return (
            <Link
              className={clsx(
                "rounded-main px-main py-2 text-sm-custom font-semibold transition-colors",
                active
                  ? "bg-main-blue text-white"
                  : "bg-white text-main-dark-gray/70 hover:text-main-blue"
              )}
              href={`/admin/reward-redemptions?status=${tab.value}`}
              key={tab.value}
            >
              {tab.label}
            </Link>
          );
        })}
      </div>

      {unavailable ? (
        <div className="mt-main-2 rounded-main border border-main-light-gray bg-white p-main-2 text-main-dark-gray/70">
          {message ?? "관리자 권한이 필요하거나 목록을 불러오지 못했습니다."}
        </div>
      ) : (
        <div className="mt-main-2 overflow-hidden rounded-main border border-main-light-gray bg-white shadow-sm">
          <div className="grid grid-cols-[1.2fr_1fr_1fr_1fr_1.3fr] gap-main border-b border-main-light-gray bg-main-light-gray/35 px-main py-3 text-xs-custom font-semibold text-main-dark-gray/55">
            <span>신청</span>
            <span>사용자</span>
            <span>연락처</span>
            <span>시각</span>
            <span>처리</span>
          </div>

          {redemptions.length === 0 ? (
            <div className="px-main py-main-2 text-sm-custom text-main-dark-gray/55">
              해당 상태의 신청이 없습니다.
            </div>
          ) : (
            redemptions.map((request) => (
              <div
                className="grid grid-cols-[1.2fr_1fr_1fr_1fr_1.3fr] gap-main border-b border-main-light-gray px-main py-4 text-sm-custom last:border-b-0"
                key={request.requestId}
              >
                <div>
                  <p className="font-semibold text-main-dark-gray">
                    {request.itemName}
                  </p>
                  <p className="mt-1 text-xs-custom text-main-dark-gray/50">
                    {request.pointAmount.toLocaleString("ko-KR")} P ·{" "}
                    {request.requestId}
                  </p>
                </div>
                <span className="text-main-dark-gray/70">{request.memberId}</span>
                <span className="font-semibold text-main-dark-gray">
                  {request.submittedPhoneNumber}
                </span>
                <span className="text-main-dark-gray/60">
                  {formatDateTime(request.requestedAt)}
                </span>
                <div className="flex flex-col gap-2">
                  {request.status === "PENDING" ? (
                    <>
                      <input
                        className="rounded-main border border-main-light-gray px-3 py-2 text-xs-custom outline-none focus:border-main-blue"
                        onChange={(event) =>
                          setMemoByRequestId((current) => ({
                            ...current,
                            [request.requestId]: event.target.value,
                          }))
                        }
                        placeholder="관리 메모"
                        value={memoByRequestId[request.requestId] ?? ""}
                      />
                      <div className="flex gap-2">
                        <ActionButton
                          disabled={pendingAction !== null}
                          icon={<Check size={15} />}
                          isPending={
                            pendingAction === `${request.requestId}:send`
                          }
                          label="승인 완료"
                          onClick={() => runAction(request, "send")}
                        />
                        <ActionButton
                          disabled={pendingAction !== null}
                          icon={<RotateCcw size={15} />}
                          isPending={
                            pendingAction === `${request.requestId}:cancel`
                          }
                          label="반려"
                          onClick={() => runAction(request, "cancel")}
                        />
                      </div>
                    </>
                  ) : (
                    <span className="text-main-dark-gray/55">
                      {request.adminMemo || statusLabel(request.status)}
                    </span>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

function ActionButton({
  icon,
  disabled,
  isPending,
  label,
  onClick,
}: {
  icon: ReactNode;
  disabled: boolean;
  isPending: boolean;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      className="flex items-center gap-1 rounded-main bg-main-blue px-3 py-2 text-xs-custom font-semibold text-white disabled:bg-main-light-gray disabled:text-main-dark-gray/40"
      disabled={disabled}
      onClick={onClick}
      type="button"
    >
      {isPending ? <Loader2 size={15} className="animate-spin" /> : icon}
      {label}
    </button>
  );
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "short",
    timeStyle: "short",
    timeZone: "Asia/Seoul",
  }).format(new Date(value));
}

function statusLabel(status: RewardRedemptionStatus): string {
  if (status === "APPROVED" || status === "SENT") return "승인 완료";
  if (status === "REJECTED" || status === "CANCELLED_REFUNDED") return "반려";
  if (status === "CANCELLED") return "취소";
  return "대기";
}
