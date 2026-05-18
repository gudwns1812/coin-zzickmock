"use client";

import CommunityDetailView from "@/components/router/(main)/community/CommunityDetailView";
import { getCommunityCommentsClient, getCommunityPostClient } from "@/lib/futures-client-api";
import { futuresQueryKeys } from "@/lib/futures-query-keys";
import { useQuery } from "@tanstack/react-query";

const COMMENT_PAGE_SIZE = 20;

export default function CommunityDetailClient({ postId }: { postId: number }) {
  const postQuery = useQuery({
    queryKey: [...futuresQueryKeys.community, postId],
    queryFn: () => getCommunityPostClient(postId),
  });
  const commentsQuery = useQuery({
    queryKey: [...futuresQueryKeys.community, postId, "comments"],
    queryFn: () => getCommunityCommentsClient(postId, { page: 0, size: COMMENT_PAGE_SIZE }),
    enabled: !postQuery.data?.unavailable,
  });
  const postResult = postQuery.data;
  const commentsResult = commentsQuery.data;

  return (
    <CommunityDetailView
      comments={commentsResult?.data ?? null}
      message={postResult?.message ?? (postQuery.isError ? "게시글을 불러오지 못했습니다." : null)}
      post={postResult?.data ?? null}
      unavailable={postQuery.isLoading ? false : postResult?.unavailable ?? postQuery.isError}
      isLoading={postQuery.isLoading}
    />
  );
}
