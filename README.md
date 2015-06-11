# Amazon AppStream Sample Entitlement Service

Reference implementation of an Amazon AppStream entitlement service. 

## About

The Amazon AppStream Sample (Developer) Entitlement Service, a.k.a 'DES', has the following features:

1) applications and subscriptions management

2) streaming sessions management

3) third-party authentication integration

The stack is deployed with CloudFormation and deploys:

1) the DES service API and task runners on EC2

2) a web application portal for access in the browser

3) a CloudFront distribution to front both (1) and (2)

Amazon DynamoDB is used for data storage. Amazon Cognito is used for authentication.

A CloudFormation template configures IAM, EC2, DynamoDB, and CloudFront.

See the README.md files in subdirectories of this repository for more documentation.

# Deploying DES

The following are instructions for running and deploying the Amazon AppStreawm Sample Entitlement Service (DES) in your account using packages built from this repository.

## Prerequisites

There are two prerequisite steps:

1) obtain a Login With Amazon Client ID, and

2) create an Amazon Cognito Developer Identity Pool connected to (1)

The identifiers and tokens obtained by doing the above will be used when deploying DES with CloudFormation. 

These authentication setup is necessary so that an Administrator can login to DES using Login with Amazon as third-party authentication.

## Creating a Login with Amazon application and a Amazon Cognito Identity Pool

1) Follow the instructions [here](https://login.amazon.com/website) to register a Login with Amazon Application

(Note: you may only need to follow Step 1 to register an application and obtain a Client ID)

2) Make sure you obtain a Amazon Client ID [here](https://login.amazon.com/manageApps)

	* Note the Login with Amazon Client ID (1)
 
## Amazon Cognito Developer Identity Pool

You will need to create a Amazon Cognito Identity Pool and associate your Login with Amazon application Client ID with it.

(Note: See [here](https://docs.aws.amazon.com/cognito/devguide/identity/developer-authenticated-identities/) for more on Developer Authenticated Identities.)

1) Go the [Amazon Cognito AWS Management Console](https://console.aws.amazon.com/cognito/)

2) Click on "Create new Identity Pool"

3) Input an "Identity Pool Name" like "DESUsers"

4) Under "Unauthenticated identities" select "Enable access to unauthenticated identities"

5) Under the "Amazon" tab of "Public identity providers" input your:

	1) Amazon App ID (1)
	
6) Under "Custom" tab of "Public identity providers" input a name like 'login.mycompany.myapp'

	(Note this name for later input for the parameter CognitoDeveloperProviderName (2))

7) Click 'Create Pool'

8) Choose 'Don't Allow' when prompted to choose Roles.

	(Note: the roles will be created by CloudFormation and later associated with the Cognito Identity Pool)

9) Click on 'Edit identity pool' at the top right.
	
	(Note the 'Identity pool ID' for input later for the parameter CognitoIdentityPoolId (3))
	
# CloudFormation Stack Launch

## Launching DES via AWS CloudFormation

You can launch the DES application(s) and infrastructure with AWS CloudFormation.

1) Use this link to open the CloudFormation template directly in the AWS Managment Console: 

[Open the DES CloudFormation template in the AWS Management Console](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=sample-appstream-entitlement-service&templateURL=https://netisense-des-staging-00.s3.amazonaws.com/sample-appstream-developer-entitlement-infrastructure/appstreamEntitlementService.template)

2) Click Next

3) Input the Login with Amazon Client ID in the Parameters for the stack:

	LoginWithAmazonOAuthClientId (1) (e.g.: amzn1.application.xxxxxxxxxxxxxxxxxxxxxxxx)
	
4) Input the following two identifers in the Parameters for the stack:

	CognitoDeveloperProviderName (2) (e.g.: 'login.mycompany.myapp')

	CognitoIdentityPoolId (3) (e.g.: 'us-east-1:d10f9dbd-b4c5-466c-8a5b-e70ebbb846b9')

5) Input the EC2 Key Pair name and an administrator user's email:

	KeyName (Note: the keypair must already exist in the same region: 'us-east-1')

	AdministratorUserEmail (Note: The email you willl use to login to the application with Login with Amazon)

6) Click Next

7) Click Next on the 'Options' step

8) In the 'Review' step select "I acknowledge that this template might cause AWS CloudFormation to create IAM resources."

9) Click Create

10) When the Stack reaches the CREATE_COMPLETE state ...

11) Navigate to the Outputs for the Stack

# Update the Cognito Developer Identity Pool

1) Navigate to the Amazon Cognito service in the AWS Management Portal.

2) Navigate to the details of the identity pool.

3) Click on 'Edit Identity Pool'.

4) Click on 'Select a role ...' for 'Anauthenticated role'.

5) Select the ID that is shown for the 'CognitoUnauthenticatedRoleId' output of your stack.

	e.g.: 'sample-appstream-entitlem-CognitoUnauthenticatedRo-1CD6XQATND48I'

6) Click on 'Select a role ...' for 'Authenticated role'.

7) Select the ID that is shown for the 'CognitoAuthenticatedRoleId' output of your stack.

	e.g.: 'sample-appstream-entitlement-service-CognitoRole-HSY4CW8R8C1L'

8) Click on 'Save Changes'

# Update the Referrer URI with Login with Amazon

You must register your endpoint URI with your third-party authentication provider(s) to allow authentications to succeed.

1) Navigate to [Login with Amazon](https://login.amazon.com/manageApps)

2) Under 'Web Settings' for your application click 'Edit'

3) Input the URL value of the WebPortalEndpointURL output of your CloudFormation stack under 'Allowed JavaScript Origins'

4) Click 'Save'

You can now go to the application's endpoint URL and login with Login with Amazon!