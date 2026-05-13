import assert from "node:assert/strict";
import { describe, it } from "node:test";

const communityContentModule: typeof import("./community-content") = await import(
  new URL("./community-content.ts", import.meta.url).href
);

const { normalizeTiptapDocumentForSubmit } = communityContentModule;

describe("community content submit normalization", () => {
  it("strips editor-only image and link attrs before backend validation", () => {
    const normalized = normalizeTiptapDocumentForSubmit({
      type: "doc",
      content: [
        {
          type: "paragraph",
          content: [
            {
              type: "text",
              text: "공유 링크",
              marks: [
                {
                  type: "link",
                  attrs: {
                    href: "https://example.com/chart",
                    target: "_blank",
                    rel: "noopener noreferrer",
                  },
                },
              ],
            },
          ],
        },
        {
          type: "image",
          attrs: {
            src: "https://cdn.example.com/community/7/image.webp",
            objectKey: "community/7/image.webp",
            alt: "client-only alt text",
            title: "client-only title",
          },
        },
      ],
    });

    assert.deepEqual(normalized.content?.[0].content?.[0].marks, [
      {
        type: "link",
        attrs: {
          href: "https://example.com/chart",
        },
      },
    ]);
    assert.deepEqual(normalized.content?.[1].attrs, {
      objectKey: "community/7/image.webp",
      src: "https://cdn.example.com/community/7/image.webp",
    });
  });
});
