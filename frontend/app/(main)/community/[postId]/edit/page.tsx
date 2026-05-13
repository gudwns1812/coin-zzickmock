import CommunityPostEditorClient from "@/components/router/(main)/community/CommunityPostEditorClient";
import CommunityState from "@/components/router/(main)/community/CommunityState";
import { getCommunityPost } from "@/lib/futures-api";
import { getJwtToken } from "@/utils/auth";
import { notFound, redirect } from "next/navigation";

type CommunityEditPageProps = {
  params: Promise<{
    postId: string;
  }>;
};

export default async function CommunityEditPage({ params }: CommunityEditPageProps) {
  const token = await getJwtToken();
  if (!token) {
    redirect("/login");
  }

  const { postId: rawPostId } = await params;
  const postId = Number(rawPostId);
  if (!Number.isInteger(postId) || postId <= 0) {
    notFound();
  }

  const result = await getCommunityPost(postId);
  if (result.unavailable || !result.data) {
    return (
      <div className="mx-auto w-full max-w-[1000px] px-main-3 pb-24 pt-4">
        <CommunityState
          actionHref="/community"
          actionLabel="목록으로 돌아가기"
          message={result.message ?? "삭제되었거나 접근할 수 없는 게시글입니다."}
          title="게시글을 불러오지 못했습니다"
          tone="error"
        />
      </div>
    );
  }

  if (!result.data.canEdit) {
    return (
      <div className="mx-auto w-full max-w-[1000px] px-main-3 pb-24 pt-4">
        <CommunityState
          actionHref={`/community/${postId}`}
          actionLabel="게시글로 돌아가기"
          message="작성자 또는 관리자만 수정할 수 있습니다."
          title="수정 권한이 없습니다"
          tone="error"
        />
      </div>
    );
  }

  return (
    <CommunityPostEditorClient
      isAdmin={Boolean(token.admin || token.role === "ADMIN")}
      mode="edit"
      post={result.data}
    />
  );
}
