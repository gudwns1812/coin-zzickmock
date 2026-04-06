"use client";

import React, { useState } from "react";
import Calendar from "@/components/ui/Calendar";

const ProfitLossCalendar = () => {
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);

  return (
    <div className="size-full flex flex-col gap-main">
      <h2 className="text-2xl-custom font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent w-fit">
        날짜별 손익
      </h2>
      <Calendar
        selectedDate={selectedDate}
        setSelectedDate={setSelectedDate}
        content={() => <div>+123,243원</div>}
        activeStartDate={new Date()}
        minDate={new Date()}
        maxDate={new Date()}
      />
    </div>
  );
};

export default ProfitLossCalendar;
