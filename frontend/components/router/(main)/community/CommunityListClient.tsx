"use client";

import CommunityListView from "@/components/router/(main)/community/CommunityListView";
import { getCommunityPostsClient } from "@/lib/futures-client-api";
import { futuresQueryKeys } from "@/lib/futures-query-keys";
import type { CommunityCategory } from "@/lib/futures-api";
import { useQuery } from "@tanstack/react-query";

const COMMUNITY_PAGE_SIZE = 20;

type Props = {
  category: Exclude<CommunityCategory, "NOTICE"> | null;
  page: number;
  searchQuery: string;
};

export default function CommunityListClient({ category, page, searchQuery }: Props) {
  const postsQuery = useQuery({
    queryKey: [...futuresQueryKeys.community, "posts", category, page],
    queryFn: () => getCommunityPostsClient({ category, page, size: COMMUNITY_PAGE_SIZE }),
  });
  const result = postsQuery.data;

  return (
    <CommunityListView
      category={category}
      message={result?.message ?? (postsQuery.isError ? "커뮤니티를 불러오지 못했습니다." : null)}
      result={result?.data ?? null}
      searchQuery={searchQuery}
      unavailable={postsQuery.isLoading ? false : result?.unavailable ?? postsQuery.isError}
      isLoading={postsQuery.isLoading}
    />
  );
}
