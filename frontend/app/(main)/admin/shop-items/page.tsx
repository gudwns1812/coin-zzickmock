import AdminShopItemsClient from "@/components/rewards/AdminShopItemsClient";
import { getAdminShopItems } from "@/lib/futures-api";

export default async function AdminShopItemsPage() {
  const result = await getAdminShopItems().catch((error) => ({
    items: [],
    unavailable: true,
    message:
      error instanceof Error
        ? error.message
        : "상품 목록을 불러오지 못했습니다.",
  }));

  return (
    <AdminShopItemsClient
      items={result.items}
      message={result.message}
      unavailable={result.unavailable}
    />
  );
}
