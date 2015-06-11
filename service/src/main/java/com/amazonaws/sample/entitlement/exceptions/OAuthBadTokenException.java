package com.amazonaws.sample.entitlement.exceptions;


public class OAuthBadTokenException extends AuthorizationException {

    public OAuthBadTokenException(String message, String authenticateHeader) {
        super(message, authenticateHeader);
    }

}
