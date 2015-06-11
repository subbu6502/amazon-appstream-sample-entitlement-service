/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use
 * this file except in compliance with the License. A copy of the License is
 * located at:
 *
 *       http://aws.amazon.com/asl/
 *
 * This Software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.amazonaws.sample.entitlement.authorization;

import com.amazonaws.sample.entitlement.exceptions.AuthorizationException;
import com.amazonaws.services.cognitoidentity.*;
import com.amazonaws.services.cognitoidentity.model.GetOpenIdTokenForDeveloperIdentityRequest;
import com.amazonaws.services.cognitoidentity.model.GetOpenIdTokenForDeveloperIdentityResult;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * AuthorizationHandler implementation that uses Amazon CognitoIdentity to authorize a 3rd-party access token.
 * Important: When using this AuthorizationHandler you must register Public Identity Providers, OpenID Providers,
 * and/or your own Developer Authenticated Identities with Amazon Cognito
 */
@Component
@Primary
public class CognitoIdentityAuthorizationHandler extends AbstractOAuth2AuthorizationHandler {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------
    @Autowired
    private AmazonCognitoIdentityClient cognitoIdentityClient;
    @Autowired
    @Qualifier("amazon")
    private LoginWithAmazonOAuth2AuthorizationHandler loginWithAmazonAuthorizationHandler;
    @Autowired
    @Qualifier("facebook")
    private FacebookOAuth2AuthorizationHandler facebookAuthorizationHandler;
    @Autowired
    @Qualifier("google")
    private GoogleOAuth2AuthorizationHandler googleAuthorizationHandler;

    private Map<String, String> cognitoErrorMessages;

    private String awsCognitoIdentityPool;
    private String awsCognitoDeveloperProviderName;

    private Logger log = Logger.getLogger(CognitoIdentityAuthorizationHandler.class.getName());

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    private static final Map<String, String> AUTHORIZATION_TYPES = new HashMap<String, String>();
    static {
        AUTHORIZATION_TYPES.put("FacebookOAuth2", "graph.facebook.com");
        AUTHORIZATION_TYPES.put("GoogleOAuth2", "www.google.com");
        AUTHORIZATION_TYPES.put("AmazonOAuth2", "www.amazon.com");
    }

    //-------------------------------------------------------------

    // Constructors - public
    //-------------------------------------------------------------

    @Inject
    public CognitoIdentityAuthorizationHandler(Properties cognitoProperties) {
        this.awsCognitoDeveloperProviderName = cognitoProperties.getProperty("awsCognitoDeveloperProviderName");
        this.awsCognitoIdentityPool = cognitoProperties.getProperty("awsCognitoIdentityPool");
        cognitoErrorMessages = new HashMap<String, String>();
        cognitoErrorMessages.put("invalid_request", "The request is missing a required parameter, has an invalid value, or is otherwise improperly formed.");
        cognitoErrorMessages.put("invalid_token", "Token provided is invalid or has expired.");
        cognitoErrorMessages.put("insufficient_scope", "The access token provided does not have access to the required scope.");
    }

    //-------------------------------------------------------------
    // Methods - Package Private
    //-------------------------------------------------------------


    /**
     * @param authorization
     *     An authorization string. The first part of the string must match one of the keys in AUTHORIZATION_TYPES
     *     The remainder of the string must be an OAuth2 or OpenId access token.
     * @return an Identity containing the IdentityID from Amazon Cognito, and email, and name from the third-party
     * @throws AuthorizationException
     */
    @Override
    public Identity processAuthorization(String authorization) throws AuthorizationException {
        String authorizationType;
        Identity thirdPartyIdentity;
        String trimmedAuthorization = authorization.trim();
        try {
            String[] splitString = trimmedAuthorization.split("\\s");
            authorizationType = splitString[0];
        } catch (Exception e) {
            throw new AuthorizationException("Don't know how to handle authorization.");
        }
        if (!AUTHORIZATION_TYPES.containsKey(authorizationType)) {
            throw new AuthorizationException("Don't know how to handle authorization type: " + authorizationType);
        }
        Util.checkAuthorizationString(authorizationType, authorization);
        // Verify that the access token is valid and belongs to us.
        // If the access token can be verified and also profile information can be retrieved with one call to the oauth2
        // provider then return an Identity object here, otherwise return null and a separate call will be made
        // in getIdentity to retrieve the profile information and create an Identity object.
        switch (authorizationType) {
            case "FacebookOAuth2":
                thirdPartyIdentity = facebookAuthorizationHandler.processAuthorization(authorization);
                log.info("Email from Facebook: " + thirdPartyIdentity.getEmail());
                break;
            case "GoogleOAuth2":
                thirdPartyIdentity = googleAuthorizationHandler.processAuthorization(authorization);
                break;
            case "AmazonOAuth2":
                thirdPartyIdentity = loginWithAmazonAuthorizationHandler.processAuthorization(authorization);
                log.info("Email from Amazon: " + thirdPartyIdentity.getEmail());
                break;
            default:
                throw new AuthorizationException("Don't know how to handle authorization.");
        }
        try {
            Instant fifteenMinutesFromNow = Instant.now().plus(15, ChronoUnit.MINUTES);
            String base64EncEmail = Base64.getEncoder().withoutPadding().encodeToString(thirdPartyIdentity.getEmail().getBytes("utf-8"));
            GetOpenIdTokenForDeveloperIdentityRequest req = new GetOpenIdTokenForDeveloperIdentityRequest();
            req.setIdentityPoolId(awsCognitoIdentityPool);
            req.addLoginsEntry(awsCognitoDeveloperProviderName, base64EncEmail);
            GetOpenIdTokenForDeveloperIdentityResult res = cognitoIdentityClient.getOpenIdTokenForDeveloperIdentity(req);
            thirdPartyIdentity.setId(res.getIdentityId());
            thirdPartyIdentity.setToken(res.getToken());
            thirdPartyIdentity.setExpires(fifteenMinutesFromNow.toEpochMilli());
        }  catch (UnsupportedEncodingException e) {
            throw new AuthorizationException("Don't know how to handle authorization.");
        }
        return thirdPartyIdentity;
    }

    @Override
    public Boolean shouldAlwaysEntitle() {
        return null;
    }

    @Override
    String getAuthorizationType() {
        return "CognitoIdentity";
    }

    @Override
    Identity getIdentity(String accessToken) throws AuthorizationException {
        return new Identity("invalid");
    }

    /**
     * Verify that the access token is valid and belongs to us.
     * @param accessToken Login with Amazon access token
     * @throws AuthorizationException
     */
    @Override
    Identity verifyAccessToken(String accessToken) throws AuthorizationException {
        return new Identity("invalid");
    }

}