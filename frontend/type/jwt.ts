export interface JwtToken {
  Address: string;
  AddressDetail: string;
  email: string;
  exp: number;
  iat: number;
  invest: number;
  memberId: string;
  memberName: string;
  phoneNumber: string;
  sub: string; //"ACCESS_TOKEN"
  zipCode: string;
}
