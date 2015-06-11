# Amazon AppStream Sample Entitlement Service

The Java/Spring/Jersey API backend for the Amazon AppStream Sample (Developer) Entitlement Service (DES)

# Running Locally

## Packaging

`mvn package`

## Running

1) Edit

`application.properties`

2) Run

`mvn spring-boot:run`

# DynamoDB and IAM Roles Reference

## User Table

[See CloudFormation JSON](infra/appstreamEntitlementService.template#L146)

### Index

Hash Key      | Range Key     | Description
------------- | ------------- | -------------
id            | (none)        | Cognito ID

#### Global Secondary Index

Hash Key      | Range Key     | Description
------------- | ------------- | -------------
emailGSI            | (none) | Cognito ID Developer ID

### Attributes

Key      | Description
------------- | -------------
email         | Cognito ID Developer ID


## Application Table

[See CloudFormation JSON](infra/appstreamEntitlementService.template#L170)

### Index

Hash Key      | Range Key     | Description
------------- | ------------- | -------------
id            | (none)        | GUID

### Attributes

Key                         | Description
-------------               | -------------
AppStreamApplicationId      | AppStream's Application ID
UserApplicationName         | Administrator provided name for Application
UserApplicationDescription  | Administrator provided description for Application

## Session Table

[See CloudFormation JSON](infra/appstreamEntitlementService.template#L182)

### Index

Hash Key      | Range Key         | Description
------------- | -------------     | -------------
UserId        | CreationTimeMilli | Cognito ID + Create Time

### Attributes

Key                                 | Description
-------------                       | -------------
Email                               | User Email
UserApplicationId                   | Duped from Application Table
AppStreamApplicationId              | Duped
UserApplicationDescription          | Duped
UserApplicationName                 | Duped
PerSessionTimeLimitMilli            | Running time limit per streaming session
TotalCombinedSessionTimeLimitMilli  | Subscription lifetime time limit per streaming session

## Subscription Table

[See CloudFormation JSON](infra/appstreamEntitlementService.template#L196)

### Index

Hash Key      | Range Key         | Description
------------- | -------------     | -------------
UserId        | CreationTimeMilli | Cognito ID + Create Time

Important Note: The IAM policy for authenticated users allows the customer portal to use the Cognito role to directly access this table from the customer portal based on the customer's third-party credentials and their related Cognito ID.

```json
"Condition": {
  "ForAllValues:StringEquals": {
      "dynamodb:LeadingKeys": [
          "${cognito-identity.amazonaws.com:sub}"
      ],
      "dynamodb:Attributes": [
          "Email",
          "UserId",
          "CreationTimeMilli",
          "UserApplicationId",
          "AppStreamApplicationId",
          "UserApplicationDescription",
          "UserApplicationName",
          "PerSessionTimeLimitMilli",
          "TotalCombinedSessionTimeLimitMilli"
      ]
  },
```

[See CloudFormation JSON](infra/appstreamEntitlementService.template#L547)

### Attributes

(Identical to Session Table)

## Configuration Table

[See CloudFormation JSON](infra/appstreamEntitlementService.template#L210)

### Index

Hash Key       | Range Key         | Description
-------------  | -------------     | -------------
StackId        | (none)            | CloudFormation Stack Id

Important Note: The IAM policy for **un**authenticated users allows the customer portal to directly access this table from the customer portal without and before authenticating to a third-party.

```json
"Condition": {
  "ForAllValues:StringEquals": {
      "dynamodb:LeadingKeys": [
          { "Ref" : "AWS::StackName" }
      ],
      "dynamodb:Attributes": [
          "StackId",
          "loginWithAmazonOAuthClientId",
          "loginWithFacebookOAuthClientId",
          "loginWithGoogleClientId"
      ]
  },
```

[See CloudFormation JSON](infra/appstreamEntitlementService.template#L609)