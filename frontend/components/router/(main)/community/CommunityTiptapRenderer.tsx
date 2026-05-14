import type { TiptapJson } from "@/lib/community-content";
import { parseTiptapDocument } from "@/lib/community-content";
import type { ReactNode } from "react";

type TiptapNode = TiptapJson;

type TiptapMark = NonNullable<TiptapJson["marks"]>[number];

export default function CommunityTiptapRenderer({ contentJson }: { contentJson: string }) {
  const document = parseDocument(contentJson);
  if (!document) {
    return (
      <div className="rounded-main bg-main-light-gray/40 p-main text-sm-custom text-main-dark-gray/60">
        본문을 표시할 수 없습니다.
      </div>
    );
  }

  return <div className="space-y-4 text-base-custom leading-7 text-main-dark-gray">{renderChildren(document.content)}</div>;
}

function parseDocument(contentJson: string): TiptapNode | null {
  const document = parseTiptapDocument(contentJson);
  return document.type === "doc" ? document : null;
}

function renderChildren(children: TiptapNode[] | undefined): ReactNode[] {
  return (children ?? []).map((child, index) => renderNode(child, index));
}

function renderNode(node: TiptapNode, index: number): ReactNode {
  switch (node.type) {
    case "paragraph":
      return <p key={index}>{renderChildren(node.content)}</p>;
    case "heading": {
      const level = typeof node.attrs?.level === "number" ? node.attrs.level : 2;
      const className = "font-bold text-main-dark-gray";
      if (level === 1) return <h1 key={index} className={`text-3xl-custom ${className}`}>{renderChildren(node.content)}</h1>;
      if (level === 2) return <h2 key={index} className={`text-2xl-custom ${className}`}>{renderChildren(node.content)}</h2>;
      return <h3 key={index} className={`text-xl-custom ${className}`}>{renderChildren(node.content)}</h3>;
    }
    case "text":
      return renderText(node, index);
    case "bulletList":
      return <ul key={index} className="list-disc space-y-2 pl-6">{renderChildren(node.content)}</ul>;
    case "orderedList":
      return <ol key={index} className="list-decimal space-y-2 pl-6">{renderChildren(node.content)}</ol>;
    case "listItem":
      return <li key={index}>{renderChildren(node.content)}</li>;
    case "blockquote":
      return <blockquote key={index} className="border-l-4 border-main-blue/40 bg-main-light-gray/35 py-2 pl-4 text-main-dark-gray/70">{renderChildren(node.content)}</blockquote>;
    case "codeBlock":
      return <pre key={index} className="overflow-x-auto rounded-main bg-main-dark-gray p-main text-sm-custom text-white"><code>{plainText(node.content)}</code></pre>;
    case "hardBreak":
      return <br key={index} />;
    case "image":
      return renderImage(node, index);
    default:
      return null;
  }
}

function renderText(node: TiptapNode, index: number): ReactNode {
  let content: ReactNode = node.text ?? "";
  for (const mark of node.marks ?? []) {
    if (mark.type === "bold") content = <strong>{content}</strong>;
    if (mark.type === "italic") content = <em>{content}</em>;
    if (mark.type === "code") content = <code className="rounded bg-main-light-gray px-1 py-0.5 text-sm-custom">{content}</code>;
    if (mark.type === "link") {
      const href = typeof mark.attrs?.href === "string" ? mark.attrs.href : "";
      if (href.startsWith("http://") || href.startsWith("https://")) {
        content = <a className="text-main-blue underline underline-offset-4" href={href} rel="noreferrer" target="_blank">{content}</a>;
      }
    }
  }
  return <span key={index}>{content}</span>;
}

function renderImage(node: TiptapNode, index: number): ReactNode {
  const src = typeof node.attrs?.src === "string" ? node.attrs.src : "";
  const alt = typeof node.attrs?.alt === "string" ? node.attrs.alt : "커뮤니티 이미지";
  if (!src.startsWith("http://") && !src.startsWith("https://")) {
    return null;
  }
  return (
    <figure key={index} className="overflow-hidden rounded-main border border-main-light-gray bg-white">
      <img alt={alt} className="max-h-[520px] w-full object-contain" src={src} />
    </figure>
  );
}

function plainText(children: TiptapNode[] | undefined): string {
  return (children ?? []).map((child) => child.text ?? plainText(child.content)).join("");
}
