package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.feature.member.application.dto.GoogleSignupProfileCommand;

public record CompleteGoogleSignupRequest(
        String name,
        String nickname,
        String email,
        String phoneNumber,
        AddressRequest address,
        boolean agreement
) {
    GoogleSignupProfileCommand toCommand() {
        if (address == null) {
            throw new coin.coinzzickmock.common.error.CoreException(
                    coin.coinzzickmock.common.error.ErrorCode.INVALID_REQUEST);
        }
        return new GoogleSignupProfileCommand(
                name,
                nickname,
                email,
                phoneNumber,
                address.zipcode(),
                address.address(),
                address.addressDetail(),
                agreement
        );
    }

    public record AddressRequest(
            String zipcode,
            String address,
            String addressDetail
    ) {
    }
}
