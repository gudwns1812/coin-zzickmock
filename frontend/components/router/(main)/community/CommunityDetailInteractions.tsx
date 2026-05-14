"use client";

import type {
  CommunityCommentList,
  CommunityPostDetail,
} from "@/lib/futures-api";
import {
  createCommunityComment,
  deleteCommunityComment,
  deleteCommunityPost,
  likeCommunityPost,
  unlikeCommunityPost,
} from "@/lib/futures-client-api";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { MessageCircle, Pencil, ThumbsUp, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { formatCommunityCount, formatCommunityDate } from "./community-format";

type CommunityPostActionsProps = {
  post: CommunityPostDetail;
};

export function CommunityPostActions({ post }: CommunityPostActionsProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [likedByMe, setLikedByMe] = useState(post.likedByMe);
  const [likeCount, setLikeCount] = useState(post.likeCount);
  const [error, setError] = useState<string | null>(null);

  const likeMutation = useMutation({
    mutationFn: async () => {
      return likedByMe
        ? unlikeCommunityPost(post.id)
        : likeCommunityPost(post.id);
    },
    onMutate: () => {
      setError(null);
      setLikedByMe((current) => !current);
      setLikeCount((current) => current + (likedByMe ? -1 : 1));
    },
    onError: (mutationError) => {
      setLikedByMe(post.likedByMe);
      setLikeCount(post.likeCount);
      setError(errorMessage(mutationError));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["community", post.id] });
      router.refresh();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteCommunityPost(post.id),
    onMutate: () => setError(null),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["community"] });
      router.push("/community");
      router.refresh();
    },
    onError: (mutationError) => setError(errorMessage(mutationError)),
  });

  const handleDelete = () => {
    if (window.confirm("게시글을 삭제할까요? 삭제 후 목록에서 보이지 않습니다.")) {
      deleteMutation.mutate();
    }
  };

  return (
    <div className="mt-main flex flex-wrap items-center justify-between gap-main border-t border-main-light-gray pt-main">
      <div className="flex flex-wrap items-center gap-2">
        <button
          className={likedByMe ? activeButtonClass : neutralButtonClass}
          disabled={likeMutation.isPending}
          onClick={() => likeMutation.mutate()}
          type="button"
        >
          <ThumbsUp size={15} /> 좋아요 {formatCommunityCount(likeCount)}
        </button>
        {post.canEdit ? (
          <Link className={neutralButtonClass} href={`/community/${post.id}/edit`}>
            <Pencil size={15} /> 수정
          </Link>
        ) : null}
        {post.canDelete ? (
          <button
            className="inline-flex items-center gap-1 rounded-main bg-red-50 px-main py-2 text-sm-custom font-semibold text-red-600 disabled:opacity-50"
            disabled={deleteMutation.isPending}
            onClick={handleDelete}
            type="button"
          >
            <Trash2 size={15} /> 삭제
          </button>
        ) : null}
      </div>
      {error ? <p className="text-sm-custom text-main-red">{error}</p> : null}
    </div>
  );
}

type CommunityCommentsProps = {
  postId: number;
  initialComments: CommunityCommentList | null;
};

export function CommunityComments({ postId, initialComments }: CommunityCommentsProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);

  const createMutation = useMutation({
    mutationFn: () => createCommunityComment(postId, content.trim()),
    onMutate: () => setError(null),
    onSuccess: () => {
      setContent("");
      queryClient.invalidateQueries({ queryKey: ["community", postId, "comments"] });
      router.refresh();
    },
    onError: (mutationError) => setError(errorMessage(mutationError)),
  });

  const deleteMutation = useMutation({
    mutationFn: (commentId: number) => deleteCommunityComment(postId, commentId),
    onMutate: () => setError(null),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["community", postId, "comments"] });
      router.refresh();
    },
    onError: (mutationError) => setError(errorMessage(mutationError)),
  });

  const canSubmit = content.trim().length > 0 && content.trim().length <= 1000;

  return (
    <section className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
      <div className="flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-xl-custom font-bold text-main-dark-gray">
          <MessageCircle size={18} /> 댓글
        </h2>
        <span className="text-xs-custom text-main-dark-gray/50">
          {formatCommunityCount(initialComments?.page.totalElements ?? 0)}개
        </span>
      </div>

      <div className="mt-main grid gap-2">
        <textarea
          className="min-h-24 rounded-main border border-main-light-gray bg-white p-main text-sm-custom outline-none transition-colors focus:border-main-blue/50"
          maxLength={1000}
          onChange={(event) => setContent(event.target.value)}
          placeholder="댓글을 입력하세요."
          value={content}
        />
        <div className="flex items-center justify-between gap-main">
          <span className="text-xs-custom text-main-dark-gray/45">
            {content.trim().length}/1000
          </span>
          <button
            className="rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white disabled:bg-main-light-gray disabled:text-main-dark-gray/35"
            disabled={!canSubmit || createMutation.isPending}
            onClick={() => createMutation.mutate()}
            type="button"
          >
            댓글 등록
          </button>
        </div>
      </div>

      {error ? <p className="mt-3 text-sm-custom text-main-red">{error}</p> : null}

      {initialComments?.comments.length ? (
        <div className="mt-main grid gap-3">
          {initialComments.comments.map((comment) => (
            <div key={comment.id} className="rounded-main bg-main-light-gray/30 p-main">
              <div className="flex items-center justify-between gap-main">
                <span className="font-semibold text-main-dark-gray">{comment.authorNickname}</span>
                <div className="flex items-center gap-2">
                  <span className="text-xs-custom text-main-dark-gray/45">
                    {formatCommunityDate(comment.createdAt)}
                  </span>
                  {comment.canDelete ? (
                    <button
                      className="text-xs-custom font-semibold text-main-red disabled:opacity-50"
                      disabled={deleteMutation.isPending}
                      onClick={() => deleteMutation.mutate(comment.id)}
                      type="button"
                    >
                      삭제
                    </button>
                  ) : null}
                </div>
              </div>
              <p className="mt-2 whitespace-pre-wrap text-sm-custom leading-6 text-main-dark-gray/70">
                {comment.content}
              </p>
            </div>
          ))}
        </div>
      ) : (
        <div className="mt-main rounded-main bg-main-light-gray/35 p-main text-sm-custom text-main-dark-gray/55">
          아직 댓글이 없습니다. 첫 댓글을 남겨보세요.
        </div>
      )}
    </section>
  );
}

const neutralButtonClass =
  "inline-flex items-center gap-1 rounded-main bg-main-light-gray/45 px-main py-2 text-sm-custom font-semibold text-main-dark-gray/65 transition-colors hover:text-main-blue disabled:opacity-50";
const activeButtonClass =
  "inline-flex items-center gap-1 rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white transition-colors disabled:opacity-50";

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "요청을 처리하지 못했습니다.";
}
