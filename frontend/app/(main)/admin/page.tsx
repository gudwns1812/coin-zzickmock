import { getAuthUser } from "@/lib/futures-api";
import { getJwtToken } from "@/utils/auth";
import { Package, ShieldAlert, ShieldCheck, TicketCheck } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";

export default async function AdminPage() {
  const [authUser, token] = await Promise.all([getAuthUser(), getJwtToken()]);
  const isAdmin = authUser?.admin ?? token?.admin ?? token?.role === "ADMIN";

  if (!isAdmin) {
    return (
      <div className="px-main-2 pb-24 pt-4">
        <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
          <div className="flex items-start gap-main">
            <div className="flex size-[56px] items-center justify-center rounded-main bg-main-light-gray text-main-dark-gray/55">
              <ShieldAlert size={26} />
            </div>
            <div>
              <p className="text-sm-custom text-main-dark-gray/55">Admin</p>
              <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
                관리자 권한이 필요합니다
              </h1>
              <p className="mt-3 text-sm-custom text-main-dark-gray/65 break-keep">
                관리자 기능은 운영 권한이 있는 계정에서만 사용할 수 있습니다.
              </p>
            </div>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="px-main-2 pb-24 pt-4">
      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-start justify-between gap-main-2">
          <div>
            <p className="text-sm-custom text-main-dark-gray/60">Admin</p>
            <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
              관리자 페이지
            </h1>
          </div>
          <div className="flex size-[72px] items-center justify-center rounded-main bg-main-blue text-white">
            <ShieldCheck size={32} />
          </div>
        </div>
      </section>

      <section className="mt-main-2 grid grid-cols-2 gap-main-2">
        <AdminLink
          href="/admin/reward-redemptions"
          icon={<TicketCheck size={22} />}
          label="교환권 관리"
        />
        <AdminLink
          href="/admin/shop-items"
          icon={<Package size={22} />}
          label="상품 관리"
        />
      </section>
    </div>
  );
}

function AdminLink({
  href,
  icon,
  label,
}: {
  href: string;
  icon: ReactNode;
  label: string;
}) {
  return (
    <Link
      className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm transition-colors hover:border-main-blue"
      href={href}
    >
      <div className="flex items-center gap-2 text-main-blue">
        {icon}
        <span className="text-lg-custom font-bold">{label}</span>
      </div>
    </Link>
  );
}
