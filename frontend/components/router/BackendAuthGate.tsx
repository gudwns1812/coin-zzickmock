"use client";

import AppLoadingScreen from "@/components/ui/shared/AppLoadingScreen";
import PageReveal from "@/components/ui/shared/PageReveal";
import { useFuturesAuthUser } from "@/hooks/useFuturesAuthUser";
import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

export default function BackendAuthGate({
  children,
  fallback,
  requireAdmin = false,
}: {
  children: ReactNode;
  fallback?: ReactNode;
  requireAdmin?: boolean;
}) {
  const router = useRouter();
  const authQuery = useFuturesAuthUser();
  const authUser = authQuery.data ?? null;
  const isChecking = authQuery.isLoading;
  const isAllowed = requireAdmin ? authUser?.admin === true : Boolean(authUser);

  useEffect(() => {
    if (!isChecking && !isAllowed) {
      router.replace("/login");
    }
  }, [isAllowed, isChecking, router]);

  if (isChecking) {
    return (
      fallback ?? (
        <AppLoadingScreen
          title="로그인 상태를 확인하고 있습니다"
          description="보호된 화면을 열기 전에 계정 권한을 확인하고 있습니다."
        />
      )
    );
  }

  if (!isAllowed) {
    return (
      <div className="px-main-2 pb-24 pt-4">
        <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
          <p className="text-sm-custom font-semibold text-main-dark-gray/60">
            로그인이 필요합니다.
          </p>
        </section>
      </div>
    );
  }

  return <PageReveal>{children}</PageReveal>;
}
