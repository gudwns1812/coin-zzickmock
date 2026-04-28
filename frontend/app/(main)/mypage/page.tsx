import {
  getFuturesAccountSummary,
  getFuturesPositions,
  getFuturesReward,
} from "@/lib/futures-api";
import { formatUsd } from "@/lib/markets";
import { getJwtToken } from "@/utils/auth";
import { CalendarDays, Mail, Phone, ShieldCheck, UserRound } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";

export default async function MyPage() {
  const token = await getJwtToken();
  const [account, positions, reward] = await Promise.all([
    getFuturesAccountSummary(),
    getFuturesPositions(),
    getFuturesReward(),
  ]);
  const openPositionCount = positions.length;

  return (
    <div className="flex flex-col gap-main-2">
      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-start justify-between gap-main-2">
          <div>
            <p className="text-sm-custom text-main-dark-gray/55">Account</p>
            <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
              {token?.memberName ?? account.memberName}
            </h1>
            <p className="mt-3 text-sm-custom text-main-dark-gray/65 break-keep">
              데모 선물 계정의 자산, 포인트, 열린 포지션 흐름을 한곳에서
              확인합니다.
            </p>
          </div>
          <div className="flex size-[72px] items-center justify-center rounded-main bg-main-blue text-white">
            <UserRound size={32} />
          </div>
        </div>

        <div className="mt-main-2 grid grid-cols-3 gap-main">
          <InfoRow
            icon={<Mail size={17} />}
            label="이메일"
            value={token?.email ?? "-"}
          />
          <InfoRow
            icon={<Phone size={17} />}
            label="휴대폰"
            value={token?.phoneNumber ?? "-"}
          />
          <InfoRow
            icon={<ShieldCheck size={17} />}
            label="회원 ID"
            value={token?.memberId ?? account.memberId}
          />
        </div>
      </section>

      <section className="grid grid-cols-4 gap-main">
        <Metric label="지갑 잔고" value={formatUsd(account.walletBalance)} />
        <Metric label="사용 가능" value={formatUsd(account.available)} />
        <Metric label="열린 포지션" value={`${openPositionCount}개`} />
        <Metric label="포인트" value={`${reward.rewardPoint.toLocaleString("ko-KR")} P`} />
      </section>

      <section className="grid grid-cols-2 gap-main-2">
        <QuickLink
          href="/mypage/assets"
          icon={<CalendarDays size={20} />}
          label="Assets"
          value="자산과 일별 실현 손익"
        />
        <QuickLink
          href="/mypage/points"
          icon={<ShieldCheck size={20} />}
          label="Point"
          value="보유 포인트와 히스토리"
        />
      </section>
    </div>
  );
}

function InfoRow({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-main bg-main-light-gray/35 p-main">
      <div className="flex items-center gap-2 text-xs-custom font-semibold text-main-dark-gray/50">
        {icon}
        {label}
      </div>
      <p className="mt-2 truncate text-sm-custom font-semibold text-main-dark-gray">
        {value}
      </p>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-main border border-main-light-gray bg-white p-main shadow-sm">
      <p className="text-xs-custom text-main-dark-gray/55">{label}</p>
      <p className="mt-2 text-xl-custom font-bold text-main-dark-gray">
        {value}
      </p>
    </div>
  );
}

function QuickLink({
  href,
  icon,
  label,
  value,
}: {
  href: string;
  icon: ReactNode;
  label: string;
  value: string;
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
      <p className="mt-2 text-sm-custom text-main-dark-gray/60">{value}</p>
    </Link>
  );
}
