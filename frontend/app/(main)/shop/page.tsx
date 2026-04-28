import {
  getFuturesReward,
  getRewardRedemptions,
  getShopItems,
} from "@/lib/futures-api";
import ShopRedemptionClient from "@/components/rewards/ShopRedemptionClient";

export default async function ShopPage() {
  const [reward, shopItems, redemptions] = await Promise.all([
    getFuturesReward(),
    getShopItems(),
    getRewardRedemptions(),
  ]);

  return (
    <ShopRedemptionClient
      redemptions={redemptions}
      reward={reward}
      shopItems={shopItems}
    />
  );
}
