package com.amazonaws.sample.entitlement.tasks;

import com.amazonaws.services.dynamodbv2.document.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

@Component
public class ConfigurationRefreshTask {

    @Autowired
    @Qualifier("configuration")
    private Properties cognitoProperties;

    private Table entitlementServiceConfigurationTable;

    private String stackName;

    private Logger log = Logger.getLogger(ConfigurationRefreshTask.class.getName());

    @Autowired
    public ConfigurationRefreshTask(ResourceIdResolver resourceIdResolver, DynamoDB dynamoDBDocument, StackResourceRegistry stackResourceRegistry) {
        this.entitlementServiceConfigurationTable = dynamoDBDocument.getTable(resourceIdResolver.resolveToPhysicalResourceId("EntitlementServiceConfiguration"));
        this.stackName = stackResourceRegistry.getStackName();
        log.info("Running Configuration Refresh Worker");
    }

    @Scheduled(fixedRate = 20000)
    public void getConfiguration() {
        log.info("Running Configuration Refresh Task");
        Item item = entitlementServiceConfigurationTable.getItem("StackId", stackName);
        Iterator<Map.Entry<String, Object>> iterator = item.attributes().iterator();
        while (iterator.hasNext()) {
            Map.Entry attr = iterator.next();
            this.cognitoProperties.setProperty(attr.getKey().toString(), attr.getValue().toString());
        }
    }

}
