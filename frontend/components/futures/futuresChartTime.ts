import { TickMarkType, type Time } from "lightweight-charts";

export const KST_TIME_ZONE = "Asia/Seoul";

const KST_PART_FORMATTER = new Intl.DateTimeFormat("en-US", {
  day: "2-digit",
  hour: "2-digit",
  hourCycle: "h23",
  minute: "2-digit",
  month: "2-digit",
  second: "2-digit",
  timeZone: KST_TIME_ZONE,
  year: "numeric",
});

type KstParts = {
  day: string;
  hour: string;
  minute: string;
  month: string;
  second: string;
  year: string;
};

export function formatChartTimeInKst(time: Time): string {
  const parts = getKstParts(time);

  if (!parts) {
    return "";
  }

  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}:${parts.second} UTC+9`;
}

export function formatChartTickInKst(
  time: Time,
  tickMarkType: TickMarkType
): string | null {
  const parts = getKstParts(time);

  if (!parts) {
    return null;
  }

  switch (tickMarkType) {
    case TickMarkType.Year:
      return parts.year;
    case TickMarkType.Month:
      return `${parts.month}월`;
    case TickMarkType.DayOfMonth:
      return `${parts.month}/${parts.day}`;
    case TickMarkType.Time:
      return `${parts.hour}:${parts.minute}`;
    case TickMarkType.TimeWithSeconds:
      return `${parts.hour}:${parts.minute}:${parts.second}`;
  }

  return null;
}

function getKstParts(time: Time): KstParts | null {
  const timestampMs = toTimestampMs(time);

  if (timestampMs == null) {
    return null;
  }

  const values = new Map(
    KST_PART_FORMATTER
      .formatToParts(timestampMs)
      .map((part) => [part.type, part.value])
  );

  return {
    day: values.get("day") ?? "",
    hour: values.get("hour") ?? "",
    minute: values.get("minute") ?? "",
    month: values.get("month") ?? "",
    second: values.get("second") ?? "",
    year: values.get("year") ?? "",
  };
}

function toTimestampMs(time: Time): number | null {
  if (typeof time === "number") {
    return time * 1000;
  }

  if (typeof time === "string") {
    const parsed = Date.parse(time);
    return Number.isNaN(parsed) ? null : parsed;
  }

  return Date.UTC(time.year, time.month - 1, time.day);
}
