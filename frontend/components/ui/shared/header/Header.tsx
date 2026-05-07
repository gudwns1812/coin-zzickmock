import React from "react";
import Link from "next/link";
import { getJwtToken } from "@/utils/auth";
import { getFuturesLeaderboard } from "@/lib/futures-api";
import LoginForm from "./LoginForm";
import { cookies } from "next/headers";
import UserInfo from "./UserInfo";
import LogoutForm from "./LogoutForm";
import Navigation from "./Navigation";
import WithdrawalForm from "./WithdrawalForm";

const Header = async () => {
  const token = await getJwtToken();
  const leaderboard = token ? await getFuturesLeaderboard() : null;

  const handleLogout = async () => {
    "use server";

    const cookieStore = await cookies();
    cookieStore.delete("accessToken");
  };

  return (
    <header className="absolute w-full py-3 px-main-2 z-50 backdrop-blur-sm min-w-[1200px]">
      <div className="w-full flex relative gap-5 justify-between items-center">
        <Link
          href="/markets"
          className="flex shrink-0 items-center gap-2.5"
        >
          <img
            src="/favicon.ico"
            alt=""
            className="size-10 rounded-main object-contain"
          />
          <span className="flex flex-col leading-tight">
            <span className="text-lg font-bold text-main-dark-gray">
              코인 찍먹
            </span>
            <span className="text-xs font-medium text-main-dark-gray">
              쉽고 가볍게, 코인 투자
            </span>
          </span>
        </Link>

        <Navigation />

        {!token && <LoginForm />}

        {token && (
          <UserInfo token={token} myRank={leaderboard?.myRank ?? null}>
            <LogoutForm action={handleLogout} />
            <WithdrawalForm action={handleLogout} token={token} />
          </UserInfo>
        )}
      </div>
    </header>
  );
};

export default Header;
