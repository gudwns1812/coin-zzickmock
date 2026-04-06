"use client";

import useOutsideClick from "@/hooks/useOutsideClick";
import clsx from "clsx";
import { ChevronDown } from "lucide-react";
import React, { useRef, useEffect, useState } from "react";

interface DropdownProps {
  groups: string[];
  selected: string;
  onSelect: (group: string) => void;
  className?: string;
  textColor?: string;
  maxHeight?: number;
  outerTextSize?: number;
  innerTextSize?: number;
}

const Dropdown = ({
  groups,
  selected,
  onSelect,
  className,
  outerTextSize = 16,
  innerTextSize = 16,
  textColor = "text-main-dark-gray",
  maxHeight = 200,
}: DropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const buttonRefs = useRef<{ [group: string]: HTMLButtonElement | null }>({});

  useOutsideClick(wrapperRef, () => setIsOpen(false));

  useEffect(() => {
    if (isOpen && selected && buttonRefs.current[selected]) {
      buttonRefs.current[selected]?.focus();
    }
  }, [isOpen, selected]);

  return (
    <div className="relative" ref={wrapperRef}>
      {" "}
      {/* ✅ ref 위치 변경 */}
      <button
        className={clsx(
          "flex items-center gap-main rounded-main pl-3 pr-2",
          className
        )}
        onClick={() => setIsOpen((prev) => !prev)}
        type="button"
      >
        <span className={textColor} style={{ fontSize: `${outerTextSize}px` }}>
          {selected}
        </span>
        <ChevronDown
          size={20}
          className={clsx(
            "text-main-dark-gray transition-transform",
            isOpen ? "rotate-180" : ""
          )}
        />
      </button>
      <div
        className={clsx(
          "absolute top-full left-0 pt-[5px] transition-opacity duration-200 z-50",
          isOpen
            ? "opacity-100 pointer-events-auto"
            : "opacity-0 pointer-events-none"
        )}
      >
        <div
          className="w-fit bg-white rounded-main p-main flex flex-col items-start shadow-lg overflow-y-auto"
          style={{ maxHeight: `${maxHeight}px` }}
        >
          {groups.map((group) => (
            <button
              key={group}
              ref={(el) => {
                if (el) {
                  buttonRefs.current[group] = el;
                }
              }}
              type="button"
              onClick={() => {
                onSelect(group);
                setIsOpen(false);
              }}
              className={clsx(
                "w-full hover:bg-main-blue/10 rounded-main transition-colors duration-200 ease-in-out px-main py-1 text-start whitespace-nowrap",
                selected === group ? "font-bold text-main-blue" : ""
              )}
              style={{ fontSize: `${innerTextSize}px` }}
            >
              {group}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Dropdown;
