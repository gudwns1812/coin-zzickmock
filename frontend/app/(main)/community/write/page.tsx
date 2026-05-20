import BackendAuthGate from "@/components/router/BackendAuthGate";
import ProtectedPageSkeleton from "@/components/ui/shared/ProtectedPageSkeleton";
import CommunityPostEditorClient from "@/components/router/(main)/community/CommunityPostEditorClient";

export default async function CommunityWritePage() {
  return (
    <BackendAuthGate fallback={<ProtectedPageSkeleton variant="community-editor" />}>
      <CommunityPostEditorClient
        isAdmin={false}
        mode="create"
      />
    </BackendAuthGate>
  );
}
