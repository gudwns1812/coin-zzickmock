export const COMMUNITY_EDITOR_ACCEPTED_IMAGE_TYPES = [
  "image/png",
  "image/jpeg",
  "image/webp",
  "image/gif",
] as const;

export const COMMUNITY_EDITOR_UPLOAD_PENDING_COPY = "이미지를 추가하는 중입니다…";
export const COMMUNITY_EDITOR_UPLOAD_SUCCESS_COPY = "이미지를 본문에 추가했습니다.";
export const COMMUNITY_EDITOR_UPLOAD_FAILURE_COPY =
  "이미지를 추가하지 못했습니다. 파일 형식/용량을 확인해주세요.";
export const COMMUNITY_EDITOR_UNSUPPORTED_IMAGE_COPY =
  "외부 이미지나 지원하지 않는 이미지 형식은 추가할 수 없습니다. 이미지 파일을 직접 추가해주세요.";

type ImageFileLike = {
  type: string;
};

type ImagePayload = {
  html?: string;
};

export function isAcceptedCommunityEditorImageFile(file: ImageFileLike): boolean {
  return COMMUNITY_EDITOR_ACCEPTED_IMAGE_TYPES.includes(
    file.type as (typeof COMMUNITY_EDITOR_ACCEPTED_IMAGE_TYPES)[number]
  );
}

export function isImageLikeCommunityEditorFile(file: ImageFileLike): boolean {
  return file.type.startsWith("image/");
}

export function splitCommunityEditorImageFiles<T extends ImageFileLike>(
  files: readonly T[]
): { accepted: T[]; rejected: T[] } {
  const accepted: T[] = [];
  const rejected: T[] = [];

  for (const file of files) {
    if (!isImageLikeCommunityEditorFile(file)) {
      continue;
    }
    if (isAcceptedCommunityEditorImageFile(file)) {
      accepted.push(file);
    } else {
      rejected.push(file);
    }
  }

  return { accepted, rejected };
}

export function containsUnsupportedCommunityEditorImagePayload({
  html,
}: ImagePayload): boolean {
  return Boolean(html && (containsHtmlImage(html) || containsDataImage(html)));
}

export function dataTransferHasCommunityEditorImageIntent(dataTransfer: DataTransfer | null): boolean {
  if (!dataTransfer) {
    return false;
  }

  const items = Array.from(dataTransfer.items ?? []);
  if (items.some((item) => item.kind === "file" && item.type.startsWith("image/"))) {
    return true;
  }

  const types = Array.from(dataTransfer.types ?? []);
  return types.includes("Files") || types.includes("text/html");
}

function containsHtmlImage(value: string): boolean {
  return /<img\b/i.test(value);
}

function containsDataImage(value: string): boolean {
  return /data:image\//i.test(value);
}
