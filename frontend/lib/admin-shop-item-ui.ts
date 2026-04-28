import type { AdminShopItem, AdminShopItemInput } from "@/lib/futures-api";

export type AdminShopItemForm = {
  code: string;
  name: string;
  description: string;
  itemType: string;
  price: string;
  active: boolean;
  totalStock: string;
  perMemberPurchaseLimit: string;
  sortOrder: string;
};

export const EMPTY_ADMIN_SHOP_ITEM_FORM: AdminShopItemForm = {
  code: "",
  name: "",
  description: "",
  itemType: "COFFEE_VOUCHER",
  price: "",
  active: true,
  totalStock: "",
  perMemberPurchaseLimit: "",
  sortOrder: "0",
};

export function formFromAdminShopItem(item: AdminShopItem): AdminShopItemForm {
  return {
    code: item.code,
    name: item.name,
    description: item.description,
    itemType: item.itemType,
    price: String(item.price),
    active: item.active,
    totalStock: item.totalStock === null ? "" : String(item.totalStock),
    perMemberPurchaseLimit:
      item.perMemberPurchaseLimit === null
        ? ""
        : String(item.perMemberPurchaseLimit),
    sortOrder: String(item.sortOrder),
  };
}

export function toAdminShopItemInput(
  form: AdminShopItemForm,
  mode: "create" | "edit"
): { input: AdminShopItemInput | null; error: string | null } {
  const code = form.code.trim();
  const name = form.name.trim();
  const description = form.description.trim();
  const itemType = form.itemType.trim();

  if (mode === "create" && !code) {
    return failure("상품 코드를 입력해주세요.");
  }
  if (!name) {
    return failure("상품명을 입력해주세요.");
  }
  if (!description) {
    return failure("설명을 입력해주세요.");
  }
  if (!itemType) {
    return failure("상품 타입을 입력해주세요.");
  }

  const price = parseRequiredNonNegativeInt(form.price, "가격");
  if (typeof price === "string") {
    return failure(price);
  }
  if (price <= 0) {
    return failure("가격은 0보다 커야 합니다.");
  }

  const totalStock = parseOptionalNonNegativeInt(form.totalStock, "총 재고");
  if (typeof totalStock === "string") {
    return failure(totalStock);
  }

  const perMemberPurchaseLimit = parseOptionalNonNegativeInt(
    form.perMemberPurchaseLimit,
    "개인 구매 제한"
  );
  if (typeof perMemberPurchaseLimit === "string") {
    return failure(perMemberPurchaseLimit);
  }
  if (perMemberPurchaseLimit !== null && perMemberPurchaseLimit <= 0) {
    return failure("개인 구매 제한은 0보다 커야 합니다.");
  }

  const sortOrder = parseRequiredNonNegativeInt(form.sortOrder, "정렬 순서");
  if (typeof sortOrder === "string") {
    return failure(sortOrder);
  }

  return {
    input: {
      code: mode === "create" ? code : null,
      name,
      description,
      itemType,
      price,
      active: form.active,
      totalStock,
      perMemberPurchaseLimit,
      sortOrder,
    },
    error: null,
  };
}

function parseRequiredNonNegativeInt(value: string, label: string): number | string {
  const trimmed = value.trim();
  if (!trimmed) {
    return `${label}을 입력해주세요.`;
  }
  return parseNonNegativeInt(trimmed, label);
}

function parseOptionalNonNegativeInt(
  value: string,
  label: string
): number | string | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  return parseNonNegativeInt(trimmed, label);
}

function parseNonNegativeInt(value: string, label: string): number | string {
  if (!/^[0-9]+$/.test(value)) {
    return `${label}은 0 이상의 정수여야 합니다.`;
  }
  return Number(value);
}

function failure(error: string): { input: null; error: string } {
  return { input: null, error };
}
