export interface Popular {
  hts_kor_isnm: string; // 종목명
  mksc_shrn_iscd: string; // 종목코드
  data_rank: string; // 순위
  prdy_vrss_sign: string; // 전일대비 부호
  prdy_vrss: string; // 전일대비
  prdy_ctrt: string; // 전일대비율
  stockImage: string; // 종목 이미지
  stck_prpr: string; // 현재가
}

export interface TestPopular {
  stockName: string;
  stockCode: string;
  rank: string;
  price: string;
  sign: string;
  changeAmount: string;
  changeRate: string;
  stockImage: string | null;
}
