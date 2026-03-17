# Verifies that the DynamoDB tables exist in LocalStack (pod in nucore-lab).
# Usage: .\verify-localstack-tables.ps1
$ErrorActionPreference = "Stop"
$podName = "verify-dynamodb-tables"
$ns = "nucore-lab"

kubectl delete pod $podName -n $ns --ignore-not-found 2>$null
Write-Host "Consultando LocalStack (list-tables)..." -ForegroundColor Cyan

kubectl run $podName -n $ns --restart=Never `
  --image=amazon/aws-cli:2.21.0 `
  --env="AWS_ACCESS_KEY_ID=test" `
  --env="AWS_SECRET_ACCESS_KEY=test" `
  --env="AWS_DEFAULT_REGION=us-east-1" `
  -- dynamodb list-tables --endpoint-url "http://localstack:4566" --region us-east-1

Start-Sleep -Seconds 2
$maxWait = 90
$elapsed = 0
$phase = ""
while ($elapsed -lt $maxWait) {
  $phase = (kubectl get pod $podName -n $ns -o jsonpath='{.status.phase}' 2>$null)
  if ($phase -eq "Succeeded" -or $phase -eq "Failed") { break }
  Start-Sleep -Seconds 3
  $elapsed += 3
}

Write-Host ""
Write-Host "--- Salida del pod (fase: $phase) ---" -ForegroundColor Cyan
$logs = kubectl logs $podName -n $ns 2>&1
if ($logs) { $logs } else { Write-Host "(sin salida)" }
Write-Host "---" -ForegroundColor Cyan
if ($phase -ne "Succeeded" -and $phase -ne "Failed") {
  Write-Host "Pod no terminó en tiempo. Eventos:" -ForegroundColor Yellow
  kubectl get events -n $ns --field-selector involvedObject.name=$podName --sort-by='.lastTimestamp' 2>&1
}
Write-Host ""
kubectl delete pod $podName -n $ns --ignore-not-found 2>$null
Write-Host "Listo." -ForegroundColor Green
