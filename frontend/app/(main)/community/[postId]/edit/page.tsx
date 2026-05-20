import BackendAuthGate from "@/components/router/BackendAuthGate";
import ProtectedPageSkeleton from "@/components/ui/shared/ProtectedPageSkeleton";
import CommunityEditClient from "@/components/router/(main)/community/CommunityEditClient";
import { notFound } from "next/navigation";

type CommunityEditPageProps = { params: Promise<{ postId: string }> };

export default async function CommunityEditPage({ params }: CommunityEditPageProps) {
  const { postId: rawPostId } = await params;
  const postId = Number(rawPostId);
  if (!Number.isInteger(postId) || postId <= 0) notFound();
  return (
    <BackendAuthGate fallback={<ProtectedPageSkeleton variant="community-editor" />}>
      <CommunityEditClient postId={postId} />
    </BackendAuthGate>
  );
}
