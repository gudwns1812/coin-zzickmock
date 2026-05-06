import {
  getFuturesReward,
  getShopItems,
} from "@/lib/futures-api";
import ShopRedemptionClient from "@/components/rewards/ShopRedemptionClient";
import { getJwtToken } from "@/utils/auth";
import { redirect } from "next/navigation";

export default async function ShopPage() {
  const token = await getJwtToken();
  if (!token) {
    redirect("/login");
  }

  const [reward, shopItems] = await Promise.all([
    getFuturesReward(),
    getShopItems(),
  ]);

  return (
    <ShopRedemptionClient
      reward={reward}
      shopItems={shopItems}
    />
  );
}
