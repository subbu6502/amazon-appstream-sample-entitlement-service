#!/usr/bin/env ruby
require 'aws-sdk'
require 'base64'

email                      = ARGV[0]
cognito_identity_pool_id 	 = ARGV[1]
developer_provider_name 	 = ARGV[2]
dynamodb_table_name 		   = ARGV[3]
dynamodb_table_name_config = ARGV[4]

puts "Creating Administrator user: " + email

cognitoidentity = Aws::CognitoIdentity::Client.new

developer_user_identifier = Base64.encode64(email).strip.sub(/=+$/,'')

resp = cognitoidentity.get_open_id_token_for_developer_identity({
  identity_pool_id: cognito_identity_pool_id,
  logins: {
    developer_provider_name => developer_user_identifier
  },
  token_duration: 1,
})

resp = cognitoidentity.lookup_developer_identity(
  identity_pool_id: cognito_identity_pool_id,
  developer_user_identifier: developer_user_identifier,
  max_results: 1,
  next_token: "PaginationKey",
)

cognito_identity_id = resp.data.identity_id

puts "Administrator User's Cognito Developer Identity Id: " + cognito_identity_id

dynamodb = Aws::DynamoDB::Client.new

resp = dynamodb.put_item(
  table_name: dynamodb_table_name,
  item: {
    "id" => cognito_identity_id,
    "email" => email,
    "role"  => 'Administrator'
  }
)

puts "Created Administrator User Item" + email

resp = dynamodb.put_item(
  table_name: dynamodb_table_name_config,
  item: {
    "StackId"                             => ARGV[5],
    "enableNoAuthV1EntitlementCondition"  => ARGV[6],
    "loginWithAmazonOAuthClientId"        => ARGV[7]
  }
)

puts "Created Configuration Item"
