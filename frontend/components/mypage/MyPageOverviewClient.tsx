"use client";

import AccountRefillCard from "@/components/mypage/AccountRefillCard";
import {
  getAccountRefillStatusClient,
  getFuturesAccountSummaryClient,
  getFuturesLeaderboardClient,
  getFuturesPositionsClient,
  getFuturesRewardClient,
} from "@/lib/futures-client-api";
import { futuresQueryKeys } from "@/lib/futures-query-keys";
import { formatMarketRank, formatUsd } from "@/lib/markets";
import { useQuery } from "@tanstack/react-query";
import {
  CalendarDays,
  ClipboardList,
  Mail,
  Phone,
  ShieldCheck,
  Trophy,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";

export default function MyPageOverviewClient() {
  const accountQuery = useQuery({
    queryKey: futuresQueryKeys.account,
    queryFn: getFuturesAccountSummaryClient,
  });
  const positionsQuery = useQuery({
    queryKey: futuresQueryKeys.positions,
    queryFn: () => getFuturesPositionsClient(),
  });
  const rewardQuery = useQuery({
    queryKey: futuresQueryKeys.reward,
    queryFn: getFuturesRewardClient,
  });
  const leaderboardQuery = useQuery({
    queryKey: futuresQueryKeys.leaderboard,
    queryFn: () => getFuturesLeaderboardClient({ limit: 4 }),
  });
  const refillQuery = useQuery({
    queryKey: futuresQueryKeys.refillStatus,
    queryFn: getAccountRefillStatusClient,
  });

  const account = accountQuery.data;
  const positions = positionsQuery.data ?? [];
  const reward = rewardQuery.data;
  const leaderboard = leaderboardQuery.data;
  const refillStatus = refillQuery.data;
  const isLoading =
    accountQuery.isLoading || rewardQuery.isLoading || refillQuery.isLoading;
  const error =
    accountQuery.isError || rewardQuery.isError || refillQuery.isError;

  if (isLoading) {
    return <MyPageState message="계정 정보를 불러오는 중입니다..." />;
  }

  if (error || !account || !reward || !refillStatus) {
    return <MyPageState message="계정 정보를 불러오지 못했습니다. 다시 로그인해 주세요." tone="error" />;
  }

  const openPositionCount = positions.length;

  return (
    <div className="flex flex-col gap-main-2">
      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-start justify-between gap-main-2">
          <div>
            <p className="text-sm-custom text-main-dark-gray/55">Account</p>
            <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
              {account.nickname}
            </h1>
            <div className="mt-3 inline-flex items-center gap-2 rounded-main bg-main-blue/10 px-3 py-1 text-sm-custom font-semibold text-main-blue">
              <Trophy size={15} />
              <span>{formatMarketRank(leaderboard?.myRank?.rank)}</span>
            </div>
          </div>
          <div className="flex size-[72px] items-center justify-center rounded-main bg-main-blue text-white">
            <UserRound size={32} />
          </div>
        </div>

        <div className="mt-main-2 grid grid-cols-3 gap-main">
          <InfoRow icon={<Mail size={17} />} label="이메일" value="-" />
          <InfoRow icon={<Phone size={17} />} label="휴대폰" value="-" />
          <InfoRow icon={<ShieldCheck size={17} />} label="아이디" value={account.account} />
        </div>
      </section>

      <section className="grid grid-cols-1 gap-main md:grid-cols-2 xl:grid-cols-5">
        <Metric label="지갑 잔고" value={formatUsd(account.walletBalance)} />
        <Metric label="사용 가능" value={formatUsd(account.available)} />
        <AccountRefillCard account={account} refillStatus={refillStatus} />
        <Metric label="열린 포지션" value={`${openPositionCount}개`} />
        <Metric label="포인트" value={`${reward.rewardPoint.toLocaleString("ko-KR")} P`} />
      </section>

      <section className="grid grid-cols-3 gap-main-2">
        <QuickLink href="/mypage/assets" icon={<CalendarDays size={20} />} label="Assets" />
        <QuickLink href="/mypage/points" icon={<ShieldCheck size={20} />} label="Point" />
        <QuickLink href="/mypage/redemptions" icon={<ClipboardList size={20} />} label="교환 내역" />
      </section>
    </div>
  );
}

function MyPageState({ message, tone = "empty" }: { message: string; tone?: "empty" | "error" }) {
  return (
    <div className="rounded-main border border-main-light-gray bg-white p-main-2 text-sm-custom text-main-dark-gray/60 shadow-sm">
      <span className={tone === "error" ? "text-main-red" : undefined}>{message}</span>
    </div>
  );
}

function InfoRow({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className="rounded-main bg-main-light-gray/35 p-main">
      <div className="flex items-center gap-2 text-xs-custom font-semibold text-main-dark-gray/50">
        {icon}
        {label}
      </div>
      <p className="mt-2 truncate text-sm-custom font-semibold text-main-dark-gray">{value}</p>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-main border border-main-light-gray bg-white p-main shadow-sm">
      <p className="text-xs-custom text-main-dark-gray/55">{label}</p>
      <p className="mt-2 text-xl-custom font-bold text-main-dark-gray">{value}</p>
    </div>
  );
}

function QuickLink({ href, icon, label }: { href: string; icon: ReactNode; label: string }) {
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
