import CommunityPostEditorClient from "@/components/router/(main)/community/CommunityPostEditorClient";
import { getJwtToken } from "@/utils/auth";
import { redirect } from "next/navigation";

export default async function CommunityWritePage() {
  const token = await getJwtToken();
  if (!token) {
    redirect("/login");
  }

  return (
    <CommunityPostEditorClient
      isAdmin={Boolean(token.admin || token.role === "ADMIN")}
      mode="create"
    />
  );
}
