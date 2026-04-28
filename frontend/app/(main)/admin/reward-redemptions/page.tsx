import AdminRewardRedemptionsClient from "@/components/rewards/AdminRewardRedemptionsClient";
import {
  getAdminRewardRedemptions,
  type RewardRedemptionStatus,
} from "@/lib/futures-api";

const REDEMPTION_STATUSES: RewardRedemptionStatus[] = [
  "PENDING",
  "SENT",
  "CANCELLED_REFUNDED",
];

type Props = {
  searchParams?: Promise<{
    status?: string;
  }>;
};

export default async function AdminRewardRedemptionsPage({
  searchParams,
}: Props) {
  const resolvedSearchParams = await searchParams;
  const status = parseStatus(resolvedSearchParams?.status);
  const result = await getAdminRewardRedemptions(status);

  return (
    <AdminRewardRedemptionsClient
      message={result.message}
      redemptions={result.redemptions}
      status={status}
      unavailable={result.unavailable}
    />
  );
}

function parseStatus(value: string | undefined): RewardRedemptionStatus {
  if (
    value &&
    REDEMPTION_STATUSES.includes(value as RewardRedemptionStatus)
  ) {
    return value as RewardRedemptionStatus;
  }

  return "PENDING";
}
