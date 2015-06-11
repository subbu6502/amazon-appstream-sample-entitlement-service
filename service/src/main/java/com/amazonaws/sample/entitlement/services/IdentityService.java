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

package com.amazonaws.sample.entitlement.services;

import com.amazonaws.sample.entitlement.authorization.AuthorizationHandler;
import com.amazonaws.sample.entitlement.authorization.Identity;
import com.amazonaws.sample.entitlement.exceptions.AuthorizationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Provides methods related to authorization of clients
 */
@Component
public class IdentityService {

    // @see http://tools.ietf.org/html/rfc6585#section-4
    private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    @Inject private AuthorizationHandler authorizationHandler;

    private Logger log = Logger.getLogger(EntitlementService.class.getName());
    private ObjectMapper mapper = new ObjectMapper();

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    /**
     * Given an authorization string return an OpenId token and Developer Cognito Identity Id.
     * @param authorization value from authorization header
     * @return a String with JSON object with IdentityId and Token
     * @throws AuthorizationException
     */
    public Identity getOpenIdTokenForDeveloperIdentity(String authorization)
            throws AuthorizationException {
        if (authorization == null) {
            throw new AuthorizationException("Missing Authorization header.");
        }

        Identity thirdPartyIdentity = authorizationHandler.processAuthorization(authorization);

        return thirdPartyIdentity;
    }

}