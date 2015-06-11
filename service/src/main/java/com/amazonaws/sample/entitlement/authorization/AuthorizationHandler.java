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

/**
 * Interface that for various authorization implementations.
 */
public interface AuthorizationHandler {

    /**
     * Authorize a user. Once authorized the entitlement service can then determine if the user is entitled to an
     * AppStream {@link com.amazonaws.services.appstream.Application}.
     * @param authorization
     *     An authorization string. The format and contents of the string will vary depending upon the
     *     the AuthorizationHandler implementation in use.
     * @return an instance of {@link Identity} representing the identity of a user if authorization is successful
     * @throws AuthorizationException if authorization fails
     */
    Identity processAuthorization(String authorization)
            throws AuthorizationException;

    /**
     * @return
     *     true if the user should always been entitled to the application, false if the check should be performed
     *     for the user.
     */
    Boolean shouldAlwaysEntitle();
}
