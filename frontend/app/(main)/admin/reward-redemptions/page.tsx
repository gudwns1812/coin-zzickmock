import AdminRewardRedemptionsClient from "@/components/rewards/AdminRewardRedemptionsClient";
import type { RewardRedemptionStatus } from "@/lib/futures-api";

const REDEMPTION_STATUSES: RewardRedemptionStatus[] = [
  "PENDING",
  "APPROVED",
  "REJECTED",
  "CANCELLED",
];

type Props = {
  searchParams?: Promise<{ status?: string }>;
};

export default async function AdminRewardRedemptionsPage({ searchParams }: Props) {
  const resolvedSearchParams = await searchParams;
  const status = parseStatus(resolvedSearchParams?.status);
  return <AdminRewardRedemptionsClient status={status} />;
}

function parseStatus(value: string | undefined): RewardRedemptionStatus {
  if (value === "SENT") return "APPROVED";
  if (value === "CANCELLED_REFUNDED") return "REJECTED";
  if (value && REDEMPTION_STATUSES.includes(value as RewardRedemptionStatus)) {
    return value as RewardRedemptionStatus;
  }
  return "PENDING";
}
