import BackendAuthGate from "@/components/router/BackendAuthGate";
import ShopRedemptionClient from "@/components/rewards/ShopRedemptionClient";
import ProtectedPageSkeleton from "@/components/ui/shared/ProtectedPageSkeleton";

export default async function ShopPage() {
  return (
    <BackendAuthGate fallback={<ProtectedPageSkeleton variant="shop" />}>
      <ShopRedemptionClient />
    </BackendAuthGate>
  );
}
