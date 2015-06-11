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

import com.amazonaws.sample.entitlement.exceptions.ApplicationBadStateException;
import com.amazonaws.sample.entitlement.exceptions.ApplicationNotFoundException;
import com.amazonaws.sample.entitlement.exceptions.AuthorizationException;
import com.amazonaws.sample.entitlement.exceptions.MaxSessionsLimitExceededException;
import com.amazonaws.sample.entitlement.exceptions.UserNotEntitledException;
import com.amazonaws.sample.entitlement.services.EntitlementService;

import com.amazonaws.services.appstream.Application;
import com.amazonaws.services.appstream.Session;
import com.amazonaws.services.dynamodbv2.document.Item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.ResponseBuilder;
import static javax.ws.rs.core.Response.Status;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * A POJO with a JAX-RS (Java API for REST-ful Web Services) annotated method to provide a REST-ful endpoint for
 * administrative tasks.
 */
@Component
@Path("/api/entitlements")
public class JaxRsEntitlementService {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    @Autowired
    @Qualifier("configuration")
    private Properties cognitoProperties;

    @Autowired
    private EntitlementService entitlementService;

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    /**
     * Request entitlements for a user
     * @param authorization string that is associated to the identity of a user
     * @return Applications
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestSubscriptions(@HeaderParam("Authorization") String authorization ) {
        try {
            Item user = entitlementService.getUserFromAuthorization(authorization);
            String response = entitlementService.getSubscriptions(user);
            return response(Status.OK, response);
        } catch (AuthorizationException e) {
            String authenticateHeader = e.getAuthenticateHeader();

            if (authenticateHeader == null) {
                return response(Status.UNAUTHORIZED, e.getMessage());
            } else {
                return response(Status.UNAUTHORIZED, e.getMessage(), Collections.singletonMap("WWW-Authenticate", authenticateHeader));
            }
        } catch (ApplicationBadStateException e) {
            return response(Status.CONFLICT, e.getMessage());
        }
    }

    /**
     * Request sessions for a user
     * @param authorization string that is associated to the identity of a user
     * @return prettified JSON sessions
     */
    @GET
    @Path("/sessions/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestSessions(@HeaderParam("Authorization") String authorization ) {
        try {
            Item user = entitlementService.getUserFromAuthorization(authorization);
            String response = entitlementService.getUserSessions(user);
            return response(Status.OK, response);
        } catch (AuthorizationException e) {
            String authenticateHeader = e.getAuthenticateHeader();

            if (authenticateHeader == null) {
                return response(Status.UNAUTHORIZED, e.getMessage());
            } else {
                return response(Status.UNAUTHORIZED, e.getMessage(), Collections.singletonMap("WWW-Authenticate", authenticateHeader));
            }
        } catch (ApplicationBadStateException e) {
            return response(Status.CONFLICT, e.getMessage());
        }
    }

    /**
     * Request AppStream entitlement for a user
     * @param authorization string that is associated to the identity of a user
     * @return prettified JSON session
     */
    @POST
    @Path("/session/startsession")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSession(String userSubscription, @HeaderParam("Authorization") String authorization) {
        try {
            Item user = entitlementService.getUserFromAuthorization(authorization);
            String response = entitlementService.startUserSession(userSubscription, user);
            return response(Status.OK, response);
        } catch (AuthorizationException e) {
            String authenticateHeader = e.getAuthenticateHeader();
            if (authenticateHeader == null) {
                return response(Status.UNAUTHORIZED, e.getMessage());
            } else {
                return response(Status.UNAUTHORIZED, e.getMessage(), Collections.singletonMap("WWW-Authenticate", authenticateHeader));
            }
        } catch (UserNotEntitledException e) {
            return response(Status.FORBIDDEN, e.getMessage());
        } catch (ApplicationBadStateException e) {
            return response(Status.CONFLICT, e.getMessage());
        } catch (MaxSessionsLimitExceededException e) {
            return response(Status.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    /**
     * Request an entitlement to an application for a user. Sample DES V1
     * @param applicationId AppStream application id
     * @return an entitlement url
     */
    @POST
    @Path("/{applicationId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response requestEntitlement(@HeaderParam("Authorization") String authorization,
                                       @PathParam("applicationId") String applicationId,
                                       @FormParam("terminatePrevious") @DefaultValue("false") Boolean terminatePrevious) {

        if (cognitoProperties.getProperty("enableNoAuthV1EntitlementCondition") == "false" ) {
            return response(Status.METHOD_NOT_ALLOWED,"");
        }

        try {
            Application application = entitlementService.getApplication(applicationId);
            Session session = entitlementService.entitleSessionV1(application);
            return response(Status.CREATED, session.getEntitlementUrl());
        } catch (ApplicationNotFoundException e) {
            return response(Status.NOT_FOUND, e.getMessage());
        } catch (ApplicationBadStateException e) {
            return response(Status.CONFLICT, e.getMessage());
        } catch (MaxSessionsLimitExceededException e) {
            return response(Status.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private Response response(Status status, String message) {
        return Response.status(status).entity(message).type(MediaType.TEXT_PLAIN).build();
    }

    private Response response(Status status, String message, Map<String, String> headers) {
        ResponseBuilder responseBuilder = Response.status(status).entity(message).type(MediaType.TEXT_PLAIN);

        for (Map.Entry<String, String> header : headers.entrySet()) {
            responseBuilder.header(header.getKey(), header.getValue());
        }

        return responseBuilder.build();
    }
}
