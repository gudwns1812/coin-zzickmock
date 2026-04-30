import { PortfolioData } from "@/type/portfolio";

/**
 * 사용자 포트폴리오 가져오기
 */
export async function fetchUserPortfolio(account: string): Promise<{
  data: PortfolioData;
}> {
  const res = await fetch(`/proxy/v1/portfolios/${account}`);

  if (!res.ok) {
    throw new Error(`포트폴리오 로드 실패: ${res.status}`);
  }

  return res.json();
}

/**
 * 관심 종목 그룹 가져오기
 */
export async function fetchInterestGroups(account: string): Promise<{
  data: {
    groupId: string;
    groupName: string;
    main: boolean;
  }[];
}> {
  const res = await fetch(`/proxy/favorite/${account}`);

  if (!res.ok) {
    throw new Error(`관심 종목 그룹 로드 실패: ${res.status}`);
  }

  return res.json();
}

/**
 * 특정 그룹의 관심 종목 가져오기
 */
export async function fetchInterestStocks(
  account: string,
  groupId: string
): Promise<
  {
    stockCode: string;
    stockName: string;
    stockImage: string;
    currentPrice: string;
    changeRate: string;
    changeAmount: string;
    sign: string;
  }[]
> {
  const res = await fetch(`/proxy/favorite/${account}/${groupId}`);

  if (!res.ok) {
    throw new Error(`관심 종목 로드 실패: ${res.status}`);
  }

  return res.json();
}
