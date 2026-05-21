import BackendAuthGate from "@/components/router/BackendAuthGate";
import ShopRedemptionClient from "@/components/rewards/ShopRedemptionClient";

export default async function ShopPage() {
  return (
    <BackendAuthGate>
      <ShopRedemptionClient />
    </BackendAuthGate>
  );
}
