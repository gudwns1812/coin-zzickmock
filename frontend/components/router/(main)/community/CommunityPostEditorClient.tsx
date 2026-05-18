"use client";

import type { TiptapJson } from "@/lib/community-content";
import {
  extractImageObjectKeys,
  extractTiptapText,
  normalizeTiptapDocumentForSubmit,
  parseTiptapDocument,
} from "@/lib/community-content";
import type { CommunityCategory, CommunityPostDetail } from "@/lib/futures-api";
import {
  createCommunityPost,
  presignCommunityImageUpload,
  updateCommunityPost,
  uploadCommunityImageToPresignedUrl,
} from "@/lib/futures-client-api";
import Image from "@tiptap/extension-image";
import LinkExtension from "@tiptap/extension-link";
import { EditorContent, useEditor, type Editor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import clsx from "clsx";
import { Bold, Heading2, ImagePlus, Italic, List, ListOrdered, Save } from "lucide-react";
import { useRouter } from "next/navigation";
import type { ChangeEvent, DragEvent, ReactNode } from "react";
import { useMemo, useRef, useState } from "react";
import {
  COMMUNITY_EDITOR_UNSUPPORTED_IMAGE_COPY,
  COMMUNITY_EDITOR_UPLOAD_FAILURE_COPY,
  COMMUNITY_EDITOR_UPLOAD_PENDING_COPY,
  COMMUNITY_EDITOR_UPLOAD_SUCCESS_COPY,
  containsUnsupportedCommunityEditorImagePayload,
  dataTransferHasCommunityEditorImageIntent,
  splitCommunityEditorImageFiles,
} from "./community-editor-image-policy";
import { COMMUNITY_CATEGORY_LABELS, COMMUNITY_POST_CATEGORIES } from "./community-format";

const TrustedImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      objectKey: {
        default: null,
        parseHTML: (element) => element.getAttribute("data-object-key"),
        renderHTML: (attributes) => {
          if (typeof attributes.objectKey !== "string" || !attributes.objectKey) {
            return {};
          }
          return { "data-object-key": attributes.objectKey };
        },
      },
    };
  },
  addInputRules() {
    return [];
  },
});

type CommunityPostEditorClientProps = {
  mode: "create" | "edit";
  post?: CommunityPostDetail;
  isAdmin: boolean;
};

type UploadedCommunityEditorImage = {
  publicUrl: string;
  objectKey: string;
  fileName: string;
};

export default function CommunityPostEditorClient({
  mode,
  post,
  isAdmin,
}: CommunityPostEditorClientProps) {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const editorRef = useRef<Editor | null>(null);
  const [title, setTitle] = useState(post?.title ?? "");
  const [category, setCategory] = useState<CommunityCategory>(post?.category ?? "CHAT");
  const [error, setError] = useState<string | null>(null);
  const [uploadStatus, setUploadStatus] = useState<string | null>(null);
  const [draggingImage, setDraggingImage] = useState(false);
  const [editorRevision, setEditorRevision] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);

  const initialDocument = useMemo(
    () => parseTiptapDocument(post?.contentJson),
    [post?.contentJson]
  );

  const editor = useEditor({
    immediatelyRender: false,
    extensions: [
      StarterKit.configure({
        heading: { levels: [1, 2, 3, 4] },
        horizontalRule: false,
        link: false,
        strike: false,
        underline: false,
      }),
      LinkExtension.configure({
        autolink: false,
        openOnClick: false,
        protocols: ["http", "https"],
      }),
      TrustedImage.configure({
        allowBase64: false,
      }),
    ],
    content: initialDocument,
    onCreate: ({ editor: createdEditor }) => {
      editorRef.current = createdEditor;
      setEditorRevision((revision) => revision + 1);
    },
    onSelectionUpdate: () => setEditorRevision((revision) => revision + 1),
    onUpdate: () => setEditorRevision((revision) => revision + 1),
    onDestroy: () => {
      editorRef.current = null;
    },
    editorProps: {
      attributes: {
        class:
          "min-h-[360px] rounded-main border border-main-light-gray bg-white p-main-2 text-base-custom leading-7 text-main-dark-gray outline-none transition-colors focus:border-main-blue/50",
      },
      handleDrop: (view, event) => {
        const imageFiles = Array.from(event.dataTransfer?.files ?? []);
        const { accepted, rejected } = splitCommunityEditorImageFiles(imageFiles);
        const hasUnsupportedImagePayload = containsUnsupportedCommunityEditorImagePayload({
          html: safeTransferData(event.dataTransfer, "text/html"),
        });

        if (accepted.length === 0 && rejected.length === 0 && !hasUnsupportedImagePayload) {
          return false;
        }

        event.preventDefault();
        setDraggingImage(false);

        if (accepted.length > 0 || rejected.length > 0) {
          const insertionPosition =
            view.posAtCoords({ left: event.clientX, top: event.clientY })?.pos ??
            view.state.selection.to;
          void insertImageFilesIntoEditor(imageFiles, insertionPosition);
          return true;
        }

        setError(COMMUNITY_EDITOR_UNSUPPORTED_IMAGE_COPY);
        setUploadStatus(null);
        return true;
      },
      handlePaste: (view, event) => {
        const clipboardFiles = filesFromClipboard(event.clipboardData);
        const { accepted, rejected } = splitCommunityEditorImageFiles(clipboardFiles);
        const hasUnsupportedImagePayload = containsUnsupportedCommunityEditorImagePayload({
          html: event.clipboardData?.getData("text/html"),
        });

        if (accepted.length === 0 && rejected.length === 0 && !hasUnsupportedImagePayload) {
          return false;
        }

        event.preventDefault();

        if (accepted.length > 0 || rejected.length > 0) {
          void insertImageFilesIntoEditor(clipboardFiles, view.state.selection.to);
          return true;
        }

        setError(COMMUNITY_EDITOR_UNSUPPORTED_IMAGE_COPY);
        setUploadStatus(null);
        return true;
      },
    },
  });

  const allowedCategories: CommunityCategory[] = isAdmin
    ? ["NOTICE", ...COMMUNITY_POST_CATEGORIES]
    : COMMUNITY_POST_CATEGORIES;

  const saveDisabled = saving || uploading || title.trim().length === 0 || !editor;
  const toolbarState = useMemo(
    () => ({
      bold: editor?.isActive("bold") ?? false,
      italic: editor?.isActive("italic") ?? false,
      heading2: editor?.isActive("heading", { level: 2 }) ?? false,
      bulletList: editor?.isActive("bulletList") ?? false,
      orderedList: editor?.isActive("orderedList") ?? false,
    }),
    [editor, editorRevision]
  );

  const handleImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    event.target.value = "";
    if (files.length === 0) {
      return;
    }

    await insertImageFilesIntoEditor(files, editorRef.current?.state.selection.to);
  };

  async function insertImageFilesIntoEditor(files: File[], position?: number) {
    const currentEditor = editorRef.current;
    if (!currentEditor || uploading) {
      return;
    }

    const { accepted, rejected } = splitCommunityEditorImageFiles(files);
    if (accepted.length === 0) {
      setError(COMMUNITY_EDITOR_UPLOAD_FAILURE_COPY);
      setUploadStatus(null);
      return;
    }

    setUploading(true);
    setError(null);
    setUploadStatus(
      accepted.length > 1
        ? `이미지 ${accepted.length}장을 추가하는 중입니다…`
        : COMMUNITY_EDITOR_UPLOAD_PENDING_COPY
    );

    try {
      for (const [index, file] of accepted.entries()) {
        const uploadedImage = await uploadCommunityEditorImage(file);
        if (editorRef.current !== currentEditor || currentEditor.isDestroyed) {
          return;
        }
        insertUploadedImage(currentEditor, uploadedImage, index === 0 ? position : undefined);
      }
      setUploadStatus(
        accepted.length > 1
          ? `이미지 ${accepted.length}장을 본문에 추가했습니다.`
          : COMMUNITY_EDITOR_UPLOAD_SUCCESS_COPY
      );
      if (rejected.length > 0) {
        setError("일부 이미지를 추가하지 못했습니다. 파일 형식/용량을 확인해주세요.");
      }
    } catch {
      setError(COMMUNITY_EDITOR_UPLOAD_FAILURE_COPY);
      setUploadStatus(null);
    } finally {
      setUploading(false);
    }
  }

  const handleSave = () => {
    if (!editor) {
      return;
    }
    const document = normalizeTiptapDocumentForSubmit(editor.getJSON() as TiptapJson);
    const trimmedTitle = title.trim();
    const extractedText = extractTiptapText(document);
    const imageObjectKeys = extractImageObjectKeys(document);

    if (trimmedTitle.length === 0 || trimmedTitle.length > 200) {
      setError("제목은 1자 이상 200자 이하로 입력해주세요.");
      return;
    }
    if (!isAdmin && category === "NOTICE") {
      setError("공지사항은 관리자만 작성할 수 있습니다.");
      return;
    }
    if (containsUnsupportedImageNode(document)) {
      setError(COMMUNITY_EDITOR_UNSUPPORTED_IMAGE_COPY);
      return;
    }
    if (extractedText.length === 0 && imageObjectKeys.length === 0) {
      setError("본문 또는 이미지를 입력해주세요.");
      return;
    }

    setSaving(true);
    setError(null);
    void (async () => {
      try {
        const input = {
          category,
          title: trimmedTitle,
          contentJson: document,
          imageObjectKeys,
        };
        const result = mode === "edit" && post
          ? await updateCommunityPost(post.id, input)
          : await createCommunityPost(input);
        router.push(`/community/${result.postId}`);
        router.refresh();
      } catch (saveError) {
        setError(errorMessage(saveError));
      } finally {
        setSaving(false);
      }
    })();
  };

  function handleEditorDragEnter(event: DragEvent<HTMLDivElement>) {
    if (!dataTransferHasCommunityEditorImageIntent(event.dataTransfer)) {
      return;
    }
    event.preventDefault();
    setDraggingImage(true);
  }

  function handleEditorDragOver(event: DragEvent<HTMLDivElement>) {
    if (!dataTransferHasCommunityEditorImageIntent(event.dataTransfer)) {
      return;
    }
    event.preventDefault();
    event.dataTransfer.dropEffect = "copy";
    setDraggingImage(true);
  }

  function handleEditorDragLeave(event: DragEvent<HTMLDivElement>) {
    const nextTarget = event.relatedTarget;
    if (nextTarget instanceof Node && event.currentTarget.contains(nextTarget)) {
      return;
    }
    setDraggingImage(false);
  }

  return (
    <div className="mx-auto flex w-full max-w-[1000px] flex-col gap-main-2 px-main-3 pb-24 pt-4">
      <div>
        <p className="text-sm-custom font-semibold text-main-blue">Community Editor</p>
        <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
          {mode === "edit" ? "게시글 수정" : "게시글 작성"}
        </h1>
        <p className="mt-2 text-sm-custom text-main-dark-gray/55">
          제목과 본문을 작성하고, 이미지는 버튼·드래그·붙여넣기로 추가할 수 있어요.
        </p>
      </div>

      <section className="grid gap-main rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <label className="grid gap-2 text-sm-custom font-semibold text-main-dark-gray">
          카테고리
          <select
            className="rounded-main border border-main-light-gray bg-white p-main text-sm-custom outline-none focus:border-main-blue/50"
            onChange={(event) => setCategory(event.target.value as CommunityCategory)}
            value={category}
          >
            {allowedCategories.map((item) => (
              <option key={item} value={item}>
                {COMMUNITY_CATEGORY_LABELS[item]}
              </option>
            ))}
          </select>
        </label>

        <label className="grid gap-2 text-sm-custom font-semibold text-main-dark-gray">
          제목
          <input
            className="rounded-main border border-main-light-gray bg-white p-main text-sm-custom outline-none focus:border-main-blue/50"
            maxLength={200}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="제목을 입력하세요"
            value={title}
          />
        </label>

        <div className="flex flex-wrap items-center gap-2 rounded-main bg-main-light-gray/30 p-2">
          <ToolbarButton active={toolbarState.bold} label="굵게" onClick={() => editor?.chain().focus().toggleBold().run()}>
            <Bold size={15} /> 굵게
          </ToolbarButton>
          <ToolbarButton active={toolbarState.italic} label="기울임" onClick={() => editor?.chain().focus().toggleItalic().run()}>
            <Italic size={15} /> 기울임
          </ToolbarButton>
          <ToolbarButton active={toolbarState.heading2} label="제목 2" onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()}>
            <Heading2 size={15} /> 제목
          </ToolbarButton>
          <ToolbarButton active={toolbarState.bulletList} label="글머리 목록" onClick={() => editor?.chain().focus().toggleBulletList().run()}>
            <List size={15} /> 목록
          </ToolbarButton>
          <ToolbarButton active={toolbarState.orderedList} label="번호 목록" onClick={() => editor?.chain().focus().toggleOrderedList().run()}>
            <ListOrdered size={15} /> 번호
          </ToolbarButton>
          <button
            className="inline-flex items-center gap-1 rounded-main bg-white px-3 py-2 text-xs-custom font-semibold text-main-dark-gray/65 hover:text-main-blue disabled:opacity-50"
            disabled={uploading}
            onClick={() => fileInputRef.current?.click()}
            type="button"
          >
            <ImagePlus size={15} /> {uploading ? "업로드 중" : "이미지"}
          </button>
          <input
            ref={fileInputRef}
            accept="image/png,image/jpeg,image/webp,image/gif"
            className="hidden"
            onChange={handleImage}
            multiple
            type="file"
          />
        </div>

        <div
          className={clsx(
            "community-rich-editor rounded-main transition-colors",
            draggingImage && "bg-main-blue/5 ring-2 ring-main-blue/35 ring-offset-2 ring-offset-white"
          )}
          onDragEnter={(event) => handleEditorDragEnter(event)}
          onDragLeave={(event) => handleEditorDragLeave(event)}
          onDragOver={(event) => handleEditorDragOver(event)}
          onDrop={() => setDraggingImage(false)}
        >
          <EditorContent editor={editor} />
        </div>

        {uploadStatus ? (
          <p className="text-sm-custom font-medium text-main-blue" role="status">
            {uploadStatus}
          </p>
        ) : null}
        {error ? <p className="text-sm-custom text-main-red" role="alert">{error}</p> : null}

        <div className="flex items-center justify-end gap-2">
          <button
            className="rounded-main bg-main-light-gray px-main py-2 text-sm-custom font-semibold text-main-dark-gray/60"
            onClick={() => router.push(post ? `/community/${post.id}` : "/community")}
            type="button"
          >
            취소
          </button>
          <button
            className="inline-flex items-center gap-2 rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white disabled:bg-main-light-gray disabled:text-main-dark-gray/35"
            disabled={saveDisabled}
            onClick={handleSave}
            type="button"
          >
            <Save size={16} /> 저장
          </button>
        </div>
      </section>
    </div>
  );
}

function ToolbarButton({
  active,
  children,
  label,
  onClick,
}: {
  active: boolean;
  children: ReactNode;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      aria-label={label}
      aria-pressed={active}
      className={clsx(
        "inline-flex items-center gap-1 rounded-main border px-3 py-2 text-xs-custom font-semibold transition-colors",
        active
          ? "border-main-blue bg-main-blue text-white shadow-sm ring-2 ring-main-blue/20"
          : "border-main-light-gray bg-white text-main-dark-gray/65 hover:border-main-blue/45 hover:text-main-blue"
      )}
      onClick={onClick}
      type="button"
    >
      {children}
    </button>
  );
}

async function uploadCommunityEditorImage(file: File): Promise<UploadedCommunityEditorImage> {
  const presign = await presignCommunityImageUpload({
    fileName: file.name,
    contentType: file.type,
    sizeBytes: file.size,
  });
  await uploadCommunityImageToPresignedUrl(file, presign);
  return {
    publicUrl: presign.publicUrl,
    objectKey: presign.objectKey,
    fileName: file.name,
  };
}

function insertUploadedImage(
  editor: Editor,
  uploadedImage: UploadedCommunityEditorImage,
  position?: number
) {
  const docEnd = editor.state.doc.content.size;
  let chain = editor.chain().focus();
  if (typeof position === "number" && Number.isFinite(position)) {
    chain = chain.setTextSelection(Math.max(0, Math.min(position, docEnd)));
  }
  chain
    .setImage({
      src: uploadedImage.publicUrl,
      alt: uploadedImage.fileName,
      objectKey: uploadedImage.objectKey,
    } as Parameters<typeof editor.commands.setImage>[0])
    .run();
}

function filesFromClipboard(clipboardData: DataTransfer | null): File[] {
  if (!clipboardData) {
    return [];
  }

  const itemFiles = Array.from(clipboardData.items ?? []).flatMap((item) => {
    if (item.kind !== "file") {
      return [];
    }
    const file = item.getAsFile();
    return file ? [file] : [];
  });

  if (itemFiles.length > 0) {
    return itemFiles;
  }

  return Array.from(clipboardData.files ?? []);
}

function safeTransferData(dataTransfer: DataTransfer | null, format: string): string | undefined {
  try {
    return dataTransfer?.getData(format) || undefined;
  } catch {
    return undefined;
  }
}

function containsUnsupportedImageNode(document: TiptapJson): boolean {
  if (document.type === "image") {
    const objectKey = document.attrs?.objectKey;
    return typeof objectKey !== "string" || objectKey.trim().length === 0;
  }

  return (document.content ?? []).some(containsUnsupportedImageNode);
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "요청을 처리하지 못했습니다.";
}
