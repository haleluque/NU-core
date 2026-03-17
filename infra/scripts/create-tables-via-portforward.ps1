# Creates DynamoDB tables in LocalStack using port-forward from the host.
# Requirements: AWS CLI installed locally, kubectl, and an existing LocalStack pod.
# Usage: .\create-tables-via-portforward.ps1
$ErrorActionPreference = "Stop"
$ns = "nucore-lab"

$podName = kubectl get pods -n $ns -l app=localstack -o jsonpath='{.items[0].metadata.name}' 2>$null
if (-not $podName) { Write-Host "No LocalStack pod found in $ns"; exit 1 }

Write-Host "LocalStack pod: $podName" -ForegroundColor Cyan
Write-Host "Starting port-forward in background (localhost:4566 -> pod:4566)..." -ForegroundColor Cyan
$pf = Start-Process -FilePath "kubectl" -ArgumentList "port-forward","-n",$ns,"pod/$podName","4566:4566" -PassThru -WindowStyle Hidden
Start-Sleep -Seconds 4

try {
    $env:AWS_ACCESS_KEY_ID = "test"
    $env:AWS_SECRET_ACCESS_KEY = "test"
    $env:AWS_DEFAULT_REGION = "us-east-1"
    Write-Host "Listing tables..." -ForegroundColor Cyan
    aws dynamodb list-tables --endpoint-url http://localhost:4566 --region us-east-1 2>&1
    Write-Host "Creating nu-core-payment-accounts..." -ForegroundColor Cyan
    aws dynamodb create-table --endpoint-url http://localhost:4566 --region us-east-1 `
        --table-name nu-core-payment-accounts `
        --attribute-definitions AttributeName=id,AttributeType=S `
        --key-schema AttributeName=id,KeyType=HASH --billing-mode PAY_PER_REQUEST 2>&1
    Write-Host "Creating nu-core-payment-transactions..." -ForegroundColor Cyan
    aws dynamodb create-table --endpoint-url http://localhost:4566 --region us-east-1 `
        --table-name nu-core-payment-transactions `
        --attribute-definitions AttributeName=id,AttributeType=S `
        --key-schema AttributeName=id,KeyType=HASH --billing-mode PAY_PER_REQUEST 2>&1
    Write-Host "Done. Verifying..." -ForegroundColor Green
    aws dynamodb list-tables --endpoint-url http://localhost:4566 --region us-east-1 2>&1
} finally {
    Stop-Process -Id $pf.Id -Force -ErrorAction SilentlyContinue
}
Write-Host "Listo." -ForegroundColor Green
