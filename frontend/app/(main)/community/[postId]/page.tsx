import BackendAuthGate from "@/components/router/BackendAuthGate";
import CommunityDetailClient from "@/components/router/(main)/community/CommunityDetailClient";
import { notFound } from "next/navigation";

type CommunityDetailPageProps = { params: Promise<{ postId: string }> };

export default async function CommunityDetailPage({ params }: CommunityDetailPageProps) {
  const { postId: rawPostId } = await params;
  const postId = Number(rawPostId);
  if (!Number.isInteger(postId) || postId <= 0) notFound();
  return (
    <BackendAuthGate>
      <CommunityDetailClient postId={postId} />
    </BackendAuthGate>
  );
}
