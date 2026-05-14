import type { CommunityCategory } from "@/lib/futures-api";

export const COMMUNITY_CATEGORY_LABELS: Record<CommunityCategory, string> = {
  NOTICE: "공지",
  CHART_ANALYSIS: "차트분석",
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
  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

export function formatCommunityCount(value: number): string {
  return value.toLocaleString("ko-KR");
}
