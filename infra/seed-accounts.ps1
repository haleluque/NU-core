# --- Configuration ---
$ENDPOINT = "http://localhost:4566"
$REGION = "us-east-1"
$TABLE_NAME = "nu-core-payment-accounts"

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
    aws dynamodb put-item --table-name $TABLE_NAME --endpoint-url=$ENDPOINT --region=$REGION --item $escapedJson
    if ($LASTEXITCODE -ne 0) { Write-Warning "Put-item failed: id=$id" }
}

Put-AccountItem "11111111-1111-1111-1111-111111111111" "Alice" "5000.00"
Put-AccountItem "22222222-2222-2222-2222-222222222222" "Bob"   "3000.50"
Put-AccountItem "33333333-3333-3333-3333-333333333333" "Carol" "10000.00"

Write-Host "--- Seeding finished: Alice, Bob, Carol ---" -ForegroundColor Green
