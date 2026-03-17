# Kubernetes Manifests (nu-core)

Deployment of **nu-payment-service** and **nu-risk-engine** on Kubernetes.

Path: `nu.core/infra/k8s/` - manifests only (Deployments, Services, ConfigMap, Jobs, and infrastructure). Insert, verification, and seed scripts are in **`nu.core/infra/scripts/`**.

## Files

| File | Description |
|------|-------------|
| `k8s-infrastructure.yaml` | **Apply first.** LocalStack (DynamoDB) and Kafka in the cluster (services `localstack:4566`, `kafka:9092`). Kafka runs in **KRaft mode** (no Zookeeper required). |
| `configmap.yaml` | ConfigMap with Kafka, LocalStack/DynamoDB, and health options (liveness/readiness). |
| `nu-payment-service-deployment.yaml` | Deployment of the payments API (2 replicas, HTTP probes, resource limits). |
| `nu-payment-service-service.yaml` | **NodePort** Service (port 30080) for external access (for example, Docker Desktop Kubernetes). |
| `nu-risk-engine-deployment.yaml` | Deployment of the risk engine (2 replicas, HTTP liveness/readiness probes, resource limits). |
| `nu-risk-engine-service.yaml` | **ClusterIP** Service (internal traffic only). |
| `dynamodb-create-tables-job.yaml` | Optional Job to create DynamoDB tables in LocalStack (tables only). Seed via the payment service endpoint. |

## Requirements

- All resources are deployed in the **nucore-lab** namespace. Create it if it does not exist: `kubectl create namespace nucore-lab`.
- If you use `k8s-infrastructure.yaml`, Kafka and LocalStack are deployed in the cluster; the ConfigMap already points to `kafka.nucore-lab.svc.cluster.local:9092` and `localstack.nucore-lab.svc.cluster.local:4566`.
- If you deploy Kafka/LocalStack by other means, adjust `configmap.yaml`:
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka broker address.
  - `AWS_DYNAMODB_ENDPOINT`: LocalStack URL or DynamoDB endpoint.

**Kafka KRaft:** The stack uses Apache Kafka in [KRaft mode](https://kafka.apache.org/documentation/#kraft) (no Zookeeper). The broker runs as a single node with combined broker and controller roles; applications connect only to the bootstrap server `kafka:9092`.

## Application order

**1. Infrastructure (Kafka in KRaft mode, LocalStack) and applications**

From the repo root or from `nu.core`:

```bash
# Infrastructure and ConfigMap
kubectl apply -f infra/k8s/k8s-infrastructure.yaml -n nucore-lab
kubectl apply -f infra/k8s/configmap.yaml -n nucore-lab
```

Wait for the infrastructure pods to be ready (`kubectl get pods -n nucore-lab`) before applying the services.

```bash
# Application services (payment API and risk engine)
kubectl apply -f infra/k8s/nu-payment-service-deployment.yaml -n nucore-lab
kubectl apply -f infra/k8s/nu-payment-service-service.yaml -n nucore-lab
kubectl apply -f infra/k8s/nu-risk-engine-deployment.yaml -n nucore-lab
kubectl apply -f infra/k8s/nu-risk-engine-service.yaml -n nucore-lab
```

Or apply the whole directory (ensure infrastructure is ready before application services start):

```bash
kubectl apply -f infra/k8s/ -n nucore-lab
```

**2. Verify that the pods are up**

```bash
kubectl get pods -n nucore-lab
```

**LocalStack pod IP** (to debug or connect by IP):

```bash
kubectl get pods -n nucore-lab -l app=localstack -o wide
```

IP only:

```bash
kubectl get pods -n nucore-lab -l app=localstack -o jsonpath='{.items[0].status.podIP}'
```

**3-step flow (tables → seed → list)**

1. **Check that LocalStack has Endpoints:**  
   From `nu.core`: `.\infra\scripts\verify-localstack-service.ps1`  
   If `ENDPOINTS` is empty, the LocalStack pod is not Ready; without a probe, this is fixed by restarting the pod:  
   `kubectl delete pod -n nucore-lab -l app=localstack`

2. **Create DynamoDB tables** (if they do not exist):  
   - From the cluster: `kubectl apply -f infra/k8s/dynamodb-create-tables-job.yaml -n nucore-lab` and check the job logs.  
   - Or from the host with port-forward: in one terminal `kubectl port-forward -n nucore-lab svc/localstack 4566:4566`, in another one (with AWS CLI, from `nu.core`):  
     `.\infra\scripts\create-tables-via-portforward.ps1`  
   Verify: `.\infra\scripts\verify-localstack-tables.ps1` should show both tables.

3. **Seed and list:**  
   - Seed: `Invoke-RestMethod -Uri "http://localhost:30080/api/v1/admin/seed-accounts" -Method Post`  
   - List accounts: `Invoke-RestMethod -Uri "http://localhost:30080/api/v1/admin/accounts" -Method Get`  
   (If you use port-forward to the service: `kubectl port-forward -n nucore-lab svc/nu-payment-service 8080:8080` and change 30080 to 8080.)

**3. LocalStack DynamoDB: tables and seed**

The payment service **creates the DynamoDB tables on startup** when `AWS_DYNAMODB_ENDPOINT` is set (the ConfigMap does this in-cluster). It retries for up to about 1 minute so LocalStack can become ready. No Job or port-forward is required for tables.

**Seed data** (Alice, Bob, Carol): call the admin endpoint once the payment service is up:

```bash
# With NodePort 30080 (replace with your node IP or use port-forward)
curl -X POST http://localhost:30080/api/v1/admin/seed-accounts
```

Or with port-forward:

```bash
kubectl port-forward -n nucore-lab svc/nu-payment-service 8080:8080
curl -X POST http://localhost:8080/api/v1/admin/seed-accounts
```

**Optional - Job (tables only):** If the payment service cannot reach LocalStack at startup, you can create tables manually with the Job (seed is not included; use the endpoint above):

```bash
kubectl apply -f infra/k8s/dynamodb-create-tables-job.yaml -n nucore-lab
# Then seed: curl -X POST http://<payment-svc>/api/v1/admin/seed-accounts
```

**Optional - Scripts from your machine (port-forward to LocalStack):** See below ("Connect to LocalStack for DynamoDB scripts").

### Connect to LocalStack for DynamoDB scripts (recommended if pod-to-pod communication fails)

The most reliable way to create tables and load seed data from your machine is **port-forward** + PowerShell scripts. It does not depend on other pods reaching LocalStack.

**1. In one terminal, keep the port-forward to the LocalStack service running:**

```bash
kubectl port-forward -n nucore-lab svc/localstack 4566:4566
```

(If port 4566 is already in use on the host, use another one: `14566:4566` and then `https://localhost:14566` in the scripts.)

**2. In another terminal, from `nu.core` (with [AWS CLI](https://aws.amazon.com/cli/) installed):**

```powershell
# Create DynamoDB tables (port-forward must be active)
.\infra\scripts\create-tables-via-portforward.ps1
# Seed: call the payment service endpoint (port-forward to nu-payment-service 8080:8080)
# See infra/scripts/seed-accounts.curl or: curl -X POST http://localhost:8080/api/v1/admin/seed-accounts
```

Done: tables created and Alice, Bob, and Carol accounts inserted. The payment service in the cluster will still need to be able to reach LocalStack for transfers; if it cannot, use the payment service seed endpoint via port-forward to that service and call `POST /api/v1/admin/seed-accounts` after creating the tables through port-forward to LocalStack.

## Docker Desktop Kubernetes: using local images

Docker Desktop uses the same Docker daemon for both local builds and the Kubernetes cluster, so images built on your machine are available to the cluster without extra steps.

If the images `nu-payment-service:latest` and `nu-risk-engine:latest` are built on your machine:

```bash
# From the monorepo root (Nu Clon):
docker build -t nu-payment-service:latest -f nu.core/Dockerfile nu.core
docker build -t nu-risk-engine:latest -f engine/Dockerfile engine
```

Then apply the manifests. The Deployments use `imagePullPolicy: IfNotPresent`.

## Accessing the payments API (Docker Desktop Kubernetes)

With the fixed NodePort (30080):

```bash
kubectl get nodes -o wide
# Then: http://<NODE_IP>:30080
```

Or use port-forward for local access:

```bash
kubectl port-forward -n nucore-lab svc/nu-payment-service 8080:8080
# Then: http://localhost:8080
```

## Probes

- **nu-payment-service**: liveness and readiness via HTTP at `/actuator/health/liveness` and `/actuator/health/readiness` (port 8080).
- **nu-risk-engine**: liveness and readiness via HTTP at `/actuator/health/liveness` and `/actuator/health/readiness` (port 8081). Both services use Spring Boot Actuator and the ConfigMap enables health status endpoints.

## Security

- Pods with `seccompProfile: RuntimeDefault`; containers with `allowPrivilegeEscalation: false` and `capabilities.drop: [ALL]`.
- For production: build images with a non-root user and set `runAsNonRoot` / `runAsUser` in the Deployment.
- LocalStack credentials in the ConfigMap (development only). In production use **Secrets** and, if applicable, IAM/IRSA.

## Resources

- **Requests**: 256Mi memory, 100m CPU.
- **Limits**: 512Mi memory, 500m CPU.

Adjust according to the expected cluster load.

## Cleanup

Use these commands when you want to remove the Kubernetes resources and clean up local Docker artifacts.

### Kubernetes environment

Remove the application resources first:

```bash
kubectl delete -f infra/k8s/nu-payment-service-service.yaml -n nucore-lab
kubectl delete -f infra/k8s/nu-payment-service-deployment.yaml -n nucore-lab
kubectl delete -f infra/k8s/nu-risk-engine-service.yaml -n nucore-lab
kubectl delete -f infra/k8s/nu-risk-engine-deployment.yaml -n nucore-lab
kubectl delete -f infra/k8s/dynamodb-create-tables-job.yaml -n nucore-lab
kubectl delete -f infra/k8s/configmap.yaml -n nucore-lab
kubectl delete -f infra/k8s/k8s-infrastructure.yaml -n nucore-lab
```

If you want a full reset, delete the namespace instead:

```bash
kubectl delete namespace nucore-lab
```

### Local Docker environment

Stop any local containers you started for testing, for example Docker Compose services:

```bash
docker compose -f infra/docker-compose.yml down
```

If you built local images and want to remove them:

```bash
docker rmi nu-payment-service:latest nu-risk-engine:latest
```

If you also want to remove unused Docker resources:

```bash
docker system prune -f
```
