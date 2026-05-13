import type { CommunityCommentList, CommunityPostDetail } from "@/lib/futures-api";
import type { ReactNode } from "react";
import { ArrowLeft, Eye, MessageCircle, ShieldCheck, ThumbsUp } from "lucide-react";
import Link from "next/link";
import CommunityState from "./CommunityState";
import CommunityTiptapRenderer from "./CommunityTiptapRenderer";
import {
  COMMUNITY_CATEGORY_LABELS,
  formatCommunityCount,
  formatCommunityDate,
} from "./community-format";

type CommunityDetailViewProps = {
  post: CommunityPostDetail | null;
  comments: CommunityCommentList | null;
  unavailable: boolean;
  message: string | null;
};

export default function CommunityDetailView({
  post,
  comments,
  unavailable,
  message,
}: CommunityDetailViewProps) {
  if (unavailable || !post) {
    return (
      <div className="mx-auto w-full max-w-[1000px] px-main-3 pb-24 pt-4">
        <CommunityState
          title="게시글을 불러오지 못했습니다"
          message={message ?? "삭제되었거나 접근할 수 없는 게시글입니다."}
          tone="error"
          actionHref="/community"
          actionLabel="목록으로 돌아가기"
        />
      </div>
    );
  }

  return (
    <article className="mx-auto flex w-full max-w-[1000px] flex-col gap-main-2 px-main-3 pb-24 pt-4">
      <Link className="inline-flex w-fit items-center gap-2 text-sm-custom font-semibold text-main-dark-gray/60 hover:text-main-blue" href="/community">
        <ArrowLeft size={16} /> 커뮤니티 목록
      </Link>

      <header className="rounded-main border border-white/60 bg-white/85 p-main-2 shadow-md backdrop-blur-md">
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-full bg-main-blue/10 px-2 py-1 text-xs-custom font-bold text-main-blue">
            {COMMUNITY_CATEGORY_LABELS[post.category]}
          </span>
          <span className="text-xs-custom text-main-dark-gray/45">{formatCommunityDate(post.createdAt)}</span>
          {post.canEdit || post.canDelete ? (
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-1 text-xs-custom font-bold text-emerald-600">
              <ShieldCheck size={13} /> 작성자/관리 권한
            </span>
          ) : null}
        </div>
        <h1 className="mt-4 text-3xl-custom font-bold leading-tight text-main-dark-gray">{post.title}</h1>
        <div className="mt-4 flex flex-wrap items-center justify-between gap-main text-sm-custom text-main-dark-gray/55">
          <span>by {post.authorNickname}</span>
          <div className="flex items-center gap-4">
            <Metric icon={<Eye size={15} />} value={post.viewCount} label="조회" />
            <Metric icon={<ThumbsUp size={15} />} value={post.likeCount} label="좋아요" active={post.likedByMe} />
            <Metric icon={<MessageCircle size={15} />} value={post.commentCount} label="댓글" />
          </div>
        </div>
      </header>

      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <CommunityTiptapRenderer contentJson={post.contentJson} />
      </section>

      <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="flex items-center justify-between">
          <h2 className="text-xl-custom font-bold text-main-dark-gray">댓글</h2>
          <span className="text-xs-custom text-main-dark-gray/50">읽기 전용</span>
        </div>
        {comments?.comments.length ? (
          <div className="mt-main grid gap-3">
            {comments.comments.map((comment) => (
              <div key={comment.id} className="rounded-main bg-main-light-gray/30 p-main">
                <div className="flex items-center justify-between gap-main">
                  <span className="font-semibold text-main-dark-gray">{comment.authorNickname}</span>
                  <span className="text-xs-custom text-main-dark-gray/45">{formatCommunityDate(comment.createdAt)}</span>
                </div>
                <p className="mt-2 whitespace-pre-wrap text-sm-custom leading-6 text-main-dark-gray/70">{comment.content}</p>
              </div>
            ))}
          </div>
        ) : (
          <div className="mt-main rounded-main bg-main-light-gray/35 p-main text-sm-custom text-main-dark-gray/55">
            아직 댓글이 없습니다. 댓글 작성은 다음 단계에서 연결됩니다.
          </div>
        )}
      </section>
    </article>
  );
}

function Metric({ icon, value, label, active = false }: { icon: ReactNode; value: number; label: string; active?: boolean }) {
  return (
    <span className={active ? "inline-flex items-center gap-1 text-main-blue" : "inline-flex items-center gap-1"} title={label}>
      {icon}
      {formatCommunityCount(value)}
    </span>
  );
}
