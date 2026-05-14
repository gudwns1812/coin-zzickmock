import CommunityListView from "@/components/router/(main)/community/CommunityListView";
import {
  getCommunityPosts,
  type CommunityCategory,
} from "@/lib/futures-api";
import { getJwtToken } from "@/utils/auth";
import { redirect } from "next/navigation";

const COMMUNITY_POST_CATEGORIES: Exclude<CommunityCategory, "NOTICE">[] = [
  "CHART_ANALYSIS",
  "COIN_INFORMATION",
  "CHAT",
];

const COMMUNITY_PAGE_SIZE = 20;

type CommunityPageProps = {
  searchParams?: Promise<{
    category?: string;
    page?: string;
  }>;
};

export default async function CommunityPage({
  searchParams,
}: CommunityPageProps) {
  const token = await getJwtToken();
  if (!token) {
    redirect("/login");
  }

  const resolvedSearchParams = await searchParams;
  const category = parseCategory(resolvedSearchParams?.category);
  const page = parsePage(resolvedSearchParams?.page);
  const result = await getCommunityPosts({
    category,
    page,
    size: COMMUNITY_PAGE_SIZE,
  });

  return (
    <CommunityListView
      category={category}
      message={result.message}
      result={result.data}
      unavailable={result.unavailable}
    />
  );
}

function parseCategory(
  value: string | undefined
): Exclude<CommunityCategory, "NOTICE"> | null {
  if (
    value &&
    COMMUNITY_POST_CATEGORIES.includes(
      value as Exclude<CommunityCategory, "NOTICE">
    )
  ) {
    return value as Exclude<CommunityCategory, "NOTICE">;
  }

  return null;
}

function parsePage(value: string | undefined): number {
  const page = Number(value);
  return Number.isInteger(page) && page > 0 ? page : 0;
}
