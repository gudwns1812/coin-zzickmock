import React from "react";
import styles from "./Bookmark.module.css";
import clsx from "clsx";

interface BookmarkProps {
  rank?: number;
  color?: string;
  className?: string;
}

const rankColors: Record<number, string> = {
  1: "rgb(255, 215, 0)", // 금
  2: "rgb(192, 192, 192)", // 은
  3: "rgb(205, 127, 50)", // 동
};

const Bookmark = ({ rank = 1, color = "red", className }: BookmarkProps) => {
  const borderColor = rankColors[rank ?? 0] || color;

  if (rank >= 1 && rank <= 3) {
    return (
      <div className={className}>
        <div className={"relative w-fit"}>
          <div
            className={clsx(
              styles.bookmark,
              rank === 1 && "drop-shadow-[0_0_3px_rgba(255,215,0,0.5)]",
              rank === 2 && "drop-shadow-[0_0_3px_rgba(192,192,192,0.5)]",
              rank === 3 && "drop-shadow-[0_0_3px_rgba(205,127,50,0.5)]"
            )}
            style={{
              borderLeftColor: borderColor,
              borderRightColor: borderColor,
            }}
          />

          <span
            className={clsx(
              styles.rank,
              "absolute top-0 left-1/2 -translate-x-1/2 text-sm-custom text-white font-semibold"
            )}
          >
            {rank}
          </span>
        </div>
      </div>
    );
  }
};

export default Bookmark;
