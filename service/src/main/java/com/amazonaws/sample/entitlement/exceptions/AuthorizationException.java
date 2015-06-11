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

package com.amazonaws.sample.entitlement.exceptions;


public class AuthorizationException extends Exception {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String authenticateHeader;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public AuthorizationException(String message) {
        this(message, null);
    }


    public AuthorizationException(String message, String authenticateHeader) {
        super(message);

        this.authenticateHeader = authenticateHeader;
    }


    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    public String getAuthenticateHeader() {
        return this.authenticateHeader;
    }

}
