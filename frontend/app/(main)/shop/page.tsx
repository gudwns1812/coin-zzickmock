import {
  getFuturesReward,
  getShopItems,
} from "@/lib/futures-api";
import ShopRedemptionClient from "@/components/rewards/ShopRedemptionClient";

export default async function ShopPage() {
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
