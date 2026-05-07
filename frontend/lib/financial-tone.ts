export type SignedFinancialTone = "positive" | "negative" | "neutral";

export function getSignedFinancialTone(value: number): SignedFinancialTone {
  if (value > 0) {
    return "positive";
  }

  if (value < 0) {
    return "negative";
  }

  return "neutral";
}

export function getSignedFinancialTextClassName(
  value: number,
  neutralClassName = "text-main-dark-gray"
): string {
  const tone = getSignedFinancialTone(value);

  if (tone === "positive") {
    return "text-emerald-600";
  }

  if (tone === "negative") {
    return "text-main-red";
  }

  return neutralClassName;
}

export function getSignedFinancialBadgeClassName(value: number): string {
  const tone = getSignedFinancialTone(value);

  if (tone === "positive") {
    return "bg-emerald-50 text-emerald-700";
  }

  if (tone === "negative") {
    return "bg-red-50 text-main-red";
  }

  return "bg-main-light-gray/45 text-main-dark-gray";
}
