package com.amazonaws.sample.entitlement;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.sample.entitlement.authorization.AuthorizationHandler;
import com.amazonaws.sample.entitlement.authorization.CognitoIdentityAuthorizationHandler;
import com.amazonaws.sample.entitlement.authorization.LoginWithAmazonOAuth2AuthorizationHandler;
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

// local development profile
@Configuration
@Profile("local")
@EnableStackConfiguration(stackName = "entitlement-service-1428627185")
public class AWSConfigLocal {

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

    @Bean
    public AuthorizationHandler authorizationHandler() {
        return new CognitoIdentityAuthorizationHandler(cognitoProperties());
    }

    @Bean
    public AuthorizationHandler loginWithAmazonAuthorizationHandler() {
        return new LoginWithAmazonOAuth2AuthorizationHandler();
    }

}
