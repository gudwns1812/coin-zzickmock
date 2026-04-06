"use client";

import React from "react";
import Calendar from "react-calendar";
import "react-calendar/dist/Calendar.css";
import "@/components/ui/calendar.css";

const ReactCalendar = ({
  selectedDate,
  setSelectedDate,
  content,
  minDate,
  maxDate,
  activeStartDate,
}: {
  selectedDate: Date | null;
  setSelectedDate: (value: Date | null) => void;
  content: (date: Date) => React.ReactNode;
  minDate: Date;
  maxDate: Date;
  activeStartDate?: Date;
}) => {
  return (
    <Calendar
      className="!w-full !border-none !bg-transparent"
      onChange={(value) => {
        if (value instanceof Date || value === null) {
          setSelectedDate(value);
        } else if (Array.isArray(value)) {
          setSelectedDate(value[0]);
        }
      }}
      value={selectedDate}
      calendarType="gregory"
      view="month"
      prev2Label={null}
      next2Label={null}
      minDate={minDate}
      maxDate={maxDate}
      showNeighboringMonth={false}
      formatDay={(locale, date) => String(date.getDate())}
      navigationLabel={({ date }) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        return `${year} ${month}`;
      }}
      tileContent={({ date }) => content(date)}
      activeStartDate={activeStartDate}
    />
  );
};

export default ReactCalendar;
