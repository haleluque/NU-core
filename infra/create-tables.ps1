# Creates DynamoDB tables for nu.core (accounts and transactions).
# Defaults to LocalStack (http://localhost:4566). Set $env:ENDPOINT = "" to use real AWS.

$ErrorActionPreference = "Stop"
if (-not $env:AWS_ACCESS_KEY_ID) { $env:AWS_ACCESS_KEY_ID = "test" }
if (-not $env:AWS_SECRET_ACCESS_KEY) { $env:AWS_SECRET_ACCESS_KEY = "test" }
if (-not $env:AWS_DEFAULT_REGION) { $env:AWS_DEFAULT_REGION = "us-east-1" }

$endpoint = $env:ENDPOINT
if ($endpoint -eq $null) { $endpoint = "http://localhost:4566" }
$endpointArg = if ($endpoint -eq "") { @() } else { @("--endpoint-url", $endpoint) }
Write-Host "Using endpoint: $(if ($endpoint) { $endpoint } else { 'default (real AWS)' })"

Write-Host "Creating table nu-core-payment-accounts..."
aws dynamodb create-table @endpointArg `
  --table-name nu-core-payment-accounts `
  --attribute-definitions AttributeName=id,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST

Write-Host "Creating table nu-core-payment-transactions..."
aws dynamodb create-table @endpointArg `
  --table-name nu-core-payment-transactions `
  --attribute-definitions AttributeName=id,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST

Write-Host "Waiting for tables to become active..."
aws dynamodb wait table-exists @endpointArg --table-name nu-core-payment-accounts
aws dynamodb wait table-exists @endpointArg --table-name nu-core-payment-transactions

Write-Host "Done. Tables: nu-core-payment-accounts, nu-core-payment-transactions"
