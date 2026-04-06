export type UserInfo = {
  name: string;
  address: {
    zipcode: string;
    address: string;
    detail: string;
  };
  phone: {
    countryCode: string;
    phoneNumber1: string;
    phoneNumber2: string;
  };
  email: string;
  id?: string;
  password?: string;
  passwordConfirm?: string;
  agree?: boolean;
};
