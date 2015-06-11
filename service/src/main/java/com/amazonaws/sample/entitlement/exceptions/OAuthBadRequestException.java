package com.amazonaws.sample.entitlement.exceptions;

public class OAuthBadRequestException extends AuthorizationException {

    public OAuthBadRequestException(String message, String authenticateHeader) {
        super(message, authenticateHeader);
    }

}
