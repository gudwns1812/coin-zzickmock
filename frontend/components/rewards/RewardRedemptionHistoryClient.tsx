"use client";

import { cancelOwnRewardRedemption } from "@/lib/futures-client-api";
import type {
  RewardRedemption,
  RewardRedemptionsResult,
} from "@/lib/futures-api";
import { Clock3, Loader2, RotateCcw } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "react-toastify";

type Props = {
  redemptions: RewardRedemptionsResult;
};

export default function RewardRedemptionHistoryClient({
  redemptions,
}: Props) {
  const router = useRouter();
  const [cancellingRequestId, setCancellingRequestId] = useState<string | null>(
    null
  );

  const cancelRedemption = async (request: RewardRedemption) => {
    if (request.status !== "PENDING") return;

    setCancellingRequestId(request.requestId);

    try {
      await cancelOwnRewardRedemption(request.requestId);
      toast.success("교환 신청을 취소했습니다.");
      router.refresh();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "교환 신청 취소에 실패했습니다."
      );
    } finally {
      setCancellingRequestId(null);
    }
  };

  if (redemptions.unavailable) {
    return (
      <div className="rounded-main bg-main-light-gray/45 p-main text-sm-custom text-main-dark-gray/60">
        {redemptions.message ?? "교환 내역을 불러오지 못했습니다."}
      </div>
    );
  }

  if (redemptions.redemptions.length === 0) {
    return (
      <div className="rounded-main bg-main-light-gray/45 p-main text-sm-custom text-main-dark-gray/60">
        아직 교환 내역이 없습니다.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-main border border-main-light-gray">
      <div className="grid min-w-[760px] grid-cols-[1.2fr_0.8fr_1fr_1fr_0.8fr] gap-main bg-main-light-gray/35 px-main py-3 text-xs-custom font-semibold text-main-dark-gray/55">
        <span>상품</span>
        <span>상태</span>
        <span>연락처</span>
        <span>신청 시각</span>
        <span>처리</span>
      </div>
      {redemptions.redemptions.map((request) => (
        <div
          className="grid min-w-[760px] grid-cols-[1.2fr_0.8fr_1fr_1fr_0.8fr] items-center gap-main border-t border-main-light-gray px-main py-4 text-sm-custom"
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
          <span
            className={[
              "inline-flex w-fit items-center gap-1 rounded-main px-2 py-1 text-xs-custom font-semibold",
              getRedemptionStatusClassName(request.status),
            ].join(" ")}
          >
            {request.status === "PENDING" && <Clock3 size={13} />}
            {getRedemptionStatusLabel(request.status)}
          </span>
          <span className="font-semibold text-main-dark-gray">
            {request.submittedPhoneNumber}
          </span>
          <span className="text-main-dark-gray/60">
            {formatDateTime(request.requestedAt)}
          </span>
          {request.status === "PENDING" ? (
            <button
              className="flex w-fit items-center gap-1 rounded-main border border-main-light-gray px-3 py-2 text-xs-custom font-semibold text-main-dark-gray/65 transition-colors hover:border-main-blue hover:text-main-blue disabled:text-main-dark-gray/35"
              disabled={cancellingRequestId !== null}
              onClick={() => cancelRedemption(request)}
              type="button"
            >
              {cancellingRequestId === request.requestId ? (
                <Loader2 size={14} className="animate-spin" />
              ) : (
                <RotateCcw size={14} />
              )}
              취소
            </button>
          ) : (
            <span className="text-xs-custom text-main-dark-gray/45">완료</span>
          )}
        </div>
      ))}
    </div>
  );
}

function getRedemptionStatusLabel(status: RewardRedemption["status"]): string {
  if (status === "APPROVED" || status === "SENT") return "승인 완료";
  if (status === "REJECTED" || status === "CANCELLED_REFUNDED") return "반려";
  if (status === "CANCELLED") return "취소";
  return "대기중";
}

function getRedemptionStatusClassName(
  status: RewardRedemption["status"]
): string {
  if (status === "APPROVED" || status === "SENT") {
    return "bg-main-blue/10 text-main-blue";
  }
  if (status === "REJECTED" || status === "CANCELLED_REFUNDED") {
    return "bg-red-50 text-red-600";
  }
  if (status === "CANCELLED") {
    return "bg-main-light-gray text-main-dark-gray/55";
  }
  return "bg-amber-50 text-amber-700";
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "short",
    timeStyle: "short",
    timeZone: "Asia/Seoul",
  }).format(new Date(value));
}
