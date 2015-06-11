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
import com.amazonaws.sample.entitlement.exceptions.UserNotFoundException;

public final class Util {

    //-------------------------------------------------------------
    // Constructors - private
    //-------------------------------------------------------------

    private Util() { }

    //-------------------------------------------------------------
    // Static methods - public
    //-------------------------------------------------------------

    public static String checkAuthorizationString(String expectedAuthorizationType, String authorization)
            throws AuthorizationException {

        String trimmedAuthorization = authorization.trim();

        if (!trimmedAuthorization.startsWith(expectedAuthorizationType)) {
            throw new AuthorizationException("Don't know how to handle authorization: " + authorization,
                    expectedAuthorizationType);
        }

        String token = trimmedAuthorization.substring(expectedAuthorizationType.length()).trim();

        if (token == null || token.length() == 0) {
            throw new UserNotFoundException(expectedAuthorizationType);
        }

        return token;
    }
}
