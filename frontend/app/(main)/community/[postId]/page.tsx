import CommunityDetailView from "@/components/router/(main)/community/CommunityDetailView";
import {
  getCommunityComments,
  getCommunityPost,
} from "@/lib/futures-api";
import { getJwtToken } from "@/utils/auth";
import { notFound, redirect } from "next/navigation";

const COMMENT_PAGE_SIZE = 20;

type CommunityDetailPageProps = {
  params: Promise<{
    postId: string;
  }>;
};

export default async function CommunityDetailPage({
  params,
}: CommunityDetailPageProps) {
  const token = await getJwtToken();
  if (!token) {
    redirect("/login");
  }

  const { postId: rawPostId } = await params;
  const postId = Number(rawPostId);
  if (!Number.isInteger(postId) || postId <= 0) {
    notFound();
  }

  const [postResult, commentsResult] = await Promise.all([
    getCommunityPost(postId),
    getCommunityComments(postId, { page: 0, size: COMMENT_PAGE_SIZE }),
  ]);

  return (
    <CommunityDetailView
      comments={commentsResult.data}
      message={postResult.message}
      post={postResult.data}
      unavailable={postResult.unavailable}
    />
  );
}
