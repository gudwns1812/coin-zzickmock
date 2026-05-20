import BackendAuthGate from "@/components/router/BackendAuthGate";
import ProtectedPageSkeleton from "@/components/ui/shared/ProtectedPageSkeleton";
import CommunityDetailClient from "@/components/router/(main)/community/CommunityDetailClient";
import { notFound } from "next/navigation";

type CommunityDetailPageProps = { params: Promise<{ postId: string }> };

export default async function CommunityDetailPage({ params }: CommunityDetailPageProps) {
  const { postId: rawPostId } = await params;
  const postId = Number(rawPostId);
  if (!Number.isInteger(postId) || postId <= 0) notFound();
  return (
    <BackendAuthGate fallback={<ProtectedPageSkeleton variant="community-detail" />}>
      <CommunityDetailClient postId={postId} />
    </BackendAuthGate>
  );
}
