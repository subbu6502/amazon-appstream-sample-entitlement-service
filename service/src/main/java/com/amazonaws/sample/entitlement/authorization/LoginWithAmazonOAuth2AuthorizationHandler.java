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
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * AuthorizationHandler implementation that uses Login With Amazon to authorize a Login With Amazon access token.
 * Use of this AuthorizationHandler requires that the client already be integrated with Login With Amazon.
 */
@Component
@Qualifier("amazon")
public class LoginWithAmazonOAuth2AuthorizationHandler extends AbstractOAuth2AuthorizationHandler {

    /**
     * loginWithAmazonOAuthClientId is required.
     * The client id for this application as registered with the third-party.
     */

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------
    @Autowired
    @Qualifier("configuration")
    private Properties cognitoProperties;

    private Map<String, String> lwaErrorMessages;

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    private static final String AUTHORIZATION_TYPE = "AmazonOAuth2";
    private static final String PROVIDER_NAME = "Login With Amazon";

    //-------------------------------------------------------------
    // Constructors - public
    //-------------------------------------------------------------

    public LoginWithAmazonOAuth2AuthorizationHandler() {
        lwaErrorMessages = new HashMap<String, String>();
        lwaErrorMessages.put("invalid_request", "The request is missing a required parameter, has an invalid value, or is otherwise improperly formed.");
        lwaErrorMessages.put("invalid_token", "Token provided is invalid or has expired.");
        lwaErrorMessages.put("insufficient_scope", "The access token provided does not have access to the required scope.");
    }

    //-------------------------------------------------------------
    // Methods - Package Private
    //-------------------------------------------------------------

    /**
     * Exchange an access token for a map of user profile information.
     * @param accessToken
     * @return an Identity object containing the user's email
     * @throws AuthorizationException
     */
    @Override
    Identity getIdentity(String accessToken) throws AuthorizationException {
        try {
            // exchange the access token for user profile
            ResponseContent r = Request.Get("https://api.amazon.com/user/profile")
                    .addHeader("Authorization", "bearer " + accessToken)
                    .execute()
                    .handleResponse(new AllStatusesContentResponseHandler());

            int statusCode = r.getStatusCode();

            Map<String, String> m = new ObjectMapper().readValue(r.getContent("{}"), new TypeReference<Object>(){});

            if (statusCode == HttpStatus.SC_OK) {
                if (!m.containsKey("email")) {
                    throw new RuntimeException("Expected response to include email but it does not.");
                }
                return new Identity(m.get("user_id"), m.get("email"));
            }
            if (statusCode == HttpStatus.SC_BAD_REQUEST || statusCode == HttpStatus.SC_UNAUTHORIZED) {
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
     * Verify that the access token is valid and belongs to us.
     * @param accessToken Login with Amazon access token
     * @throws AuthorizationException
     */
    @Override
    Identity verifyAccessToken(String accessToken) throws AuthorizationException {
        try {
            ResponseContent r = Request.Get("https://api.amazon.com/auth/o2/tokeninfo?access_token="
                    + URLEncoder.encode(accessToken, "UTF-8"))
                    .execute()
                    .handleResponse(new AllStatusesContentResponseHandler());

            int statusCode = r.getStatusCode();

            Map<String, String> m = new ObjectMapper().readValue(r.getContent("{}"), new TypeReference<Object>(){});

            if (statusCode == HttpStatus.SC_OK) {
                if (!this.getOauthClientId().equals(m.get("aud"))) {
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

    @Override
    String getAuthorizationType() {
        return AUTHORIZATION_TYPE;
    }

    String getOauthClientId() {
        return this.cognitoProperties.getProperty("loginWithAmazonOAuthClientId");
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private String buildErrorMessageFromResponse(Map<String, String> m, int statusCode) {
        String message;
        String errorCode = m.get("message");

        if (errorCode == null) {
            message = "Bad " + PROVIDER_NAME + " request. Status code: " + statusCode;
        } else {
            message = lwaErrorMessages.get(errorCode);
            if (message == null) {
                if (m.containsKey("error_description")) {
                    message = m.get("error_description");
                } else {
                    message = PROVIDER_NAME + " error code: " + errorCode + ". Status code: " + statusCode;
                }
            }
        }

        return message;
    }

}
