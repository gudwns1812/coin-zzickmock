import type { CommunityCategory } from "@/lib/futures-api";

export const COMMUNITY_CATEGORY_LABELS: Record<CommunityCategory, string> = {
  NOTICE: "공지",
  CHART_ANALYSIS: "사례분석",
  COIN_INFORMATION: "코인정보",
  CHAT: "잡담",
};

export const COMMUNITY_POST_CATEGORIES: Exclude<CommunityCategory, "NOTICE">[] = [
  "CHART_ANALYSIS",
  "COIN_INFORMATION",
  "CHAT",
];

export function formatCommunityDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function formatCommunityCount(value: number): string {
  return value.toLocaleString("ko-KR");
}
