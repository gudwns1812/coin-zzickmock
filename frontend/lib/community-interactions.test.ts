import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.join(__dirname, "..");

function source(relativePath: string): string {
  return readFileSync(path.join(rootDir, relativePath), "utf8");
}

test("community client mutations use backend-approved presign upload data", () => {
  const api = source("lib/futures-client-api.ts");
  const editor = source(
    "components/router/(main)/community/CommunityPostEditorClient.tsx"
  );

  assert.match(api, /presignCommunityImageUpload/);
  assert.match(api, /uploadCommunityImageToPresignedUrl/);
  assert.match(api, /if \(!response\.ok\)/);
  assert.match(editor, /objectKey: uploadedImage\.objectKey/);
  assert.match(editor, /src: uploadedImage\.publicUrl/);
  assert.doesNotMatch(editor, /URL\.createObjectURL/);
});

test("community editor routes picker drop and paste images through the same safe upload policy", () => {
  const editor = source(
    "components/router/(main)/community/CommunityPostEditorClient.tsx"
  );
  const policy = source(
    "components/router/(main)/community/community-editor-image-policy.ts"
  );

  assert.match(editor, /handleDrop/);
  assert.match(editor, /handlePaste/);
  assert.match(editor, /insertImageFilesIntoEditor/);
  assert.match(editor, /uploadCommunityEditorImage/);
  assert.match(editor, /insertUploadedImage/);
  assert.match(editor, /addInputRules\(\)\s*{\s*return \[\];\s*}/);
  assert.match(editor, /containsUnsupportedImageNode/);
  assert.match(policy, /containsUnsupportedCommunityEditorImagePayload/);
  assert.match(policy, /COMMUNITY_EDITOR_ACCEPTED_IMAGE_TYPES/);
  assert.doesNotMatch(policy, /containsExternalImageUrl/);
});

test("community editor user-facing copy hides infrastructure terminology", () => {
  const editor = source(
    "components/router/(main)/community/CommunityPostEditorClient.tsx"
  );
  const policy = source(
    "components/router/(main)/community/community-editor-image-policy.ts"
  );
  const api = source("lib/futures-client-api.ts");

  assert.doesNotMatch(
    editor,
    /이미지는 백엔드가 승인한 presigned URL 업로드 후 본문에 삽입됩니다/
  );
  assert.doesNotMatch(policy, /S3|CORS|backend|백엔드/i);
  assert.doesNotMatch(api, /S3 CORS/);
});

test("community write UI keeps NOTICE behind the admin flag", () => {
  const editor = source(
    "components/router/(main)/community/CommunityPostEditorClient.tsx"
  );

  assert.match(editor, /isAdmin\s*\? \["NOTICE", \.\.\.COMMUNITY_POST_CATEGORIES\]/);
  assert.match(editor, /공지사항은 관리자만 작성할 수 있습니다/);
});

test("community detail enables mutations without raw HTML rendering", () => {
  const detail = source("components/router/(main)/community/CommunityDetailView.tsx");
  const interactions = source(
    "components/router/(main)/community/CommunityDetailInteractions.tsx"
  );
  const renderer = source(
    "components/router/(main)/community/CommunityTiptapRenderer.tsx"
  );

  assert.match(detail, /CommunityPostActions/);
  assert.match(detail, /CommunityComments/);
  assert.match(interactions, /likeCommunityPost/);
  assert.match(interactions, /createCommunityComment/);
  assert.doesNotMatch(renderer, /dangerouslySetInnerHTML/);
});
