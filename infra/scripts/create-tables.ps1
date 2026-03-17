# Creates DynamoDB tables for nu.core (accounts and transactions).
# Defaults to LocalStack (https://localhost:4566). Set $env:ENDPOINT = "" to use real AWS.
#
# From host with K8s: first run: kubectl port-forward -n nucore-lab svc/localstack 4566:4566
# Then: $env:ENDPOINT = "https://localhost:4566"; .\create-tables.ps1

$ErrorActionPreference = "Stop"
if (-not $env:AWS_ACCESS_KEY_ID) { $env:AWS_ACCESS_KEY_ID = "test" }
if (-not $env:AWS_SECRET_ACCESS_KEY) { $env:AWS_SECRET_ACCESS_KEY = "test" }
if (-not $env:AWS_DEFAULT_REGION) { $env:AWS_DEFAULT_REGION = "us-east-1" }

$endpoint = $env:ENDPOINT
if ($endpoint -eq $null) { $endpoint = "https://localhost:4566" }
# Build common args: endpoint + optional --no-verify-ssl for LocalStack 3.x HTTPS
$commonArgs = @()
if ($endpoint -ne "") { $commonArgs += "--endpoint-url", $endpoint }
if ($endpoint -match "^https://") { $commonArgs += "--no-verify-ssl" }
Write-Host "Using endpoint: $(if ($endpoint) { $endpoint } else { 'default (real AWS)' })"

Write-Host "Creating table nu-core-payment-accounts..."
& aws dynamodb create-table @commonArgs --table-name nu-core-payment-accounts --attribute-definitions AttributeName=id,AttributeType=S --key-schema AttributeName=id,KeyType=HASH --billing-mode PAY_PER_REQUEST

Write-Host "Creating table nu-core-payment-transactions..."
& aws dynamodb create-table @commonArgs --table-name nu-core-payment-transactions --attribute-definitions AttributeName=id,AttributeType=S --key-schema AttributeName=id,KeyType=HASH --billing-mode PAY_PER_REQUEST

Write-Host "Waiting for tables to become active..."
& aws dynamodb wait table-exists @commonArgs --table-name nu-core-payment-accounts
& aws dynamodb wait table-exists @commonArgs --table-name nu-core-payment-transactions

Write-Host "Done. Tables: nu-core-payment-accounts, nu-core-payment-transactions"
