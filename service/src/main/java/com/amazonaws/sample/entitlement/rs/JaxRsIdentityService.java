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

package com.amazonaws.sample.entitlement.rs;

import com.amazonaws.sample.entitlement.authorization.Identity;
import com.amazonaws.sample.entitlement.exceptions.AuthorizationException;
import com.amazonaws.sample.entitlement.services.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;

/**
 * A POJO with a JAX-RS (Java API for REST-ful Web Services) annotated method to provide a REST-ful endpoint for
 * Cognito Identity Id
 */
@Component
@Path("/api/identity")
public class JaxRsIdentityService {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    @Autowired
    private IdentityService identityService;

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    /**
     * Request Cognito Identity Id
     *
     * @param authorization string that is associated to the identity of a user
     * @return user
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIdentity(@HeaderParam("Authorization") String authorization) {
        try {
            Identity identity = identityService.getOpenIdTokenForDeveloperIdentity(authorization);
            return Response.ok(identity).build();
        } catch (AuthorizationException e) {
            String authenticateHeader = e.getAuthenticateHeader();
            if (authenticateHeader == null) {
                return response(Response.Status.UNAUTHORIZED, e.getMessage());
            } else {
                return response(Response.Status.UNAUTHORIZED, e.getMessage(), Collections.singletonMap("WWW-Authenticate", authenticateHeader));
            }
        }
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private Response response(Response.Status status, String message) {
        return Response.status(status).entity(message).type(MediaType.TEXT_PLAIN).build();
    }

    private Response response(Response.Status status, String message, Map<String, String> headers) {
        Response.ResponseBuilder responseBuilder = Response.status(status).entity(message).type(MediaType.TEXT_PLAIN);

        for (Map.Entry<String, String> header : headers.entrySet()) {
            responseBuilder.header(header.getKey(), header.getValue());
        }

        return responseBuilder.build();
    }
}
