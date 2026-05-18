"use client";

import {
  FUTURES_AUTH_CHANGED_EVENT,
  getFuturesAuthUserClient,
  getFuturesLeaderboardClient,
} from "@/lib/futures-auth-state";
import type { AuthUser } from "@/lib/futures-api";
import type { MarketRankingMemberRank } from "@/lib/markets";
import { useCallback, useEffect, useState } from "react";
import LoginForm from "./LoginForm";
import LogoutForm from "./LogoutForm";
import UserInfo from "./UserInfo";
import WithdrawalForm from "./WithdrawalForm";

export default function HeaderAuthControls() {
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);
  const [myRank, setMyRank] = useState<MarketRankingMemberRank | null>(null);
  const [isResolved, setIsResolved] = useState(false);

  const refreshAuthState = useCallback(async () => {
    const nextAuthUser = await getFuturesAuthUserClient();
    setAuthUser(nextAuthUser);

    if (!nextAuthUser) {
      setMyRank(null);
      setIsResolved(true);
      return;
    }

    const leaderboard = await getFuturesLeaderboardClient();
    setMyRank(leaderboard?.myRank ?? null);
    setIsResolved(true);
  }, []);

  useEffect(() => {
    void refreshAuthState();
    window.addEventListener(FUTURES_AUTH_CHANGED_EVENT, refreshAuthState);

    return () => {
      window.removeEventListener(FUTURES_AUTH_CHANGED_EVENT, refreshAuthState);
    };
  }, [refreshAuthState]);

  if (!isResolved) {
    return <div className="h-10 w-[86px]" aria-hidden="true" />;
  }

  if (!authUser) {
    return <LoginForm />;
  }

  return (
    <UserInfo token={authUser} myRank={myRank}>
      <LogoutForm onLoggedOut={() => setAuthUser(null)} />
      <WithdrawalForm onWithdrawn={() => setAuthUser(null)} token={authUser} />
    </UserInfo>
  );
}
