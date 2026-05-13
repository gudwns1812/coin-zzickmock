export type TiptapJson = {
  type: string;
  attrs?: Record<string, unknown>;
  text?: string;
  marks?: Array<{ type: string; attrs?: Record<string, unknown> }>;
  content?: TiptapJson[];
};

export const EMPTY_TIPTAP_DOCUMENT: TiptapJson = {
  type: "doc",
  content: [
    {
      type: "paragraph",
      content: [],
    },
  ],
};

export function parseTiptapDocument(contentJson: string | null | undefined): TiptapJson {
  if (!contentJson) {
    return EMPTY_TIPTAP_DOCUMENT;
  }

  try {
    const parsed: unknown = JSON.parse(contentJson);
    if (isTiptapNode(parsed) && parsed.type === "doc") {
      return parsed;
    }
  } catch {
    // fall through to empty document
  }

  return EMPTY_TIPTAP_DOCUMENT;
}

export function stringifyTiptapDocument(document: TiptapJson): string {
  return JSON.stringify(document);
}

export function extractTiptapText(document: TiptapJson): string {
  if (document.type === "text") {
    return document.text ?? "";
  }
  return (document.content ?? []).map(extractTiptapText).join(" ").trim();
}

export function extractImageObjectKeys(document: TiptapJson): string[] {
  const keys = new Set<string>();
  visitNodes(document, (node) => {
    if (node.type !== "image") {
      return;
    }
    const objectKey = node.attrs?.objectKey;
    if (typeof objectKey === "string" && objectKey.trim()) {
      keys.add(objectKey.trim());
    }
  });
  return [...keys];
}

function visitNodes(node: TiptapJson, visitor: (node: TiptapJson) => void) {
  visitor(node);
  for (const child of node.content ?? []) {
    visitNodes(child, visitor);
  }
}

function isTiptapNode(value: unknown): value is TiptapJson {
  if (!value || typeof value !== "object") {
    return false;
  }
  return typeof (value as { type?: unknown }).type === "string";
}
