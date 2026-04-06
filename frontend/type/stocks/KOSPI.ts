export interface KOSPI {
  prev: string; // 전일 종가
  sign: string; // 등락 부호
  prev_rate: string; // 전일 대비 등락률(%)
  indices: {
    bstp_nmix_hgpr: string; // KOSPI 지수(또는 종목)의 장중 최고가
    bstp_nmix_lwpr: string; // KOSPI 지수(또는 종목)의 장중 최저가
    bstp_nmix_prpr: string; // KOSPI 지수(또는 종목)의 종가(마감 가격)
    stck_bsop_date: string; // 기준 일자 (YYYYMMDD, 해당 데이터의 날짜)
  }[];
}
