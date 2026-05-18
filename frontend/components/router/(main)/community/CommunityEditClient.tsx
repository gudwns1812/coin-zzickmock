"use client";

import CommunityPostEditorClient from "@/components/router/(main)/community/CommunityPostEditorClient";
import CommunityState from "@/components/router/(main)/community/CommunityState";
import { getCommunityPostForEditClient } from "@/lib/futures-client-api";
import { futuresQueryKeys } from "@/lib/futures-query-keys";
import { useQuery } from "@tanstack/react-query";

export default function CommunityEditClient({ postId }: { postId: number }) {
  const postQuery = useQuery({
    queryKey: [...futuresQueryKeys.community, postId, "edit"],
    queryFn: () => getCommunityPostForEditClient(postId),
  });
  const result = postQuery.data;

  if (postQuery.isLoading) {
    return <EditorState title="게시글을 불러오는 중입니다" message="수정 권한과 본문을 확인하고 있습니다." />;
  }
  if (postQuery.isError || result?.unavailable || !result?.data) {
    return <EditorState title="게시글을 불러오지 못했습니다" message={result?.message ?? "삭제되었거나 접근할 수 없는 게시글입니다."} tone="error" />;
  }
  if (!result.data.canEdit) {
    return <EditorState title="수정 권한이 없습니다" message="작성자 또는 관리자만 수정할 수 있습니다." tone="error" actionHref={`/community/${postId}`} actionLabel="게시글로 돌아가기" />;
  }
  return <CommunityPostEditorClient isAdmin={false} mode="edit" post={result.data} />;
}

function EditorState({ title, message, tone, actionHref = "/community", actionLabel = "목록으로 돌아가기" }: { title: string; message: string; tone?: "empty" | "error"; actionHref?: string; actionLabel?: string }) {
  return (
    <div className="mx-auto w-full max-w-[1000px] px-main-3 pb-24 pt-4">
      <CommunityState actionHref={actionHref} actionLabel={actionLabel} message={message} title={title} tone={tone} />
    </div>
  );
}
