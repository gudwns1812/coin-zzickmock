"use client";

import Button from "@/components/ui/shared/Button";
import clsx from "clsx";

export type IntervalKey = "D" | "W" | "M" | "Y";

interface IntervalItem {
  value: string;
  label: string;
}

const CANDLE_INTERVALS: IntervalItem[] = [
  { value: "D", label: "일" },
  { value: "W", label: "주" },
  { value: "M", label: "월" },
  { value: "Y", label: "년" },
];

interface IntervalSelectorProps {
  selectedInterval: IntervalKey;
  onIntervalChange: (interval: IntervalKey) => void;
}

const IntervalSelector = ({
  selectedInterval,
  onIntervalChange,
}: IntervalSelectorProps) => {
  return (
    <div className="flex gap-2 mb-2">
      {CANDLE_INTERVALS.map((item) => (
        <Button
          key={item.value}
          variant={selectedInterval === item.value ? "primary" : "ghost"}
          className="!rounded-full"
          onClick={() => onIntervalChange(item.value as IntervalKey)}
        >
          {item.label}
        </Button>
      ))}
    </div>
  );
};

export default IntervalSelector;
export { CANDLE_INTERVALS };
