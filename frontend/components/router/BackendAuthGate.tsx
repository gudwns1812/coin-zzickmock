"use client";

import {
  FUTURES_AUTH_CHANGED_EVENT,
  getFuturesAuthUserClient,
} from "@/lib/futures-auth-state";
import AppLoadingScreen from "@/components/ui/shared/AppLoadingScreen";
import { useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";

type AuthGateState = "checking" | "allowed" | "denied";

export default function BackendAuthGate({
  children,
  requireAdmin = false,
}: {
  children: ReactNode;
  requireAdmin?: boolean;
}) {
  const router = useRouter();
  const [state, setState] = useState<AuthGateState>("checking");

  useEffect(() => {
    let isActive = true;

    async function checkAuth() {
      setState("checking");
      const authUser = await getFuturesAuthUserClient().catch(() => null);
      const isAllowed = requireAdmin
        ? authUser?.admin === true
        : Boolean(authUser);

      if (!isActive) {
        return;
      }

      if (isAllowed) {
        setState("allowed");
        return;
      }

      setState("denied");
      router.replace("/login");
    }

    const handleAuthChanged = () => {
      void checkAuth();
    };

    void checkAuth();
    window.addEventListener(FUTURES_AUTH_CHANGED_EVENT, handleAuthChanged);

    return () => {
      isActive = false;
      window.removeEventListener(FUTURES_AUTH_CHANGED_EVENT, handleAuthChanged);
    };
  }, [requireAdmin, router]);

  if (state === "checking") {
    return (
      <AppLoadingScreen
        title="로그인 상태를 확인하고 있습니다"
        description="보호된 화면을 열기 전에 계정 권한을 확인하고 있습니다."
      />
    );
  }

  if (state === "denied") {
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

  return children;
}
