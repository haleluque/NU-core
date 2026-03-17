# --- Configuration ---
# Usage: .\seed-accounts.ps1
#        .\seed-accounts.ps1 -Endpoint "https://localhost:4566"
#        .\seed-accounts.ps1 -Endpoint $env:ENDPOINT
#
# From host with K8s: first run: kubectl port-forward -n nucore-lab svc/localstack 4566:4566
# Then: $env:ENDPOINT = "https://localhost:4566"; .\create-tables.ps1; .\seed-accounts.ps1
param(
    [string]$Endpoint = $env:ENDPOINT
)
if (-not $Endpoint) { $Endpoint = "https://localhost:4566" }
$ENDPOINT = $Endpoint
$REGION = "us-east-1"
$TABLE_NAME = "nu-core-payment-accounts"
$sslArgs = if ($Endpoint -match "^https://") { @("--no-verify-ssl") } else { @() }

# LocalStack: dummy credentials so AWS CLI does not ask for login
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = $REGION

Write-Host "--- Seeding nu-core-payment-accounts ---" -ForegroundColor Cyan

function Put-AccountItem($id, $customerName, $balance) {
    $itemJson = @{
        id           = @{ S = $id }
        customerName = @{ S = $customerName }
        balance      = @{ N = [string]$balance }
    } | ConvertTo-Json -Compress

    $escapedJson = $itemJson.Replace('"', '\"')
    & aws dynamodb put-item --table-name $TABLE_NAME --endpoint-url=$ENDPOINT --region=$REGION --item $escapedJson @sslArgs
    if ($LASTEXITCODE -ne 0) { Write-Warning "Put-item failed: id=$id" }
}

Put-AccountItem "11111111-1111-1111-1111-111111111111" "Alice" "5000.00"
Put-AccountItem "22222222-2222-2222-2222-222222222222" "Bob"   "3000.50"
Put-AccountItem "33333333-3333-3333-3333-333333333333" "Carol" "10000.00"

Write-Host "--- Seeding finished: Alice, Bob, Carol ---" -ForegroundColor Green
