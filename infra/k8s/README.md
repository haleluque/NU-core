# Manifiestos Kubernetes (nu-core)

Despliegue de **nu-payment-service** y **nu-risk-engine** en Kubernetes.

Ruta: `nu.core/infra/k8s/`

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `configmap.yaml` | ConfigMap con Kafka, LocalStack/DynamoDB y opciones de health (liveness/readiness). |
| `nu-payment-service-deployment.yaml` | Deployment del API de pagos (2 réplicas, probes HTTP, límites de recursos). |
| `nu-payment-service-service.yaml` | Service **NodePort** (puerto 30080) para acceso externo (p. ej. Minikube). |
| `nu-risk-engine-deployment.yaml` | Deployment del motor de riesgo (2 réplicas, probes HTTP liveness/readiness, límites de recursos). |
| `nu-risk-engine-service.yaml` | Service **ClusterIP** (solo tráfico interno). |

## Requisitos

- **Kafka** y **LocalStack** (o DynamoDB) desplegados en el clúster y accesibles desde los pods.
- Ajustar en `configmap.yaml`:
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS`: dirección del broker Kafka en el clúster.
  - `AWS_DYNAMODB_ENDPOINT`: URL de LocalStack o endpoint de DynamoDB.

## Orden de aplicación

Desde la raíz del repo (o desde `nu.core`):

```bash
kubectl apply -f infra/k8s/configmap.yaml
kubectl apply -f infra/k8s/nu-payment-service-deployment.yaml
kubectl apply -f infra/k8s/nu-payment-service-service.yaml
kubectl apply -f infra/k8s/nu-risk-engine-deployment.yaml
kubectl apply -f infra/k8s/nu-risk-engine-service.yaml
```

O aplicar todo el directorio:

```bash
kubectl apply -f infra/k8s/
```

## Minikube: uso de imágenes locales

Si las imágenes `nu-payment-service:latest` y `nu-risk-engine:latest` se construyeron en tu máquina:

```bash
eval $(minikube docker-env)
# Desde la raíz del monorepo (Nu Clon):
docker build -t nu-payment-service:latest -f nu.core/Dockerfile nu.core
docker build -t nu-risk-engine:latest -f engine/Dockerfile engine
```

Luego aplica los manifiestos. Los Deployments usan `imagePullPolicy: IfNotPresent`.

## Acceso al API de pagos (Minikube)

```bash
minikube service nu-payment-service --url
```

O con NodePort fijo (30080):

```bash
kubectl get nodes -o wide
# Luego: http://<NODE_IP>:30080
```

## Probes

- **nu-payment-service**: liveness y readiness vía HTTP en `/actuator/health/liveness` y `/actuator/health/readiness` (puerto 8080).
- **nu-risk-engine**: liveness y readiness vía HTTP en `/actuator/health/liveness` y `/actuator/health/readiness` (puerto 8081). Ambos servicios usan Spring Boot Actuator y el ConfigMap habilita los estados de health.

## Seguridad

- Pods con `seccompProfile: RuntimeDefault`; contenedores con `allowPrivilegeEscalation: false` y `capabilities.drop: [ALL]`.
- Para producción: construir imágenes con usuario no root y configurar `runAsNonRoot` / `runAsUser` en el Deployment.
- Credenciales de LocalStack en ConfigMap (solo para desarrollo). En producción usar **Secrets** y, si aplica, IAM/IRSA.

## Recursos

- **Requests**: 256Mi memoria, 100m CPU.
- **Limits**: 512Mi memoria, 500m CPU.

Ajusta según la carga esperada del clúster.
