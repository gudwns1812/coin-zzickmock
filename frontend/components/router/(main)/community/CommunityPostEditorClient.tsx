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
import { EditorContent, useEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import clsx from "clsx";
import { ImagePlus, List, ListOrdered, Save } from "lucide-react";
import { useRouter } from "next/navigation";
import type { ChangeEvent, ReactNode } from "react";
import { useMemo, useRef, useState } from "react";
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
});

type CommunityPostEditorClientProps = {
  mode: "create" | "edit";
  post?: CommunityPostDetail;
  isAdmin: boolean;
};

export default function CommunityPostEditorClient({
  mode,
  post,
  isAdmin,
}: CommunityPostEditorClientProps) {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [title, setTitle] = useState(post?.title ?? "");
  const [category, setCategory] = useState<CommunityCategory>(post?.category ?? "CHAT");
  const [error, setError] = useState<string | null>(null);
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
    editorProps: {
      attributes: {
        class:
          "min-h-[320px] rounded-main border border-main-light-gray bg-white p-main-2 text-base-custom leading-7 text-main-dark-gray outline-none focus:border-main-blue/50",
      },
    },
  });

  const allowedCategories: CommunityCategory[] = isAdmin
    ? ["NOTICE", ...COMMUNITY_POST_CATEGORIES]
    : COMMUNITY_POST_CATEGORIES;

  const saveDisabled = saving || uploading || title.trim().length === 0 || !editor;

  const handleImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || !editor) {
      return;
    }

    setUploading(true);
    setError(null);
    try {
      const presign = await presignCommunityImageUpload({
        fileName: file.name,
        contentType: file.type,
        sizeBytes: file.size,
      });
      await uploadCommunityImageToPresignedUrl(file, presign);
      editor
        .chain()
        .focus()
        .setImage({
          src: presign.publicUrl,
          alt: file.name,
          objectKey: presign.objectKey,
        } as Parameters<typeof editor.commands.setImage>[0])
        .run();
    } catch (uploadError) {
      setError(errorMessage(uploadError));
    } finally {
      setUploading(false);
    }
  };

  const handleSave = () => {
    if (!editor) {
      return;
    }
    const document = normalizeTiptapDocumentForSubmit(editor.getJSON() as TiptapJson);
    const trimmedTitle = title.trim();
    const extractedText = extractTiptapText(document);

    if (trimmedTitle.length === 0 || trimmedTitle.length > 200) {
      setError("제목은 1자 이상 200자 이하로 입력해주세요.");
      return;
    }
    if (!isAdmin && category === "NOTICE") {
      setError("공지사항은 관리자만 작성할 수 있습니다.");
      return;
    }
    if (extractedText.length === 0 && extractImageObjectKeys(document).length === 0) {
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
          imageObjectKeys: extractImageObjectKeys(document),
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

  return (
    <div className="mx-auto flex w-full max-w-[1000px] flex-col gap-main-2 px-main-3 pb-24 pt-4">
      <div>
        <p className="text-sm-custom font-semibold text-main-blue">Community Editor</p>
        <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
          {mode === "edit" ? "게시글 수정" : "게시글 작성"}
        </h1>
        <p className="mt-2 text-sm-custom text-main-dark-gray/55">
          이미지는 백엔드가 승인한 presigned URL 업로드 후 본문에 삽입됩니다.
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
          <ToolbarButton active={editor?.isActive("bold") ?? false} onClick={() => editor?.chain().focus().toggleBold().run()}>
            Bold
          </ToolbarButton>
          <ToolbarButton active={editor?.isActive("italic") ?? false} onClick={() => editor?.chain().focus().toggleItalic().run()}>
            Italic
          </ToolbarButton>
          <ToolbarButton active={editor?.isActive("heading", { level: 2 }) ?? false} onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()}>
            H2
          </ToolbarButton>
          <ToolbarButton active={editor?.isActive("bulletList") ?? false} onClick={() => editor?.chain().focus().toggleBulletList().run()}>
            <List size={15} /> 목록
          </ToolbarButton>
          <ToolbarButton active={editor?.isActive("orderedList") ?? false} onClick={() => editor?.chain().focus().toggleOrderedList().run()}>
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
            type="file"
          />
        </div>

        <EditorContent editor={editor} />

        {error ? <p className="text-sm-custom text-main-red">{error}</p> : null}

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
  onClick,
}: {
  active: boolean;
  children: ReactNode;
  onClick: () => void;
}) {
  return (
    <button
      className={clsx(
        "inline-flex items-center gap-1 rounded-main px-3 py-2 text-xs-custom font-semibold transition-colors",
        active ? "bg-main-blue text-white" : "bg-white text-main-dark-gray/65 hover:text-main-blue"
      )}
      onClick={onClick}
      type="button"
    >
      {children}
    </button>
  );
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "요청을 처리하지 못했습니다.";
}
