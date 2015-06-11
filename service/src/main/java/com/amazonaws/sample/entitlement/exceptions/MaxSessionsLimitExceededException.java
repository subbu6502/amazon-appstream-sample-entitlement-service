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


/**
 * Exception type thrown when maximum sessions
 * limit is exceeded when entitling a new session.
 */
public class MaxSessionsLimitExceededException
        extends Exception {

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public MaxSessionsLimitExceededException() {
        super("Maximum account sessions limit has been exceeded.");
    }
}
