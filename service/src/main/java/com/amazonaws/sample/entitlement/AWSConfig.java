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

package com.amazonaws.sample.entitlement;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.appstream.AmazonAppStream;
import com.amazonaws.services.appstream.AppStream;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.aws.context.config.annotation.EnableStackConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Properties;

// running in an EC2 instance in a CloudFormation stack
@Configuration
@Profile("default")
@EnableStackConfiguration
public class AWSConfig {

    @Bean
    @Qualifier("configuration")
    public Properties cognitoProperties() {
        Properties properties = new Properties();
        properties.setProperty("awsCognitoDeveloperProviderName", env.getRequiredProperty("aws.cognito.developer.provider.name"));
        properties.setProperty("awsCognitoIdentityPool", env.getRequiredProperty("aws.cognito.identity.pool"));
        return properties;
     }

    @Autowired
    Environment env;

    @Bean
    public AWSCredentialsProvider awsCredentialsProvider() {
        return new DefaultAWSCredentialsProviderChain();
    }

    @Bean
    public Region region() {
        return Region.getRegion(Regions.fromName(env.getRequiredProperty("cloud.aws.region.static")));
    }

    @Bean
    public AppStream appStream() {
        return new AmazonAppStream().with(awsCredentialsProvider()).getAppStream();
    }

    @Bean
    public AmazonDynamoDBClient dynamoDBClient() {
       return region().createClient(AmazonDynamoDBClient.class, awsCredentialsProvider(), null);
    }

    @Bean
    public DynamoDB dynamoDBDocument() {
        return new DynamoDB(dynamoDBClient());
    }

    @Bean
    public AmazonCognitoIdentityClient cognitoIdentity() {
        return region().createClient(AmazonCognitoIdentityClient.class, awsCredentialsProvider(), null);
    }

}


