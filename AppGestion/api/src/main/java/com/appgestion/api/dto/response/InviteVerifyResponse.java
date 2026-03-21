package com.appgestion.api.dto.response;

public record InviteVerifyResponse(boolean valid, String email) {
    public static InviteVerifyResponse invalid() {
        return new InviteVerifyResponse(false, null);
    }
}
