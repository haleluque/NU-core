# Scripts (nu-core infra)

Scripts to create tables, verify LocalStack, and seed data. Run them from `nu.core` (for example: `.\infra\scripts\verify-localstack-service.ps1`).

| Script | Description |
|--------|-------------|
| `create-tables-via-portforward.ps1` | Creates DynamoDB tables in LocalStack via port-forward from the host (AWS CLI required). |
| `verify-localstack-service.ps1` | Checks the LocalStack Service, Endpoints, and ConfigMap in the cluster. |
| `verify-localstack-tables.ps1` | Runs a pod that performs `list-tables` against LocalStack to verify the tables exist. |
| `seed-accounts.curl` | Reference for calling the seed endpoint (Alice, Bob, Carol); requires port-forward to `nu-payment-service`. |

The Kubernetes manifests (deployments, services, ConfigMap) are in `nu.core/infra/k8s/`.
