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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;

/**
 * Implementations of verifyAccessToken and getIdentity that are specific to Google's OAuth2 implementation.
 */
@Component
@Qualifier("google")
public class GoogleOAuth2AuthorizationHandler extends AbstractOAuth2AuthorizationHandler {

    /**
     * loginWithGoogleClientId and loginWithGoogleClientSecret are required.
     * The client id and token for this application as registered with the third-party.
     */

    @Autowired
    @Qualifier("configuration")
    private Properties cognitoProperties;

    private Logger log = Logger.getLogger(GoogleOAuth2AuthorizationHandler.class.getName());

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    /**
     * The authorization string provided to {@link #processAuthorization(String)} must start with this value.
     */
    static final String AUTHORIZATION_TYPE = "GoogleOAuth2";
    static final String PROVIDER_NAME = "Login with Google";

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

            ResponseContent r = Request.Get("https://www.googleapis.com/userinfo/v2/me")
                    .addHeader("Authorization", "Bearer " + URLEncoder.encode(accessToken, "UTF-8"))
                    .execute()
                    .handleResponse(new AllStatusesContentResponseHandler());

            int statusCode = r.getStatusCode();
            log.info(statusCode);

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
        }
    }

    @Override
    String getAuthorizationType() {
        return AUTHORIZATION_TYPE;
    }

    String getOauthClientId() {
        return cognitoProperties.getProperty("loginWithGoogleClientId");
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
