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


import java.time.Instant;

/**
 * An entity that represents the identity of a user.
 *
 * An <code>Identity</code> object is returned upon a successful authorization by an {@link AuthorizationHandler}
 * implementation.
 *
 * The only attribute that is guaranteed to exist is the id.
 */
public class Identity {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String id;
    private String email;
    private String token;
    private Long expires;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public Identity(String id) {
        this.id = id;
    }

    public Identity(String id, String email) {
        this.id = id;
        this.email = email;
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpires() { return expires; }

    public void setExpires(Long expires) { this.expires = expires; }

}
