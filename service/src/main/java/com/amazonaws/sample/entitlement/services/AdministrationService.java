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
import com.amazonaws.sample.entitlement.authorization.AuthorizationHandler;
import com.amazonaws.sample.entitlement.authorization.Identity;
import com.amazonaws.sample.entitlement.exceptions.*;
import com.amazonaws.services.appstream.*;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient;
import com.amazonaws.services.cognitoidentity.model.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.*;

/**
 * Provides methods related to authorization, retrieval of AppStream {@link com.amazonaws.services.appstream.Application} objects, user entitlement and
 * {@link com.amazonaws.services.appstream.Session} management.
 */
@Component
public class AdministrationService {

    private Table entitlementServiceUserTable;
    private Table entitlementServiceUserApplicationTable;
    private Table entitlementServiceUserSubscriptionTable;
    private Table entitlementServiceUserSessionTable;
    private Table entitlementServiceConfigurationTable;

    private String awsCognitoIdentityPool;
    private String awsCognitoDeveloperProviderName;

    @Inject
    public AdministrationService(ResourceIdResolver resourceIdResolver, Properties cognitoProperties, DynamoDB dynamoDBDocument) {
        this.entitlementServiceUserTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceUser"));
        this.entitlementServiceUserApplicationTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceUserApplication"));
        this.entitlementServiceUserSubscriptionTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceUserSubscription"));
        this.entitlementServiceUserSessionTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceUserSession"));
        this.entitlementServiceConfigurationTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceConfiguration"));
        this.awsCognitoDeveloperProviderName = cognitoProperties.getProperty("awsCognitoDeveloperProviderName");
        this.awsCognitoIdentityPool = cognitoProperties.getProperty("awsCognitoIdentityPool");
    }

    // @see http://tools.ietf.org/html/rfc6585#section-4
    private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    @Autowired
    @Qualifier("configuration")
    private Properties cognitoProperties;

    @Inject private AppStream appstream;
    @Inject private AuthorizationHandler authorizationHandler;
    @Inject private AmazonCognitoIdentityClient cognitoIdentityClient;


    // Since we have a PolicyBasedAuthorizationHandler, it is safe to default createUserWhenNew to true.
    // If the policy is Deny then the AuthorizationHandler will always check to see if the user is valid and when a
    // new user is created via the UI you still have to specify that the user is entitled to all applications.
    // If the policy is Allow then these both need to be true or else so a user can be created if necessary and
    // a new user will always be entitled to all applications.
    @Value("${createUserWhenNew:false}") private boolean createUserWhenNew;
    @Value("${entitleAllWhenNew:false}") private boolean entitleAllWhenNew;

    private Logger log = Logger.getLogger(AdministrationService.class.getName());

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
        // TODO have to verify the token only
        Identity thirdPartyIdentity = authorizationHandler.processAuthorization(authorization);

        //TODO  then we have to lookup by email
        Item user = entitlementServiceUserTable.getItem("id", thirdPartyIdentity.getId());

        if (user == null) {
            if (!createUserWhenNew) {
                log.warn("No such user: " + thirdPartyIdentity.getId());
                throw new UserNotFoundException();
            }
            user.withString("id", thirdPartyIdentity.getId());
            user.withString("email", thirdPartyIdentity.getEmail());
            entitlementServiceUserTable.putItem(user);
        }

        String role = user.getString("role");
        if (role == null || !role.equals("Administrator")) {
            log.info("An authorization exception occurred: User not assigned Administrator role.");
            throw new AuthorizationException("Not Authorized.");
        }

        return user;
    }

    /**
     * Get users
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String getUsers()
            throws ApplicationBadStateException {
        try {
            ItemCollection<ScanOutcome> items = entitlementServiceUserTable.scan(
                    // filter expression
                    null,
                    // projection expression
                    "id, email, #n, #r",
                    // attribute name substitution
                    new NameMap()
                            .with("#n", "name")
                            .with("#r", "role"),
                    // attribute value substitution
                    null
            );
            List<String> allItems = new ArrayList<>();
            // Process each page of results
            for (Page<Item, ScanOutcome> page : items.pages()) {
                // Process each item on the current page
                Iterator<Item> item = page.iterator();
                while (item.hasNext()) {
                    allItems.add(item.next().toJSON());
                }
            }
            return "[" + StringUtils.join(allItems, ",") + "]";
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Add user
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */
    public String addUser(String user)
            throws ApplicationBadStateException, UserBadIdentifierException {
        try {
            Item userItem = Item.fromJSON(user);
            String base64EncEmail = Base64.getEncoder().withoutPadding().encodeToString(userItem.getString("email").getBytes("utf-8"));
            GetOpenIdTokenForDeveloperIdentityRequest req = new GetOpenIdTokenForDeveloperIdentityRequest();
            req.setIdentityPoolId(awsCognitoIdentityPool);
            req.addLoginsEntry(awsCognitoDeveloperProviderName, base64EncEmail);
            GetOpenIdTokenForDeveloperIdentityResult res = cognitoIdentityClient.getOpenIdTokenForDeveloperIdentity(req);
            String cognitoIdentityId = res.getIdentityId();
            userItem.withString("id", cognitoIdentityId);
            entitlementServiceUserTable.putItem(userItem).getItem();
            return userItem.toJSONPretty();
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        } catch (UnsupportedEncodingException e) {
            String m = "An error occurred while encoding provided user's email address.";
            log.error(m + e);
            throw new UserBadIdentifierException(m);
        }
    }

    /**
     * Delete user
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     */
    public void deleteUser(String email)
            throws ApplicationBadStateException {
        try {
            String cognitoIdentityId;
            String base64EncEmail;
            try {
                base64EncEmail = Base64.getEncoder().withoutPadding().encodeToString(email.getBytes("utf-8"));
            }   catch (UnsupportedEncodingException e) {
                throw new ApplicationBadStateException("Don't know how to handle authorization.");
            }
            try {
                LookupDeveloperIdentityRequest req = new LookupDeveloperIdentityRequest()
                        .withMaxResults(1)
                        .withDeveloperUserIdentifier(base64EncEmail)
                        .withIdentityPoolId(awsCognitoIdentityPool);
                LookupDeveloperIdentityResult res = cognitoIdentityClient.lookupDeveloperIdentity(req);
                cognitoIdentityId = res.getIdentityId();
            }  catch (NotAuthorizedException e) {
                log.error("No Cognito Developer Identity exists for " + email + " " + e );
                throw new ApplicationBadStateException("");
            }
            UnlinkDeveloperIdentityRequest unlinkRequest = new UnlinkDeveloperIdentityRequest()
                    .withIdentityId(cognitoIdentityId)
                    .withDeveloperUserIdentifier(base64EncEmail)
                    .withIdentityPoolId(awsCognitoIdentityPool)
                    .withDeveloperProviderName(awsCognitoDeveloperProviderName);
            cognitoIdentityClient.unlinkDeveloperIdentity(unlinkRequest);
            entitlementServiceUserTable.deleteItem("id", cognitoIdentityId);
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Get applications
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String getUserApplications()
            throws ApplicationBadStateException {
        try {
            ItemCollection<ScanOutcome> items = entitlementServiceUserApplicationTable.scan(
                    // filter expression
                    null,
                    // projection expression
                    "id, UserApplicationName, UserApplicationDescription, AppStreamApplicationId",
                    // attribute name substitution
                    null,
                    // attribute value substitution
                    null
            );
            List<String> allItems = new ArrayList<>();
            // Process each page of results
            for (Page<Item, ScanOutcome> page : items.pages()) {
                // Process each item on the current page
                Iterator<Item> item = page.iterator();
                while (item.hasNext()) {
                    allItems.add(item.next().toJSON());
                }
            }
            return "[" + StringUtils.join(allItems, ",") + "]";
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
     * Add application
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String addUserApplication(String application)
            throws ApplicationBadStateException {
        try {
            // TODO WebRTCEnabled=true
            String id = UUID.randomUUID().toString();
            Item userItem = Item.fromJSON(application)
                    .withPrimaryKey("id", id);
            entitlementServiceUserApplicationTable.putItem(userItem).getItem();
            return userItem.toJSONPretty();
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
     * Delete application
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     */

    public void deleteUserApplication(String id)
            throws ApplicationBadStateException {
        try {
            entitlementServiceUserApplicationTable.deleteItem("id", id);
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Get subscriptions
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String getUserSubscriptions()
            throws ApplicationBadStateException {
        try {
            ItemCollection<ScanOutcome> items = entitlementServiceUserSubscriptionTable.scan(
                    // filter expression
                    null,
                    // projection expression
                    "Email, UserId, CreationTimeMilli, UserApplicationId, AppStreamApplicationId, UserApplicationDescription, UserApplicationName, PerSessionTimeLimitMilli, TotalCombinedSessionTimeLimitMilli",
                    // attribute name substitution
                    null,
                    // attribute value substitution
                    null
            );
            List<String> allItems = new ArrayList<>();
            // Process each page of results
            for (Page<Item, ScanOutcome> page : items.pages()) {
                // Process each item on the current page
                Iterator<Item> item = page.iterator();
                while (item.hasNext()) {
                    allItems.add(item.next().toJSON());
                }
            }
            return "[" + StringUtils.join(allItems, ",") + "]";
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Add subscription
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String addUserSubscription(String subscription)
            throws ApplicationBadStateException {
        try {
            Item subscriptionItem = Item.fromJSON(subscription);
            // lookup user item by GSI query on user table (table must only contain one result)
            ItemCollection<QueryOutcome> responses = entitlementServiceUserTable.getIndex("emailGSI").query("email", subscriptionItem.getString("Email"));
            Iterator<Item> iter = responses.iterator();
            Item user = iter.next();
            // add and remove attributes to create valid subscription
            subscriptionItem
                    .withString("UserApplicationId", subscriptionItem.getString("id"))
                    .removeAttribute("id")
                    .withPrimaryKey("UserId", user.getString("id"), "CreationTimeMilli", Instant.now().toEpochMilli())
                    ;
            // save new subscription
            entitlementServiceUserSubscriptionTable.putItem(subscriptionItem).getItem();
            return subscriptionItem.toJSONPretty();
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Delete subscription
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     */

    public void deleteUserSubscription(String userId, Long creationTimeMilli)
            throws ApplicationBadStateException {
        try {

            entitlementServiceUserSubscriptionTable.deleteItem("UserId", userId, "CreationTimeMilli", creationTimeMilli);
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Get subscriptions
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String getUserSessions()
            throws ApplicationBadStateException {
        try {
            ItemCollection<ScanOutcome> items = entitlementServiceUserSessionTable.scan(
                    // filter expression
                    "attribute_not_exists(AppStreamEntitlementExpired)",
                    // projection expression
                    "Email, UserId, CreationTimeMilli, UserApplicationId, AppStreamApplicationId, UserApplicationDescription, UserApplicationName, PerSessionTimeLimitMilli, TotalCombinedSessionTimeLimitMilli",
                    // attribute name substitution
                    null,
                    // attribute value substitution
                    null
            );
            List<String> allItems = new ArrayList<>();
            // Process each page of results
            for (Page<Item, ScanOutcome> page : items.pages()) {
                // Process each item on the current page
                Iterator<Item> item = page.iterator();
                while (item.hasNext()) {
                    allItems.add(item.next().toJSON());
                }
            }
            return "[" + StringUtils.join(allItems, ",") + "]";
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Get configuration
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String getConfiguration()
            throws ApplicationBadStateException {
        try {
            JSONObject response = new JSONObject(cognitoProperties);
            return response.toString();
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }

    /**
     * Set configuration
     * @throws ApplicationBadStateException if the application the current request is for is in an error state
     * @return prettified string of JSON
     */

    public String setConfiguration(String config)
            throws ApplicationBadStateException {
        try {
            Item configItem = Item.fromJSON(config)
                .removeAttribute("id")
                .withPrimaryKey("StackId", cognitoProperties.getProperty("StackId"));
            entitlementServiceConfigurationTable.putItem(configItem);
            return configItem.toJSONPretty();
        } catch (AmazonServiceException e) {
            if (e.getErrorType().equals(AmazonServiceException.ErrorType.Service)) {
                log.error("An error occurred while getting data from DynamoDB: " + e);
            }
            throw e;
        }
    }
}