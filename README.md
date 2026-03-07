# NU CORE

Servicio de pagos que orquesta transferencias entre cuentas, persiste en DynamoDB y publica eventos en Kafka para validación de riesgo (saga). Incluye compensación automática cuando el motor de riesgo rechaza la operación.

---

## Stack y versiones

| Componente | Versión |
|------------|---------|
| **Java** | 21 |
| **Spring Boot** | 3.4.5 |
| **Spring Kafka** | (managed by Boot) |
| **AWS SDK DynamoDB** | 2.28.0 |
| **Micrometer Prometheus** | (managed by Boot) |
| **Testcontainers** | 1.21.4 |
| **Awaitility** | (managed by Boot) |

Imágenes Testcontainers:

- **Kafka:** `confluentinc/cp-kafka:7.6.0`
- **LocalStack (DynamoDB):** `localstack/localstack:3.0`

---

## Requisitos

- **JDK 21**
- **Maven 3.9+**
- **Docker** (para tests de integración con Testcontainers)
- **Docker Desktop (Windows):** activar *"Expose daemon on tcp://localhost:2375 without TLS"* (Settings → General) para que los tests de integración conecten correctamente.

Para desarrollo local sin Testcontainers:

- **Kafka** en `localhost:29092` (o configurar `spring.kafka.bootstrap-servers`)
- **LocalStack** en `http://localhost:4566` para DynamoDB (o endpoint vacío para AWS real)

---

## Cómo arrancar la aplicación

Este proyecto está compuesto por 2 microservicios:

- **core** (puerto **8080**): orquesta transferencias y gestiona compensaciones.
- **engine** (puerto **8081**): valida límites y reglas de riesgo.

Para iniciar cada uno localmente:

### Arrancar `core` (8080)

```bash
cd nu.core
mvn spring-boot:run
```

La API de core queda en **http://localhost:8080**.

### Arrancar `engine` (8081)

_Asumiendo que el módulo/codebase de engine también está disponible localmente:_

```bash
cd nu.engine
mvn spring-boot:run
```

La API del engine queda en **http://localhost:8081**.

---

## Cómo funcionan los tests

### Tipos de test

1. **Tests unitarios / contexto**  
   - **`ApplicationTests`**: carga el contexto de Spring. No usa Docker ni Kafka/DynamoDB reales (puede fallar si Kafka no está disponible en `localhost:29092` según `application.yml`).

2. **Tests de integración (Testcontainers)**  
   - **`PaymentSagaTest`**: extiende `AbstractIntegrationTest`.  
   - Levanta **Kafka** y **LocalStack (DynamoDB)** en contenedores (una vez por suite).  
   - Usa **WebTestClient** para llamar al API de pagos y **Kafka** para simular el rechazo del motor de riesgo; con **Awaitility** comprueba que el saldo en DynamoDB vuelve al valor original (compensación).

### Flujo de `PaymentSagaTest`

1. **Preparación:** `AbstractIntegrationTest` inicia Kafka y LocalStack, crea las tablas DynamoDB y hace seed de cuentas (Alice, Bob).
2. **Test:**  
   - Se obtiene el saldo inicial de la cuenta origen.  
   - Se envía un **POST** a `/api/v1/payments/transfer`.  
   - Se simula un mensaje de **rechazo** en el topic `risk-events` (como si lo enviara el microservicio de riesgo).  
   - Con **Awaitility** se espera (hasta 15 s) a que el saldo de la cuenta origen en DynamoDB coincida de nuevo con el inicial (compensación aplicada).

### Qué revisar para verificar que todo funciona

- **Docker:**  
  - `docker info` debe responder bien.  
  - Si usas TCP 2375: `$env:DOCKER_HOST="tcp://localhost:2375"; docker info` (PowerShell).
- **Tests de integración:**  
  - `mvn clean test -Dtest=PaymentSagaTest` debe terminar en **BUILD SUCCESS** y *Tests run: 1, Failures: 0, Errors: 0*.
- **Aplicación local:**  
  - Health: `GET http://localhost:8080/actuator/health`  
  - Métricas: `GET http://localhost:8080/actuator/prometheus` (contadores `payments.processed.total`, `payments.compensated.total`).
- **LocalStack/DynamoDB:**  
  - Tablas creadas (ver sección de comandos).  
  - Cuentas con saldo esperado tras seed (Alice, Bob, Carol si usaste el script de seed).

Documentación adicional de tests de integración: [docs/INTEGRATION_TESTS.md](docs/INTEGRATION_TESTS.md).

---

## Configuración principal

En `src/main/resources/application.yml`:

- **Server:** puerto `8080`.
- **Actuator:** endpoints `health` y `prometheus` expuestos.
- **Kafka:** `bootstrap-servers`, consumer group, producer (por defecto `localhost:29092`).
- **Tópicos (app.payment.kafka):** `payment-events`, `risk-events`, `payment.completed`.
- **Métricas (app.payment.metrics):** nombres de los contadores de pagos procesados y compensados.
- **AWS/DynamoDB:** región, endpoint (p. ej. `http://localhost:4566` para LocalStack), credenciales de prueba.

---

## API de ejemplo

- **POST** `/api/v1/payments/transfer`  
  - Body: `{ "originAccountId": "uuid", "destinationAccountId": "uuid", "amount": 100.00 }`  
  - Respuesta: `transferId`, `status` (p. ej. `PENDING_RISK`).

- **GET** `/api/v1/payments/transfers/{transferId}`  
  - Detalle del transfer (cuentas, monto, estado, fecha).

---

## Comandos útiles

### Docker

```powershell
# Comprobar que Docker está activo
docker info

# Usar daemon expuesto por TCP (Windows)
$env:DOCKER_HOST="tcp://localhost:2375"; docker info

# Listar contenedores en ejecución
docker ps

# Ver contextos (p. ej. desktop-linux)
docker context ls
```

### Maven

```powershell
# Compilar
mvn clean compile

# Ejecutar todos los tests
mvn test

# Solo test de integración PaymentSagaTest
mvn clean test -Dtest=PaymentSagaTest

# Solo test de contexto
mvn test -Dtest=ApplicationTests

# Arrancar la aplicación
mvn spring-boot:run

# Empaquetar (sin tests)
mvn clean package -DskipTests
```

### DynamoDB (LocalStack / AWS CLI)

Variables para LocalStack (PowerShell):

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
$env:ENDPOINT = "http://localhost:4566"
```

Crear tablas (LocalStack por defecto):

```powershell
cd nu.core/infra
.\create-tables.ps1
```

Seed de cuentas (Alice, Bob, Carol):

```powershell
.\seed-accounts.ps1
```

Comandos útiles de DynamoDB (LocalStack):

```powershell
# Listar tablas
aws dynamodb list-tables --endpoint-url $env:ENDPOINT --region us-east-1

# Describir tabla de cuentas
aws dynamodb describe-table --endpoint-url $env:ENDPOINT --table-name nu-core-payment-accounts

# Describir tabla de transacciones
aws dynamodb describe-table --endpoint-url $env:ENDPOINT --table-name nu-core-payment-transactions

# Escanear items de cuentas (ver datos)
aws dynamodb scan --endpoint-url $env:ENDPOINT --table-name nu-core-payment-accounts

# Obtener una cuenta por id
aws dynamodb get-item --endpoint-url $env:ENDPOINT --table-name nu-core-payment-accounts `
  --key '{"id":{"S":"11111111-1111-1111-1111-111111111111"}}'

# Escanear transacciones
aws dynamodb scan --endpoint-url $env:ENDPOINT --table-name nu-core-payment-transactions
```

Para **AWS real**, quitar `--endpoint-url` (o usar `$env:ENDPOINT = ""` en los scripts).

### Kafka (si tienes Kafka local o en Docker)

```powershell
# Listar tópicos (ejemplo con herramienta en container)
docker run --rm -it --network host confluentinc/cp-kafka:7.6.0 kafka-topics --bootstrap-server localhost:29092 --list

# Consumir mensajes de risk-events (ejemplo)
# (depende de tu instalación; aquí se asume kafka-console-consumer disponible)
kafka-console-consumer --bootstrap-server localhost:29092 --topic risk-events --from-beginning
```

### API y salud (curl / PowerShell)

```powershell
# Health
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" | ConvertTo-Json

# Prometheus metrics (fragmento)
Invoke-WebRequest -Uri "http://localhost:8080/actuator/prometheus" -UseBasicParsing | Select-Object -ExpandProperty Content

# POST transfer (ejemplo con cuentas del seed)
$body = @{
  originAccountId      = "11111111-1111-1111-1111-111111111111"
  destinationAccountId = "22222222-2222-2222-2222-222222222222"
  amount               = 50.00
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payments/transfer" -Method Post -Body $body -ContentType "application/json"
```

### Testcontainers (configuración local)

Archivo **`~/.testcontainers.properties`** (para que los tests usen Docker por TCP en Windows):

```properties
docker.host=tcp://localhost:2375
docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy
```

---

## Estructura del proyecto

- **`src/main/java`**: aplicación (hexagonal: application, domain, infrastructure).
- **`src/main/resources`**: `application.yml`, metadata de configuración.
- **`src/test/java`**: tests; `com.haleluque.nu.core.payment.AbstractIntegrationTest` y `PaymentSagaTest`.
- **`infra/`**: scripts PowerShell para crear tablas DynamoDB y seed de cuentas (LocalStack).
- **`docs/`**: documentación adicional (p. ej. tests de integración).
