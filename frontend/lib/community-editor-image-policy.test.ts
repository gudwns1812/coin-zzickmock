import assert from "node:assert/strict";
import { describe, it } from "node:test";

const policyModule: typeof import("../components/router/(main)/community/community-editor-image-policy") =
  await import(
    new URL(
      "../components/router/(main)/community/community-editor-image-policy.ts",
      import.meta.url
    ).href
  );

const {
  COMMUNITY_EDITOR_UPLOAD_FAILURE_COPY,
  containsUnsupportedCommunityEditorImagePayload,
  isAcceptedCommunityEditorImageFile,
  splitCommunityEditorImageFiles,
} = policyModule;

describe("community editor image policy", () => {
  it("accepts only backend-supported community image MIME types", () => {
    assert.equal(isAcceptedCommunityEditorImageFile({ type: "image/png" }), true);
    assert.equal(isAcceptedCommunityEditorImageFile({ type: "image/jpeg" }), true);
    assert.equal(isAcceptedCommunityEditorImageFile({ type: "image/webp" }), true);
    assert.equal(isAcceptedCommunityEditorImageFile({ type: "image/gif" }), true);
    assert.equal(isAcceptedCommunityEditorImageFile({ type: "image/svg+xml" }), false);
    assert.equal(isAcceptedCommunityEditorImageFile({ type: "text/plain" }), false);
  });

  it("splits accepted image files from unsupported image files without treating text as an image", () => {
    const result = splitCommunityEditorImageFiles([
      { name: "chart.webp", type: "image/webp" },
      { name: "vector.svg", type: "image/svg+xml" },
      { name: "notes.txt", type: "text/plain" },
    ]);

    assert.deepEqual(result.accepted.map((file) => file.name), ["chart.webp"]);
    assert.deepEqual(result.rejected.map((file) => file.name), ["vector.svg"]);
  });

  it("detects unsupported pasted or dropped HTML image payloads", () => {
    assert.equal(
      containsUnsupportedCommunityEditorImagePayload({
        html: '<p>ok</p><img src="https://example.com/chart.png">',
      }),
      true
    );
    assert.equal(
      containsUnsupportedCommunityEditorImagePayload({
        html: '<img src="data:image/png;base64,AAAA">',
      }),
      true
    );
    assert.equal(
      containsUnsupportedCommunityEditorImagePayload({
        html: "<p><strong>normal rich text</strong></p>",
      }),
      false
    );
  });

  it("does not block plain text image URLs because they do not create image nodes", () => {
    assert.equal(
      containsUnsupportedCommunityEditorImagePayload({
        html: undefined,
      }),
      false
    );
  });

  it("keeps upload failure copy free of infrastructure terminology", () => {
    assert.doesNotMatch(COMMUNITY_EDITOR_UPLOAD_FAILURE_COPY, /presigned|S3|CORS|backend|백엔드/i);
  });
});
