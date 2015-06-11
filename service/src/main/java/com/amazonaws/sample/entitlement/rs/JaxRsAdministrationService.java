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

import com.amazonaws.sample.entitlement.exceptions.*;
import com.amazonaws.sample.entitlement.services.AdministrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;

import static javax.ws.rs.core.Response.ResponseBuilder;
import static javax.ws.rs.core.Response.Status;

/**
 * A POJO with a JAX-RS (Java API for REST-ful Web Services) annotated method to provide a REST-ful endpoint for requesting
 * administration tasks.
 */
@Component
@Path("/api/admin")
public class JaxRsAdministrationService {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    @Inject
    private AdministrationService administrationService;

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    /**
     * Get users
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @GET
    @Path("/users/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers(@HeaderParam("Authorization") String authorization ) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            String response = administrationService.getUsers();
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
     * Add user
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @POST
    @Path("/users/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUser(String user, @HeaderParam("Authorization") String authorization) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            String response = administrationService.addUser(user);
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
        } catch (UserBadIdentifierException e) {
            return response(Status.CONFLICT, e.getMessage());
        }
    }

    /**
     * Delete user
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @DELETE
    @Path("/user/{email}")
    public Response deleteUser(@PathParam("email") String email, @HeaderParam("Authorization") String authorization) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            administrationService.deleteUser(email);
            return response(Status.OK, "");
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
     * Get applications
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @GET
    @Path("/applications/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserApplications(@HeaderParam("Authorization") String authorization ) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            String response = administrationService.getUserApplications();
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
     * Add application
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @POST
    @Path("/applications/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUserApplication(String application, @HeaderParam("Authorization") String authorization) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            String response = administrationService.addUserApplication(application);
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
     * Delete application
     * @param authorization string that is associated to the identity of the requester
     */
    @DELETE
    @Path("/application/{id}")
    public Response deleteUserApplication(@PathParam("id") String id, @HeaderParam("Authorization") String authorization) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            administrationService.deleteUserApplication(id);
            return response(Status.OK, "");
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
     * Get subscriptions
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @GET
    @Path("/subscriptions/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserSubscriptions(@HeaderParam("Authorization") String authorization ) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            String response = administrationService.getUserSubscriptions();
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
     * Add subscription
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @POST
    @Path("/subscription/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUserSubscription(String subscription, @HeaderParam("Authorization") String authorization) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            String response = administrationService.addUserSubscription(subscription);
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
     * Delete subscription
     * @param authorization string that is associated to the identity of the requester
     */
    @DELETE
    @Path("/subscription/{userId}/{creationTimeMilli}")
    public Response deleteUserSubscription(@PathParam("userId") String userId, @PathParam("creationTimeMilli") Long creationTimeMilli, @HeaderParam("Authorization") String authorization) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            administrationService.deleteUserSubscription(userId, creationTimeMilli);
            return response(Status.OK, "");
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
     * Get sessions
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @GET
    @Path("/sessions/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserSessions(@HeaderParam("Authorization") String authorization ) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            String response = administrationService.getUserSessions();
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
     * Get configuration
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @GET
    @Path("/config/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfiguration(@HeaderParam("Authorization") String authorization ) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            String response = administrationService.getConfiguration();
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
     * Set configuration
     * @param authorization string that is associated to the identity of the requester
     * @return prettified JSON
     */
    @POST
    @Path("/config/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setConfiguration(String config, @HeaderParam("Authorization") String authorization) {
        try {
            administrationService.getUserFromAuthorization(authorization);
            String response = administrationService.setConfiguration(config);
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
