export interface StockData {
  acml_tr_pbmn: string;
  acml_vol: string;
  prdy_vrss: string;
  prdy_vrss_sign: string;
  stck_bsop_date: string;
  stck_clpr: string;
  stck_hgpr: string;
  stck_lwpr: string;
  stck_oprc: string;
}

export interface TestStockData {
  stockCode: string;
  date: [number, number, number];
  type: string | null;
  open: string;
  high: string;
  low: string;
  close: string;
  volume: string;
  volumeAmount: string;
  prevPrice: number;
  openFromPrev: number;
  closeFromPrev: number;
  highFromPrev: number;
  lowFromPrev: number;
}
