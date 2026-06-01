export type UserInfo = {
  name: string;
  nickname: string;
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
