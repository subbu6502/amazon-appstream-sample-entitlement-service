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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.sample.entitlement.authorization.AuthorizationHandler;
import com.amazonaws.sample.entitlement.authorization.Identity;
import com.amazonaws.sample.entitlement.exceptions.ApplicationBadStateException;
import com.amazonaws.sample.entitlement.exceptions.ApplicationNotFoundException;
import com.amazonaws.sample.entitlement.exceptions.AuthorizationException;
import com.amazonaws.sample.entitlement.exceptions.MaxSessionsLimitExceededException;
import com.amazonaws.sample.entitlement.exceptions.SessionActiveException;
import com.amazonaws.sample.entitlement.exceptions.UserNotEntitledException;
import com.amazonaws.sample.entitlement.exceptions.UserNotFoundException;
import com.amazonaws.services.appstream.AppStream;
import com.amazonaws.services.appstream.Application;
import com.amazonaws.services.appstream.EntitleSessionInput;
import com.amazonaws.services.appstream.Session;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.util.json.JSONArray;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Iterator;

/**
 * Provides methods related to authorization, retrieval of AppStream {@link Application} objects, user entitlement and
 * {@link Session} management.
 */
@Component
public class EntitlementService {

    // @see http://tools.ietf.org/html/rfc6585#section-4
    private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

    @Inject
    public EntitlementService(ResourceIdResolver resourceIdResolver, DynamoDB dynamoDBDocument) {
        this.entitlementServiceUserTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceUser"));
        this.entitlementServiceUserSessionTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceUserSession"));
        this.entitlementServiceUserSubscriptionTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceUserSubscription"));
    }

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private Table entitlementServiceUserTable;
    private Table entitlementServiceUserSessionTable;
    private Table entitlementServiceUserSubscriptionTable;

    @Inject private AppStream appstream;
    @Inject private AuthorizationHandler authorizationHandler;

    @Value("${createUserWhenNew:false}") private boolean createUserWhenNew;
    @Value("${entitleAllWhenNew:false}") private boolean entitleAllWhenNew;

    private Logger log = Logger.getLogger(EntitlementService.class.getName());

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------


    /**
     * Given an authorization string (tied to a users identity), return a User object.
     * @param authorization value from authorization header
     * @return a User object
     * @throws AuthorizationException
     */
    public Item getUserFromAuthorization(String authorization)
            throws AuthorizationException {
        if (authorization == null) {
            throw new AuthorizationException("Missing Authorization header.");
        }

        Identity thirdPartyIdentity = authorizationHandler.processAuthorization(authorization);

        Item user = entitlementServiceUserTable.getItem("id", thirdPartyIdentity.getId());
        log.info(thirdPartyIdentity.getId());

        if (user == null) {
            if (!createUserWhenNew) {
                log.warn("No such user: " + thirdPartyIdentity.getId());
                throw new UserNotFoundException();
            }
            user.withString("id", thirdPartyIdentity.getId());
            user.withString("email", thirdPartyIdentity.getEmail());
            user = entitlementServiceUserTable.putItem(user).getItem();
        }

        return user;
    }

    public Boolean shouldAlwaysEntitle() {
        return authorizationHandler.shouldAlwaysEntitle();
    }


    /**
     * Retrieve an Application object from the AppStream service.
     * @param applicationId AppStream application id
     * @return an AppStream Application
     * @throws ApplicationNotFoundException
     */
    public Application getApplication(String applicationId)
            throws ApplicationNotFoundException {
        try {
            return appstream.getApplications().getById(applicationId);
        } catch (Exception e) {
            log.error(e);

            throw new ApplicationNotFoundException("The application identified by " + applicationId + " was not found.");
        }
    }

    /**
     * Return applications user is entitled to run
     * @param user the user the current request is for
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String getSubscriptions(Item user)
            throws ApplicationBadStateException {
        try {
            QuerySpec querySpec = new QuerySpec()
                    .withHashKey("UserId", user.getString("id"));
            ItemCollection<QueryOutcome> items = entitlementServiceUserTable.query(querySpec);
            JSONArray allItems = new JSONArray();
            // Process each page of results
            for (Page<Item, QueryOutcome> page : items.pages()) {
                // Process each item on the current page
                Iterator<Item> item = page.iterator();
                while (item.hasNext()) {
                    allItems.put(item.next().toJSON());
                }
            }
            // String jsonString= allItems.toString();
            return allItems.toString();
        } catch (AmazonServiceException e) {
            // Make the exception just a bit more obvious with an informational message.
            // It may not be obvious where the error occurred with just a stack trace.
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Return applications user is running (connected or not)
     * @param user the user the current request is for
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String getUserSessions(Item user)
            throws ApplicationBadStateException {
        try {
            QuerySpec querySpec = new QuerySpec()
                    .withHashKey("UserId", user.getString("id"));
            ItemCollection<QueryOutcome> items = entitlementServiceUserTable.query(querySpec);
            JSONArray allItems = new JSONArray();
            // Process each page of results
            for (Page<Item, QueryOutcome> page : items.pages()) {
                // Process each item on the current page
                Iterator<Item> item = page.iterator();
                while (item.hasNext()) {
                    // allItems.put(item.next());
                    allItems.put(item.next().toJSON());
                }
            }
            String jsonString= allItems.toString();
            return jsonString;
        } catch (AmazonServiceException e) {
            // Make the exception just a bit more obvious with an informational message.
            // It may not be obvious where the error occurred with just a stack trace.
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Return new UserSession
     * @param user the user the current request is for
     * @param userSubscription the current request is for
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String startUserSession(String userSubscription, Item user)
            throws UserNotEntitledException, ApplicationBadStateException, MaxSessionsLimitExceededException {
        try {
            Session session;
            Application application;
            Item retrievedSubscription;
            try {
                // user input subscription
                Item inputSubscription = Item.fromJSON(userSubscription);
                // substitute verified user id into query
                retrievedSubscription = entitlementServiceUserSubscriptionTable.getItem(new GetItemSpec()
                    .withPrimaryKey(
                            "UserId", user.getString("id"),
                            "CreationTimeMilli", inputSubscription.getLong("CreationTimeMilli"))
                );
                if (retrievedSubscription.getLong("TotalCombinedSessionTimeLimitMilli") < 0) {
                    throw new UserNotEntitledException("You do not have remaining time for this subscription.");
                }
            } catch (Error e) {
                log.error("Couldn't retrieve subscription provided. Error: " + e);
                throw new UserNotEntitledException("You are not currently allowed to use this application.  Please ask for access.");
            }
            try {
                application = getApplication(retrievedSubscription.getString("AppStreamApplicationId"));
                session = entitleSessionV2(application);
            } catch (ApplicationNotFoundException e) {
                log.error(e);
                throw new ApplicationBadStateException("Could not connect subscription to AppStream Application");
            } catch (MaxSessionsLimitExceededException e) {
                log.error(e);
                throw e;
            }
           // save session
            Item userSession = new Item()
                    .withPrimaryKey("UserId", user.getString("id"), "CreationTimeMilli", Instant.now().toEpochMilli())
                    .withLong("UserSubscriptionCreationTimeMilli", retrievedSubscription.getLong("CreationTimeMilli"))
                    .withString("Email", user.getString("email"))
                    .withString("UserApplicationId", retrievedSubscription.getString("UserApplicationId"))
                    .withString("AppStreamSessionId", session.getId())
                    .withString("AppStreamApplicationId", retrievedSubscription.getString("AppStreamApplicationId"))
                    .withString("UserApplicationName", retrievedSubscription.getString("UserApplicationName"))
                    .withString("UserApplicationDescription", retrievedSubscription.getString("UserApplicationDescription"))
                    .withLong("PerSessionTimeLimitMilli", retrievedSubscription.getLong("PerSessionTimeLimitMilli"))
                    .withString("AppStreamEntitlementUrl", session.getEntitlementUrl())
                    .withLong("AppStreamEntitlementUrlValidTimeMilli", 350000)
               ;
            entitlementServiceUserSessionTable.putItem(userSession).getItem();
           // send back session
           return userSession.toJSONPretty();
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Return a Session from the AppStream service for a User and Application.
     * @param application the application the current request is for
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return an AppStream entitled Session
     */
    public Session entitleSessionV2(Application application)
            throws ApplicationBadStateException, MaxSessionsLimitExceededException {
        Session session;

        try {
            EntitleSessionInput entitleSessionInput = new EntitleSessionInput();

            // Could optionally pass opaque data to the application that will service the session.
            // entitleSessionInput.setOpaqueData("some application-specific data");

            session = application.entitleSession(entitleSessionInput);
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred in the AppStream service while entitling a session for app: " + application.getId(), e);
            } else if (ErrorType.Client == e.getErrorType()) {
                if (e.getStatusCode() == HttpStatus.SC_CONFLICT) {
                    throw new ApplicationBadStateException(e.getMessage());
                } else if (e.getStatusCode() == HTTP_STATUS_TOO_MANY_REQUESTS) { // maximum sessions limit reached
                    throw new MaxSessionsLimitExceededException();
                }
            }

            throw e;
        }

        return session;
    }

    /**
     * Return a Session from the AppStream service for a User and Application. Sample DES V1
     * @param application the application the current request is for
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return an AppStream entitled Session
     */
    public Session entitleSessionV1(Application application)
            throws ApplicationBadStateException, MaxSessionsLimitExceededException {
        Session session;
        try {
            EntitleSessionInput entitleSessionInput = new EntitleSessionInput();
            session = application.entitleSession(entitleSessionInput);
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred in the AppStream service while entitling a session for app: " + application.getId(), e);
            } else if (ErrorType.Client == e.getErrorType()) {
                if (e.getStatusCode() == HttpStatus.SC_CONFLICT) {
                    throw new ApplicationBadStateException(e.getMessage());
                } else if (e.getStatusCode() == HTTP_STATUS_TOO_MANY_REQUESTS) { // maximum sessions limit reached
                    throw new MaxSessionsLimitExceededException();
                }
            }

            throw e;
        }

        return session;
    }


}
