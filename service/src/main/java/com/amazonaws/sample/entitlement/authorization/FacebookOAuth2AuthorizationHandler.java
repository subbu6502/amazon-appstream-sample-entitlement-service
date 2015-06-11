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
import com.amazonaws.sample.entitlement.exceptions.OAuthBadRequestException;
import com.amazonaws.sample.entitlement.exceptions.OAuthBadTokenException;
import com.amazonaws.sample.entitlement.http.AllStatusesContentResponseHandler;
import com.amazonaws.sample.entitlement.http.ResponseContent;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;

/**
 * Implementations of verifyAccessToken and getIdentity that are specific to Facebook's OAuth2 implementation.
 */
@Component
@Qualifier("facebook")
public class FacebookOAuth2AuthorizationHandler extends AbstractOAuth2AuthorizationHandler {

    /**
     * loginWithFacebookClientId and loginWithFacebookOAuthClientToken are required.
     * The client id and token for this application as registered with the third-party.
     */

    @Autowired
    @Qualifier("configuration")
    private Properties cognitoProperties;

    private Logger log = Logger.getLogger(FacebookOAuth2AuthorizationHandler.class.getName());

/*    @Autowired
    public FacebookOAuth2AuthorizationHandler(){
        String hmacOAuth2Token;
        try {
            String loginWithFacebookOAuthClientId = cognitoProperties.getProperty("loginWithFacebookOAuthClientId");
            String loginWithFacebookOAuthClientToken = cognitoProperties.getProperty("loginWithFacebookOAuthClientToken");
            SecretKeySpec key = new SecretKeySpec(loginWithFacebookOAuthClientToken.getBytes("UTF-8"), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] bytes = mac.doFinal(loginWithFacebookOAuthClientId.getBytes("UTF-8"));
            hmacOAuth2Token = new String(bytes, "UTF-8");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new ApplicationContextException("Could not create SHA-256 digest of OAuth2 token");
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new ApplicationContextException("Could not create SHA-256 digest of OAuth2 token");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new ApplicationContextException("Could not create SHA-256 digest of OAuth2 token");
        }
    }*/

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    /**
     * The authorization string provided to {@link #processAuthorization(String)} must start with this value.
     */
    static final String AUTHORIZATION_TYPE = "FacebookOAuth2";
    static final String PROVIDER_NAME = "Login with Facebook";

    //-------------------------------------------------------------
    // Methods - Package Private
    //-------------------------------------------------------------

    /**
     * Verify acccess_token via the Facebook graph API.
     * @param accessToken
     * @return null because we do not get enough information from the graph API request for an Identity
     * @throws AuthorizationException
     */
    @Override
    Identity verifyAccessToken(String accessToken) throws AuthorizationException {
        try {
            ResponseContent r = Request.Get("https://graph.facebook.com/app?"
                    + "access_token=" + URLEncoder.encode(accessToken, "UTF-8")
                    // + "&"
                    // + "appsecret_proof=" + URLEncoder.encode(hmacOAuth2Token, "UTF-8")
                )
                    .execute()
                    .handleResponse(new AllStatusesContentResponseHandler());

            int statusCode = r.getStatusCode();

            Map<String, Object> m = new ObjectMapper().readValue(r.getContent("{}"), new TypeReference<Object>(){});

            if (statusCode == HttpStatus.SC_OK) {
                // For Facebook compare the "id" field against the client id (app id for facebook)
                if (!this.getOauthClientId().equals(m.get("id"))) {
                    // the access token is valid but it does not belong to us
                    throw new OAuthBadTokenException("access token is invalid", AUTHORIZATION_TYPE);
                }
                // The response does not contain enough information to create an Identity object so null is returned.
                return null;
            }
            if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                throw new OAuthBadRequestException(buildErrorMessageFromResponse(m, statusCode), AUTHORIZATION_TYPE);
            }
            if (statusCode >= 500) {
                throw new RuntimeException(PROVIDER_NAME + " encountered an error. Status code: " + statusCode);
            }

            throw new RuntimeException("Unanticipated response from " + PROVIDER_NAME + ". Status code: " + statusCode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetch profile information via the Facebook graph API and return an Identity object.
     * @param accessToken
     * @return an Identity object containing the user's email
     * @throws AuthorizationException
     */
    @Override
    Identity getIdentity(String accessToken) throws AuthorizationException {
        try {
            // exchange the access token for user profile
            ResponseContent r = Request.Get("https://graph.facebook.com/v2.3/me?access_token="
                    + URLEncoder.encode(accessToken, "UTF-8"))
                    .execute()
                    .handleResponse(new AllStatusesContentResponseHandler());

            int statusCode = r.getStatusCode();

            Map<String, Object> m = new ObjectMapper().readValue(r.getContent("{}"), new TypeReference<Object>(){});

            if (statusCode == HttpStatus.SC_OK) {
                if (!m.containsKey("email")) {
                    throw new RuntimeException("Expected response to include email but it does not.");
                }
                return new Identity(m.get("id").toString(), m.get("email").toString());
            }
            if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                throw new OAuthBadRequestException(buildErrorMessageFromResponse(m, statusCode), AUTHORIZATION_TYPE);
            }
            if (statusCode >= 500) {
                throw new RuntimeException(PROVIDER_NAME + " encountered an error. Status code: " + statusCode);
            }

            throw new RuntimeException("Unanticipated response from " + PROVIDER_NAME + ". Status code: " + statusCode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }    }

    @Override
    String getAuthorizationType() {
        return AUTHORIZATION_TYPE;
    }

    String getOauthClientId() {
        return cognitoProperties.getProperty("loginWithFacebookOAuthClientId");
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private String buildErrorMessageFromResponse(Map<String, Object> m, int statusCode) {
        StringBuilder message = new StringBuilder();
        Map errorMap = (Map) m.get("error");

        if (errorMap == null) {
            message.append("Bad ");
            message.append(PROVIDER_NAME);
            message.append(" request.");
        } else {
            if (errorMap.containsKey("message")) {
                message.append(errorMap.get("message"));
            }
            if (errorMap.containsKey("type")) {
                message.append(" Type: ");
                message.append(errorMap.get("type"));
            }
            if (errorMap.containsKey("code")) {
                message.append(" Code: ");
                message.append(errorMap.get("code"));
            }
            if (errorMap.containsKey("error_subcode")) {
                message.append(" Error Subcode: ");
                message.append(errorMap.get("code"));
            }
        }

        message.append(" Response status code: ");
        message.append(statusCode);

        return message.toString();
    }
}
