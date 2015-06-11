package com.amazonaws.sample.entitlement.tasks;

import com.amazonaws.sample.entitlement.exceptions.ApplicationNotFoundException;
import com.amazonaws.services.appstream.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import org.apache.log4j.Logger;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;

@Component
public class AppStreamSessionsTask {

    @Inject private AppStream appstream;

    private Table entitlementServiceUserSessionTable;
    private Table entitlementServiceUserSubscriptionTable;

    private Logger log = Logger.getLogger(AppStreamSessionsTask.class.getName());

    @Inject
    public AppStreamSessionsTask(ResourceIdResolver resourceIdResolver, Properties cognitoProperties, DynamoDB dynamoDBDocument) {
        this.entitlementServiceUserSessionTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceUserSession"));
        this.entitlementServiceUserSubscriptionTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceUserSubscription"));
        log.info("Running Worker");
    }

    @Scheduled(fixedRate = 20000)
    public void getApplicationStatus() {
        log.info("Running DES Session Management Task");
        // filter out ended and expired sessions
        ScanSpec scanSpec = new ScanSpec()
                .withFilterExpression("attribute_not_exists(EndDateEpochMilli) AND attribute_not_exists(AppStreamEntitlementExpired)");
        ItemCollection<ScanOutcome> items = entitlementServiceUserSessionTable.scan(scanSpec);
        for (Page<Item, ScanOutcome> page : items.pages()) {
            Iterator<Item> item = page.iterator();
            while (item.hasNext()) {
                Item session = item.next();
                try {
                    // get info from AppStream
                    Application appstreamApplication = getApplication(session.getString("AppStreamApplicationId"));
                    // get session
                    Session appstreamSession = appstreamApplication.getSessionById(session.getString("AppStreamSessionId"));
                    // get session state
                    String state = appstreamSession.getStatus().getState().toString();
                    log.info(appstreamSession.getId() + " [" + state + "]");
                    // get subscription
                    Item subscription = entitlementServiceUserSubscriptionTable.getItem("UserId", session.getString("UserId"), "CreationTimeMilli", session.getLong("UserSubscriptionCreationTimeMilli"));
                    if (subscription == null) {
                      log.info("Missing subscription: " +  session.getString("UserId") + session.getLong("UserSubscriptionCreationTimeMilli"));
                    }
                    switch (state) {
                        // TODO don't process already ended sessions
                        case "Entitled":
                            // terminate if past valid and not started
                            Long entitleTimeMilli = session.getLong("CreationTimeMilli");
                            // AppStream default entitlement URL validity
                            Long validEntitledTimeMilli = new Long(60000);
                            if (session.isPresent("AppStreamEntitlementUrlValidTimeMilli")) {
                                validEntitledTimeMilli = session.getLong("AppStreamEntitlementUrlValidTimeMilli");
                            }
                            // calculate elapsed running time
                            Long elapsedEntitledMilli = (Instant.now().toEpochMilli() - entitleTimeMilli);
                            if (elapsedEntitledMilli > validEntitledTimeMilli) {
                                log.info("Terminating for past entitled state session time limit: " + validEntitledTimeMilli);
                                // terminate
                                SessionStatus newStatus = appstreamSession.terminate(new TerminateSessionInput());
                                // update session state
                                session.withString("AppStreamSessionState", newStatus.getState().toString());
                                // set endDate
                                session.with("AppStreamEntitlementExpired", true);
                            }
                            break;
                        case "Terminated":
                        case "Completed":
                            if (session.getString("EndDateEpochMilli") == null) {
                                // get and save startDate
                                Long startDateEpochMilli = appstreamSession.getStartDate().toInstant().toEpochMilli();
                                session.with("StartDateEpochMilli", startDateEpochMilli);
                                log.info("StartDateEpochMilli: " + startDateEpochMilli);
                                // get and save endDate
                                Long endDateEpochMilli = appstreamSession.getEndDate().toInstant().toEpochMilli();
                                session.with("EndDateEpochMilli", endDateEpochMilli);
                                log.info("EndDateEpochMilli: " + endDateEpochMilli);
                                if (subscription != null) {
                                    // calculate remaining total subscription running time
                                    Long totalCombinedSessionTimeLimitMilli = subscription.getLong("TotalCombinedSessionTimeLimitMilli");
                                    log.info("Total Combined Session Limit Milliseconds: " + totalCombinedSessionTimeLimitMilli);
                                    // calculate remaining total subscription running time
                                    Long remainingTotalCombinedSessionTimeLimitMilli = totalCombinedSessionTimeLimitMilli - (endDateEpochMilli - startDateEpochMilli);
                                    log.info("Remaining Combined Session Limit Milliseconds: " + remainingTotalCombinedSessionTimeLimitMilli);
                                    // update subscription with remaining subscription time
                                    subscription.withLong("TotalCombinedSessionTimeLimitMilli", remainingTotalCombinedSessionTimeLimitMilli);
                                }
                                // update session in DynamoDB with status
                                session.with("AppStreamSessionState", state);
                            }
                            break;
                        case "Active":
                            // get session start date
                            Long startDateEpochMilli = appstreamSession.getStartDate().toInstant().toEpochMilli();
                            session.with("StartDateEpochMilli", startDateEpochMilli);
                            log.info("StartDateEpochMilli: " + startDateEpochMilli);
                            Long perSessionTimeLimitMilli = Long.valueOf(session.getString("PerSessionTimeLimitMilli"));
                            log.info("Per Session Time Limit Milliseconds: " + perSessionTimeLimitMilli);
                            // calculate elapsed running time
                            Long elapsedMilli = (Instant.now().toEpochMilli() - startDateEpochMilli);
                            log.info("Elapsed Time Milliseconds: " + elapsedMilli);
                            // terminate if past session time
                            if (elapsedMilli > perSessionTimeLimitMilli) {
                                SessionStatus newStatus = appstreamSession.terminate(new TerminateSessionInput());
                                log.info("Terminating for past session time limit: " + perSessionTimeLimitMilli);
                                session.withString("AppStreamSessionState", newStatus.getState().toString());
                            }
                            // if no subscription then just work with session end time
                            if (subscription != null) {
                                // get session time limit and remaining total subscription running time limit
                                Long totalCombinedSessionTimeLimitMilli = subscription.getLong("TotalCombinedSessionTimeLimitMilli");
                                log.info("Total Combined Session Time Limit Milliseconds: " + startDateEpochMilli);
                                // terminate if past total combined session time
                                if (elapsedMilli > totalCombinedSessionTimeLimitMilli) {
                                    log.info("Terminating for past total combined time limit: " + totalCombinedSessionTimeLimitMilli);
                                    SessionStatus newStatus = appstreamSession.terminate(new TerminateSessionInput());
                                    session.withString("AppStreamSessionState", newStatus.getState().toString());
                                }
                            }
                            break;
                    }
                    // save
                    entitlementServiceUserSessionTable.putItem(session);
                    if (subscription != null) {
                        entitlementServiceUserSubscriptionTable.putItem(subscription);
                    }
                } catch (ApplicationNotFoundException e) {
                    log.error(e);
                }
            }
        }
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
}
