import BackendAuthGate from "@/components/router/BackendAuthGate";
import CommunityPostEditorClient from "@/components/router/(main)/community/CommunityPostEditorClient";

export default async function CommunityWritePage() {
  return (
    <BackendAuthGate>
      <CommunityPostEditorClient
        isAdmin={false}
        mode="create"
      />
    </BackendAuthGate>
  );
}
