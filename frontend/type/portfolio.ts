export interface PortfolioData {
  portfolioStocks: {
    stockName: string;
    stockImage: string;
    stockCode: string;
    stockCount: number;
    entryPrice: number;
    currentPrice: number;
    profitLoss: number;
    profitLossRate: number;
  };
  totalAsset: number;
}
