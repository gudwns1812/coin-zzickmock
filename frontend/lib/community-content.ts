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

export function normalizeTiptapDocumentForSubmit(document: TiptapJson): TiptapJson {
  return normalizeNode(document);
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

function normalizeNode(node: TiptapJson): TiptapJson {
  const normalized: TiptapJson = { type: node.type };

  if (node.type === "text") {
    normalized.text = typeof node.text === "string" ? node.text : "";
    const marks = normalizeMarks(node.marks);
    if (marks.length > 0) {
      normalized.marks = marks;
    }
    return normalized;
  }

  if (node.type === "heading" && typeof node.attrs?.level === "number") {
    normalized.attrs = { level: node.attrs.level };
  }

  if (node.type === "image") {
    normalized.attrs = {
      objectKey: typeof node.attrs?.objectKey === "string" ? node.attrs.objectKey : "",
      src: typeof node.attrs?.src === "string" ? node.attrs.src : "",
    };
    return normalized;
  }

  if (Array.isArray(node.content)) {
    normalized.content = node.content.map(normalizeNode);
  }

  return normalized;
}

function normalizeMarks(marks: TiptapJson["marks"] | undefined): NonNullable<TiptapJson["marks"]> {
  return (marks ?? []).flatMap((mark) => {
    if (mark.type === "bold" || mark.type === "italic" || mark.type === "code") {
      return [{ type: mark.type }];
    }
    if (mark.type === "link" && typeof mark.attrs?.href === "string") {
      return [{ type: "link", attrs: { href: mark.attrs.href } }];
    }
    return [];
  });
}

function isTiptapNode(value: unknown): value is TiptapJson {
  if (!value || typeof value !== "object") {
    return false;
  }
  return typeof (value as { type?: unknown }).type === "string";
}
