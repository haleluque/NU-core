# Tests de integración (Testcontainers)

Los tests que usan **Testcontainers** (p. ej. `PaymentSagaTest`) necesitan que el cliente Java pueda hablar con el daemon de Docker.

## Windows + Docker Desktop (WSL2)

En este entorno el daemon suele estar en WSL2 y la conexión por **npipe** devuelve HTTP 400 al cliente Java. La opción que funciona es exponer el daemon por **TCP**:

1. Abre **Docker Desktop**.
2. Ve a **Settings** (engranaje) → **General**.
3. Activa la opción **"Expose daemon on tcp://localhost:2375 without TLS"**.
4. Pulsa **Apply & Restart** si es necesario.

Después de eso, desde la raíz del proyecto:

```bash
mvn clean test -Dtest=PaymentSagaTest
```

El `pom.xml` ya configura Surefire para usar `DOCKER_HOST=tcp://localhost:2375` en los tests, así que no hace falta exportar la variable a mano.

## Comprobar que Docker responde

```powershell
docker info
```

Si usas TCP 2375:

```powershell
$env:DOCKER_HOST="tcp://localhost:2375"; docker info
```

Debe mostrar datos del servidor (contenedores, imágenes, etc.). Si ves "connection refused", el daemon no está expuesto en 2375.
