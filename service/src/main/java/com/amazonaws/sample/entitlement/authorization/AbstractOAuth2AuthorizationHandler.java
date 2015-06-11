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
import org.apache.log4j.Logger;

/**
 * Class provides the OAuth2 workflow in the processAuthorization method. Implementations for specific OAuth2 providers
 * are handled by subclasses which provide the implementations for verifying the access token and retrieving
 * profile information that are specific to the vendor. Each vendor has specific request urls and response formats.
 */
public abstract class AbstractOAuth2AuthorizationHandler implements AuthorizationHandler {

    private Logger log = Logger.getLogger(AbstractOAuth2AuthorizationHandler.class.getName());

    /**
     * @param authorization
     *     An authorization string. The first part of the string must match the String returned from getAuthorizationType().
     *     The remainder of the string must be an OAuth2 access token.
     * @return an Identity containing the user id from the OAuth2 profile
     * @throws AuthorizationException
     */
    @Override
    public Identity processAuthorization(String authorization) throws AuthorizationException {
        String access_token = Util.checkAuthorizationString(getAuthorizationType(), authorization);
        log.info("Authorization string looks valid.");
        // Verify that the access token is valid and belongs to us.
        // If the access token can be verified and also profile information can be retrieved with one call to the oauth2
        // provider then return an Identity object here, otherwise return null and a separate call will be made
        // in getIdentity to retrieve the profile information and create an Identity object.
        Identity identity = verifyAccessToken(access_token);
        // fetch the profile info from the OAuth provider and return an Identity
        if (identity == null) {
            identity = getIdentity(access_token);
        }

        return identity;
    }

    /**
     * @return Boolean.FALSE always. OAuth2 authorization handlers will never bypass the authorization check.
     */
    public Boolean shouldAlwaysEntitle() {
        return Boolean.FALSE;
    }

    /**
     * Given an OAuth2 access token verify that the token is valid, has not expired and was issued for our application.
     * If the access token can be verified and also profile information can be retrieved with one call to the oauth2
     * provider then return an Identity object here, otherwise return null.
     * @param accessToken
     * @return an Identity object if possible, otherwise null
     * @throws AuthorizationException
     */
    abstract Identity verifyAccessToken(String accessToken) throws AuthorizationException;

    abstract String getAuthorizationType();

    /**
     * Given an OAuth2 access token, retrieve profile information and return an Identity that at minimum contains
     * a user id.
     * Provider specific subclasses need to override this only if verifyAccessToken cannot return an Identity object,
     * usually because the call to the provider API to verify the access token does not return profile information.
     * @param accessToken
     * @return an Identity object that contains at least a user id
     * @throws AuthorizationException
     */
    Identity getIdentity(String accessToken) throws AuthorizationException {
        throw new UnsupportedOperationException();
    }
}
