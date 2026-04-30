"use client";

import {
  deactivateAdminShopItem,
  createAdminShopItem,
  updateAdminShopItem,
} from "@/lib/futures-client-api";
import type { AdminShopItem } from "@/lib/futures-api";
import {
  EMPTY_ADMIN_SHOP_ITEM_FORM,
  formFromAdminShopItem,
  toAdminShopItemInput,
  type AdminShopItemForm,
} from "@/lib/admin-shop-item-ui";
import clsx from "clsx";
import {
  ArrowLeft,
  Edit3,
  Loader2,
  PackagePlus,
  Power,
  Save,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useState } from "react";
import { toast } from "react-toastify";

type Props = {
  items: AdminShopItem[];
  unavailable: boolean;
  message: string | null;
};

export default function AdminShopItemsClient({
  items,
  unavailable,
  message,
}: Props) {
  const router = useRouter();
  const [editingCode, setEditingCode] = useState<string | null>(null);
  const [form, setForm] = useState<AdminShopItemForm>(
    EMPTY_ADMIN_SHOP_ITEM_FORM
  );
  const [pendingAction, setPendingAction] = useState<string | null>(null);
  const isEditing = editingCode !== null;

  const startCreate = () => {
    setEditingCode(null);
    setForm(EMPTY_ADMIN_SHOP_ITEM_FORM);
  };

  const startEdit = (item: AdminShopItem) => {
    setEditingCode(item.code);
    setForm(formFromAdminShopItem(item));
  };

  const updateForm = <K extends keyof AdminShopItemForm>(
    key: K,
    value: AdminShopItemForm[K]
  ) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const submit = async () => {
    const { input, error } = toAdminShopItemInput(
      form,
      isEditing ? "edit" : "create"
    );
    if (error || !input) {
      toast.error(error ?? "상품 정보를 확인해주세요.");
      return;
    }

    setPendingAction("save");
    try {
      if (isEditing && editingCode) {
        await updateAdminShopItem(editingCode, input);
        toast.success("상품을 수정했습니다.");
      } else {
        await createAdminShopItem(input);
        toast.success("상품을 생성했습니다.");
      }
      startCreate();
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "상품 저장 실패");
    } finally {
      setPendingAction(null);
    }
  };

  const deactivate = async (item: AdminShopItem) => {
    setPendingAction(`deactivate:${item.code}`);
    try {
      await deactivateAdminShopItem(item.code);
      toast.success("상품 판매를 중지했습니다.");
      if (editingCode === item.code) {
        startCreate();
      }
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "상품 판매 중지 실패");
    } finally {
      setPendingAction(null);
    }
  };

  return (
    <div className="px-main-2 pb-24 pt-4">
      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-start justify-between gap-main-2">
          <div>
            <p className="text-sm-custom text-main-dark-gray/60">Admin</p>
            <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
              상점 상품 관리
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <Link
              className="flex items-center gap-2 rounded-main bg-main-light-gray px-main py-2 text-sm-custom font-semibold text-main-dark-gray/70 hover:text-main-blue"
              href="/admin"
            >
              <ArrowLeft size={15} />
              관리자 홈
            </Link>
            <Link
              className="rounded-main bg-main-light-gray px-main py-2 text-sm-custom font-semibold text-main-dark-gray/70 hover:text-main-blue"
              href="/admin/reward-redemptions"
            >
              교환권 신청
            </Link>
          </div>
        </div>
      </section>

      {unavailable ? (
        <div className="mt-main-2 rounded-main border border-main-light-gray bg-white p-main-2 text-main-dark-gray/70">
          {message ?? "관리자 권한이 필요하거나 상품을 불러오지 못했습니다."}
        </div>
      ) : (
        <div className="mt-main-2 grid grid-cols-[1.35fr_0.9fr] gap-main-2">
          <section className="overflow-hidden rounded-main border border-main-light-gray bg-white shadow-sm">
            <div className="grid grid-cols-[1.1fr_0.9fr_0.8fr_0.8fr_0.7fr_0.8fr] gap-main border-b border-main-light-gray bg-main-light-gray/35 px-main py-3 text-xs-custom font-semibold text-main-dark-gray/55">
              <span>상품</span>
              <span>타입</span>
              <span>가격</span>
              <span>재고</span>
              <span>상태</span>
              <span>관리</span>
            </div>

            {items.length === 0 ? (
              <div className="px-main py-main-2 text-sm-custom text-main-dark-gray/55">
                등록된 상품이 없습니다.
              </div>
            ) : (
              items.map((item) => (
                <div
                  className="grid grid-cols-[1.1fr_0.9fr_0.8fr_0.8fr_0.7fr_0.8fr] gap-main border-b border-main-light-gray px-main py-4 text-sm-custom last:border-b-0"
                  key={item.code}
                >
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-main-dark-gray">
                      {item.name}
                    </p>
                    <p className="mt-1 truncate text-xs-custom text-main-dark-gray/50">
                      {item.code}
                    </p>
                  </div>
                  <span className="truncate text-main-dark-gray/65">
                    {item.itemType}
                  </span>
                  <span className="font-semibold text-main-blue">
                    {item.price.toLocaleString("ko-KR")} P
                  </span>
                  <span className="text-main-dark-gray/65">
                    {formatStock(item)}
                  </span>
                  <span
                    className={clsx(
                      "w-fit rounded-main px-2 py-1 text-xs-custom font-semibold",
                      item.active
                        ? "bg-main-blue/10 text-main-blue"
                        : "bg-main-light-gray text-main-dark-gray/50"
                    )}
                  >
                    {item.active ? "판매" : "중지"}
                  </span>
                  <div className="flex gap-2">
                    <IconButton
                      icon={<Edit3 size={15} />}
                      label="수정"
                      onClick={() => startEdit(item)}
                    />
                    <IconButton
                      disabled={!item.active || pendingAction !== null}
                      icon={
                        pendingAction === `deactivate:${item.code}` ? (
                          <Loader2 size={15} className="animate-spin" />
                        ) : (
                          <Power size={15} />
                        )
                      }
                      label="중지"
                      onClick={() => deactivate(item)}
                    />
                  </div>
                </div>
              ))
            )}
          </section>

          <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
            <div className="flex items-center justify-between gap-main">
              <h2 className="text-xl-custom font-bold text-main-dark-gray">
                {isEditing ? "상품 수정" : "상품 생성"}
              </h2>
              <button
                className="rounded-main bg-main-light-gray px-3 py-2 text-xs-custom font-semibold text-main-dark-gray/60"
                onClick={startCreate}
                type="button"
              >
                신규
              </button>
            </div>

            <div className="mt-main grid gap-3">
              <TextInput
                disabled={isEditing}
                label="코드"
                onChange={(value) => updateForm("code", value)}
                placeholder="voucher.coffee"
                value={form.code}
              />
              <TextInput
                label="상품명"
                onChange={(value) => updateForm("name", value)}
                placeholder="커피 교환권"
                value={form.name}
              />
              <TextInput
                label="설명"
                onChange={(value) => updateForm("description", value)}
                placeholder="상품 설명"
                value={form.description}
              />
              <TextInput
                label="타입"
                onChange={(value) => updateForm("itemType", value)}
                placeholder="COFFEE_VOUCHER"
                value={form.itemType}
              />
              <div className="grid grid-cols-2 gap-3">
                <TextInput
                  inputMode="numeric"
                  label="가격"
                  onChange={(value) => updateForm("price", value)}
                  placeholder="100"
                  value={form.price}
                />
                <TextInput
                  inputMode="numeric"
                  label="정렬"
                  onChange={(value) => updateForm("sortOrder", value)}
                  placeholder="10"
                  value={form.sortOrder}
                />
                <TextInput
                  inputMode="numeric"
                  label="총 재고"
                  onChange={(value) => updateForm("totalStock", value)}
                  placeholder="비우면 무제한"
                  value={form.totalStock}
                />
                <TextInput
                  inputMode="numeric"
                  label="개인 제한"
                  onChange={(value) =>
                    updateForm("perMemberPurchaseLimit", value)
                  }
                  placeholder="비우면 없음"
                  value={form.perMemberPurchaseLimit}
                />
              </div>
              <label className="flex items-center gap-2 text-sm-custom font-semibold text-main-dark-gray/70">
                <input
                  checked={form.active}
                  className="size-4 accent-main-blue"
                  onChange={(event) => updateForm("active", event.target.checked)}
                  type="checkbox"
                />
                판매 활성화
              </label>
              <button
                className="mt-2 flex items-center justify-center gap-2 rounded-main bg-main-blue px-main py-3 text-sm-custom font-semibold text-white disabled:bg-main-light-gray disabled:text-main-dark-gray/40"
                disabled={pendingAction !== null}
                onClick={submit}
                type="button"
              >
                {pendingAction === "save" ? (
                  <Loader2 size={16} className="animate-spin" />
                ) : isEditing ? (
                  <Save size={16} />
                ) : (
                  <PackagePlus size={16} />
                )}
                {isEditing ? "수정" : "생성"}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

function TextInput({
  disabled,
  inputMode,
  label,
  onChange,
  placeholder,
  value,
}: {
  disabled?: boolean;
  inputMode?: "numeric";
  label: string;
  onChange: (value: string) => void;
  placeholder: string;
  value: string;
}) {
  return (
    <label className="block">
      <span className="text-xs-custom font-semibold text-main-dark-gray/55">
        {label}
      </span>
      <input
        className="mt-1 w-full rounded-main border border-main-light-gray px-3 py-2 text-sm-custom font-semibold text-main-dark-gray outline-none focus:border-main-blue disabled:bg-main-light-gray/40 disabled:text-main-dark-gray/45"
        disabled={disabled}
        inputMode={inputMode}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        value={value}
      />
    </label>
  );
}

function IconButton({
  disabled,
  icon,
  label,
  onClick,
}: {
  disabled?: boolean;
  icon: ReactNode;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      className="flex items-center gap-1 rounded-main bg-main-light-gray px-2 py-2 text-xs-custom font-semibold text-main-dark-gray/65 disabled:text-main-dark-gray/30"
      disabled={disabled}
      onClick={onClick}
      type="button"
    >
      {icon}
      {label}
    </button>
  );
}

function formatStock(item: AdminShopItem): string {
  if (item.totalStock === null) {
    return `${item.soldQuantity.toLocaleString("ko-KR")} / 무제한`;
  }
  return `${item.soldQuantity.toLocaleString("ko-KR")} / ${item.totalStock.toLocaleString("ko-KR")}`;
}
