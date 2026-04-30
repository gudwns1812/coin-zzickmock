import RewardRedemptionHistoryClient from "@/components/rewards/RewardRedemptionHistoryClient";
import { getRewardRedemptions } from "@/lib/futures-api";
import { ShoppingBag } from "lucide-react";
import Link from "next/link";

export default async function MyPageRedemptionsPage() {
  const redemptions = await getRewardRedemptions();

  return (
    <div className="flex flex-col gap-main-2">
      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-start justify-between gap-main-2">
          <div>
            <p className="text-sm-custom text-main-dark-gray/55">
              Reward Exchange
            </p>
            <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
              교환 내역
            </h1>
          </div>
          <Link
            className="flex items-center gap-2 rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white"
            href="/shop"
          >
            <ShoppingBag size={16} />
            상점으로
          </Link>
        </div>
      </section>

      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="mb-main flex items-center justify-between gap-main">
          <h2 className="text-xl-custom font-bold text-main-dark-gray">
            구매/교환 내역
          </h2>
          <span className="rounded-main bg-main-light-gray/55 px-3 py-1 text-xs-custom font-semibold text-main-dark-gray/60">
            최근 신청순
          </span>
        </div>
        <RewardRedemptionHistoryClient redemptions={redemptions} />
      </section>
    </div>
  );
}
