"use client";

import React from "react";
import * as Sentry from "@sentry/nextjs";
import Error from "../../app/error";

const SentryProvider = ({ children }: { children: React.ReactNode }) => {
  return (
    <Sentry.ErrorBoundary fallback={<Error />}>{children}</Sentry.ErrorBoundary>
  );
};

export default SentryProvider;
