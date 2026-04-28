"use client";

import Modal from "@/components/ui/Modal";
import { createRewardRedemption } from "@/lib/futures-client-api";
import type { FuturesReward, ShopItem } from "@/lib/futures-api";
import {
  canRedeemShopItem,
  getShopItemAvailabilityLabel,
  isShopItemLimitReached,
  isShopItemSoldOut,
  normalizeVoucherPhoneNumber,
  validateVoucherPhoneNumber,
} from "@/lib/reward-shop-ui";
import { CheckCircle2, Coffee, Loader2, Lock, Phone, Send } from "lucide-react";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { toast } from "react-toastify";

type Props = {
  reward: FuturesReward;
  shopItems: ShopItem[];
};

export default function ShopRedemptionClient({ reward, shopItems }: Props) {
  const router = useRouter();
  const [selectedItem, setSelectedItem] = useState<ShopItem | null>(null);
  const [phoneNumber, setPhoneNumber] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [lastRequestId, setLastRequestId] = useState<string | null>(null);
  const phoneError = useMemo(
    () => (phoneNumber ? validateVoucherPhoneNumber(phoneNumber) : null),
    [phoneNumber]
  );

  const openModal = (item: ShopItem) => {
    if (!canRedeemShopItem(item, reward.rewardPoint)) {
      toast.error(getDisabledReason(item, reward.rewardPoint));
      return;
    }

    setSelectedItem(item);
    setPhoneNumber("");
    setLastRequestId(null);
  };

  const closeModal = () => {
    if (isSubmitting) return;
    setSelectedItem(null);
  };

  const submitRedemption = async () => {
    if (!selectedItem) return;

    const validationError = validateVoucherPhoneNumber(phoneNumber);
    if (validationError) {
      toast.error(validationError);
      return;
    }

    setIsSubmitting(true);

    try {
      const redemption = await createRewardRedemption(
        selectedItem.code,
        normalizeVoucherPhoneNumber(phoneNumber)
      );
      setLastRequestId(redemption.requestId);
      toast.success("교환권 신청이 접수되었습니다.");
      router.refresh();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "교환권 신청에 실패했습니다."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="px-main-2 pb-24 flex flex-col gap-8 pt-4">
      <section className="grid grid-cols-[1.2fr_1fr] gap-main-2">
        <div className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray">
          <p className="text-sm-custom text-main-dark-gray/60">Reward Shop</p>
          <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
            포인트 상점
          </h1>
          <p className="mt-3 text-sm-custom text-main-dark-gray/70 break-keep">
            실현 손익으로 모은 포인트를 교환권으로 바꾸는 공간입니다.
            신청하면 포인트가 즉시 차감되고 관리자가 발송 상태를 처리합니다.
          </p>
        </div>

        <div className="rounded-main bg-main-blue text-white p-main-2 shadow-sm">
          <p className="text-sm-custom text-white/75">현재 포인트</p>
          <h2 className="mt-3 text-4xl-custom font-bold">
            {reward.rewardPoint.toLocaleString("ko-KR")} P
          </h2>
          <p className="mt-3 text-sm-custom text-white/80 break-keep">
            포인트는 교환권 신청 시점에 바로 차감됩니다.
          </p>
        </div>
      </section>

      <section className="grid grid-cols-3 gap-main-2">
        {shopItems.map((item) => {
          const disabled = !canRedeemShopItem(item, reward.rewardPoint);

          return (
            <article
              key={item.code}
              className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray flex min-h-[320px] flex-col gap-4"
            >
              <div className="flex h-[140px] items-center justify-center rounded-main bg-main-light-gray/45 text-main-blue">
                <Coffee size={52} strokeWidth={1.8} />
              </div>
              <div>
                <div className="flex items-center justify-between gap-main">
                  <h2 className="text-xl-custom font-semibold text-main-dark-gray">
                    {item.name}
                  </h2>
                  <span className="rounded-main bg-main-light-gray/55 px-3 py-1 text-xs-custom font-semibold text-main-dark-gray/70">
                    {getShopItemAvailabilityLabel(item)}
                  </span>
                </div>
                <p className="mt-2 text-sm-custom text-main-dark-gray/60 break-keep">
                  {item.description}
                </p>
              </div>
              <div className="grid grid-cols-2 gap-2 text-xs-custom text-main-dark-gray/55">
                <span>판매 {item.soldQuantity.toLocaleString("ko-KR")}개</span>
                <span>
                  {item.perMemberPurchaseLimit === null
                    ? "개인 제한 없음"
                    : `남은 구매 ${item.remainingPurchaseLimit ?? 0}회`}
                </span>
              </div>
              <div className="mt-auto flex items-center justify-between">
                <span className="text-lg-custom font-bold text-main-blue">
                  {item.price.toLocaleString("ko-KR")} P
                </span>
                <button
                  className={[
                    "flex items-center gap-2 rounded-main px-main py-2 font-semibold transition-colors",
                    disabled
                      ? "bg-main-light-gray text-main-dark-gray/40"
                      : "bg-main-blue text-white hover:bg-main-blue/90",
                  ].join(" ")}
                  disabled={disabled}
                  onClick={() => openModal(item)}
                  type="button"
                >
                  {disabled ? <Lock size={16} /> : <Send size={16} />}
                  {getButtonLabel(item, reward.rewardPoint)}
                </button>
              </div>
            </article>
          );
        })}
      </section>

      <Modal
        hasBackdropBlur={false}
        isEscapeClose
        isOpen={selectedItem !== null}
        onClose={closeModal}
      >
        <div className="w-[min(460px,calc(100vw-48px))] pr-6">
          <p className="text-lg-custom font-bold text-main-dark-gray">
            커피 교환권 신청
          </p>
          <p className="mt-2 text-sm-custom text-main-dark-gray/60 break-keep">
            {selectedItem?.name} · {selectedItem?.price.toLocaleString("ko-KR")} P
          </p>

          <label className="mt-5 block">
            <span className="text-xs-custom font-semibold text-main-dark-gray/60">
              휴대폰 번호
            </span>
            <div className="mt-2 flex items-center gap-2 rounded-main border border-main-light-gray px-main py-3">
              <Phone size={16} className="text-main-dark-gray/45" />
              <input
                className="min-w-0 flex-1 bg-transparent text-sm-custom font-bold text-main-dark-gray outline-none"
                inputMode="tel"
                onChange={(event) => setPhoneNumber(event.target.value)}
                placeholder="010-1234-5678"
                value={phoneNumber}
              />
            </div>
          </label>

          {phoneError && (
            <p className="mt-2 text-xs-custom font-semibold text-red-500">
              {phoneError}
            </p>
          )}

          {lastRequestId && (
            <div className="mt-4 flex items-start gap-2 rounded-main bg-main-light-gray/50 p-main text-sm-custom text-main-dark-gray/70">
              <CheckCircle2 size={18} className="mt-0.5 text-main-blue" />
              <span>신청번호 {lastRequestId} 접수 완료</span>
            </div>
          )}

          <div className="mt-6 flex justify-end gap-2">
            <button
              className="rounded-main border border-main-light-gray px-main py-2 text-sm-custom font-semibold text-main-dark-gray/70"
              disabled={isSubmitting}
              onClick={closeModal}
              type="button"
            >
              닫기
            </button>
            <button
              className="flex items-center gap-2 rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white disabled:bg-main-light-gray disabled:text-main-dark-gray/40"
              disabled={isSubmitting || !!phoneError || !phoneNumber}
              onClick={submitRedemption}
              type="button"
            >
              {isSubmitting && <Loader2 size={16} className="animate-spin" />}
              신청하기
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

function getButtonLabel(item: ShopItem, rewardPoint: number): string {
  if (!item.active || isShopItemSoldOut(item) || isShopItemLimitReached(item)) {
    return getShopItemAvailabilityLabel(item);
  }

  if (rewardPoint < item.price) {
    return "포인트 부족";
  }

  return "구매";
}

function getDisabledReason(item: ShopItem, rewardPoint: number): string {
  if (!item.active || isShopItemSoldOut(item) || isShopItemLimitReached(item)) {
    return getShopItemAvailabilityLabel(item);
  }

  return rewardPoint < item.price
    ? "보유 포인트가 부족합니다."
    : getShopItemAvailabilityLabel(item);
}
