"use client";

import * as React from "react";
import { motion, type Transition } from "motion/react";
import { ChevronDown } from "lucide-react";

import { cn } from "@/lib/utils";
import {
  Disclosure,
  DisclosureButton,
  DisclosurePanel,
  type DisclosureProps,
  type DisclosureButtonProps,
  type DisclosurePanelProps,
} from "@/components/animate-ui/headless/disclosure";

type AccordionProps<TTag extends React.ElementType = "div"> =
  React.ComponentProps<TTag> & {
    children: React.ReactNode;
    as?: TTag;
  };

function Accordion<TTag extends React.ElementType = "div">({
  as: Component = "div",
  ...props
}: AccordionProps<TTag>) {
  return <Component data-slot="accordion" {...props} />;
}

type AccordionItemProps<TTag extends React.ElementType = "div"> =
  DisclosureProps<TTag> & {
    className?: string;
    as?: TTag;
  };

function AccordionItem<TTag extends React.ElementType = "div">(
  props: AccordionItemProps<TTag>
) {
  const { className, as = "div", ...rest } = props;

  return (
    <Disclosure
      data-slot="accordion-item"
      {...rest}
      as={as as React.ElementType}
      className={className}
    />
  );
}

type AccordionButtonProps<TTag extends React.ElementType = "button"> =
  DisclosureButtonProps<TTag> & {
    transition?: Transition;
    chevron?: boolean;
    className?: string;
    as?: TTag;
  };

function AccordionButton<TTag extends React.ElementType = "button">(
  props: AccordionButtonProps<TTag>
) {
  const {
    children,
    className,
    transition = { type: "spring", stiffness: 150, damping: 17 },
    chevron = true,
    ...rest
  } = props;

  return (
    <DisclosureButton
      data-slot="accordion-button"
      {...rest}
      className={cn(
        "flex w-full text-start flex-1 items-center justify-between py-4 font-medium",
        className
      )}
    >
      {(bag) => (
        <>
          {typeof children === "function" ? children(bag) : children}

          {chevron && (
            <motion.div
              data-slot="accordion-button-chevron"
              animate={{ rotate: bag.open ? 180 : 0 }}
              transition={transition}
            >
              <ChevronDown className="size-5 shrink-0" />
            </motion.div>
          )}
        </>
      )}
    </DisclosureButton>
  );
}

type AccordionPanelProps<TTag extends React.ElementType = "div"> =
  DisclosurePanelProps<TTag> & {
    as?: TTag;
  };

function AccordionPanel<TTag extends React.ElementType = "div">(
  props: AccordionPanelProps<TTag>
) {
  const { children, className, as = "div", ...rest } = props;

  return (
    <DisclosurePanel
      data-slot="accordion-panel"
      {...rest}
      as={as as React.ElementType}
    >
      {(bag) => (
        <div className={cn("pb-4 pt-0 text-sm", className)}>
          {typeof children === "function" ? children(bag) : children}
        </div>
      )}
    </DisclosurePanel>
  );
}

export {
  Accordion,
  AccordionItem,
  AccordionButton,
  AccordionPanel,
  type AccordionProps,
  type AccordionItemProps,
  type AccordionButtonProps,
  type AccordionPanelProps,
};
