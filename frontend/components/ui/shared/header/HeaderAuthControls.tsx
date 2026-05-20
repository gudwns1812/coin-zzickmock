"use client";

import { useFuturesAuthUser } from "@/hooks/useFuturesAuthUser";
import { getFuturesLeaderboardClient } from "@/lib/futures-client-api";
import { futuresQueryKeys } from "@/lib/futures-query-keys";
import { useQuery } from "@tanstack/react-query";
import LoginForm from "./LoginForm";
import LogoutForm from "./LogoutForm";
import UserInfo from "./UserInfo";
import WithdrawalForm from "./WithdrawalForm";

export default function HeaderAuthControls() {
  const authQuery = useFuturesAuthUser();
  const authUser = authQuery.data ?? null;
  const leaderboardQuery = useQuery({
    queryKey: [...futuresQueryKeys.leaderboard, "me"],
    queryFn: () => getFuturesLeaderboardClient({ limit: 4 }),
    enabled: Boolean(authUser),
  });

  if (authQuery.isLoading) {
    return <div className="h-10 w-[86px]" aria-hidden="true" />;
  }

  if (!authUser) {
    return <LoginForm />;
  }

  return (
    <UserInfo token={authUser} myRank={leaderboardQuery.data?.myRank ?? null}>
      <LogoutForm onLoggedOut={() => undefined} />
      <WithdrawalForm onWithdrawn={() => undefined} token={authUser} />
    </UserInfo>
  );
}
