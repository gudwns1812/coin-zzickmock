"use client";

import * as React from "react";
import {
  motion,
  useInView,
  type Transition,
  type UseInViewOptions,
} from "motion/react";
import { cn } from "@/lib/utils";

type WritingTextProps = Omit<React.ComponentProps<"span">, "children"> & {
  transition?: Transition;
  inView?: boolean;
  inViewMargin?: UseInViewOptions["margin"];
  inViewOnce?: boolean;
  spacing?: number | string;
  text: string;
  isGradient?: boolean;
};

function WritingText({
  ref,
  inView = false,
  inViewMargin = "0px",
  inViewOnce = true,
  spacing = 5,
  text,
  transition = { type: "spring", bounce: 0, duration: 2, delay: 0.5 },
  isGradient = false,
  className,
  ...props
}: WritingTextProps) {
  const localRef = React.useRef<HTMLSpanElement>(null);
  React.useImperativeHandle(ref, () => localRef.current as HTMLSpanElement);

  const inViewResult = useInView(localRef, {
    once: inViewOnce,
    margin: inViewMargin,
  });
  const isInView = !inView || inViewResult;

  const words = React.useMemo(() => text.split(" "), [text]);

  return (
    <span
      ref={localRef}
      data-slot="writing-text"
      className={cn(
        isGradient &&
          "bg-gradient-to-r from-main-blue to-purple-500 bg-clip-text text-transparent",
        className
      )}
      {...props}
    >
      {words.map((word, index) => (
        <motion.span
          key={index}
          className={cn(
            isGradient &&
              "bg-gradient-to-r from-main-blue to-purple-500 bg-clip-text text-transparent",
            "inline-block will-change-transform will-change-opacity"
          )}
          style={{ marginRight: spacing }}
          initial={{ opacity: 0, y: 10 }}
          animate={isInView ? { opacity: 1, y: 0 } : undefined}
          transition={{
            ...transition,
            delay: index * (transition?.delay ?? 0),
          }}
        >
          {word}{" "}
        </motion.span>
      ))}
    </span>
  );
}

export { WritingText, type WritingTextProps };
