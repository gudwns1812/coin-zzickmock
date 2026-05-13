import assert from "node:assert/strict";
import { describe, it } from "node:test";

const communityFormatModule: typeof import("../components/router/(main)/community/community-format") = await import(
  new URL("../components/router/(main)/community/community-format.ts", import.meta.url).href
);

const {
  COMMUNITY_CATEGORY_LABELS,
  formatCommunityCount,
  formatCommunityDate,
} = communityFormatModule;

describe("community read UI formatting", () => {
  it("maps backend category names to Korean labels", () => {
    assert.equal(COMMUNITY_CATEGORY_LABELS.NOTICE, "공지");
    assert.equal(COMMUNITY_CATEGORY_LABELS.CHART_ANALYSIS, "차트분석");
    assert.equal(COMMUNITY_CATEGORY_LABELS.COIN_INFORMATION, "코인정보");
    assert.equal(COMMUNITY_CATEGORY_LABELS.CHAT, "잡담");
  });

  it("formats counters with the Korean locale", () => {
    assert.equal(formatCommunityCount(999), "999");
    assert.equal(formatCommunityCount(1_200), "1,200");
    assert.equal(formatCommunityCount(15_400), "15,400");
  });

  it("keeps invalid dates as a terse safe fallback", () => {
    assert.equal(formatCommunityDate("not-a-date"), "-");
  });
});
