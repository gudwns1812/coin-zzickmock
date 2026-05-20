import BackendAuthGate from "@/components/router/BackendAuthGate";
import ProtectedPageSkeleton from "@/components/ui/shared/ProtectedPageSkeleton";
import CommunityListClient from "@/components/router/(main)/community/CommunityListClient";
import type { CommunityCategory } from "@/lib/futures-api";

const COMMUNITY_POST_CATEGORIES: Exclude<CommunityCategory, "NOTICE">[] = [
  "CHART_ANALYSIS",
  "COIN_INFORMATION",
  "CHAT",
];

type CommunityPageProps = {
  searchParams?: Promise<{ category?: string; page?: string; q?: string }>;
};

export default async function CommunityPage({ searchParams }: CommunityPageProps) {
  const resolvedSearchParams = await searchParams;
  const category = parseCategory(resolvedSearchParams?.category);
  const page = parsePage(resolvedSearchParams?.page);
  const searchQuery = parseSearchQuery(resolvedSearchParams?.q);

  return (
    <BackendAuthGate fallback={<ProtectedPageSkeleton variant="community-list" />}>
      <CommunityListClient category={category} page={page} searchQuery={searchQuery} />
    </BackendAuthGate>
  );
}

function parseCategory(value: string | undefined): Exclude<CommunityCategory, "NOTICE"> | null {
  if (value && COMMUNITY_POST_CATEGORIES.includes(value as Exclude<CommunityCategory, "NOTICE">)) {
    return value as Exclude<CommunityCategory, "NOTICE">;
  }
  return null;
}
function parsePage(value: string | undefined): number {
  const page = Number(value);
  return Number.isInteger(page) && page > 0 ? page : 0;
}
function parseSearchQuery(value: string | undefined): string {
  return typeof value === "string" ? value.trim().slice(0, 80) : "";
}
