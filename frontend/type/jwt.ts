export interface JwtToken {
  memberId: string;
  memberName: string;
  email: string;
  sub: string; // "ACCESS_TOKEN"
  iat: number;
  exp: number;

  // 아래 필드들은 이전 버전과의 호환성 또는 향후 확장을 위해 선택사항(Optional)으로 둡니다.
  // 실제 백엔드 JWT payload에는 현재 포함되어 있지 않습니다.
  Address?: string;
  AddressDetail?: string;
  invest?: number;
  phoneNumber?: string;
  zipCode?: string;
}
