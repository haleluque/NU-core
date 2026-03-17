# SRE: Verifies that the LocalStack Service is properly configured so that the pods can connect.
# Usage: .\verify-localstack-service.ps1
$ErrorActionPreference = "Stop"
$ns = "nucore-lab"

Write-Host "=== 1. Service localstack (puerto 4566 -> targetPort del contenedor) ===" -ForegroundColor Cyan
kubectl describe service localstack -n $ns

Write-Host "`n=== 2. Endpoints (pods que reciben tráfico del Service; vacío = ningún pod Ready) ===" -ForegroundColor Cyan
kubectl get endpoints localstack -n $ns

Write-Host "`n=== 3. Pods del Deployment localstack (selector del Service: app=localstack) ===" -ForegroundColor Cyan
kubectl get pods -n $ns -l app=localstack -o wide

Write-Host "`n=== 4. ConfigMap: AWS_DYNAMODB_ENDPOINT ===" -ForegroundColor Cyan
kubectl get configmap nu-app-config -n $ns -o jsonpath='{.data.AWS_DYNAMODB_ENDPOINT}'
Write-Host ""
