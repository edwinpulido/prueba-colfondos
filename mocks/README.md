# Simuladores de servicios externos (WireMock)

Respuestas estaticas para ejecutar el flujo sin depender de los servicios corporativos.

## Levantar los simuladores

Cada servicio debe correr en el puerto que espera el orquestador.

```bash
# Identity -> 9091
docker run --rm -p 9091:8080 -v "%cd%/mocks/identity:/home/wiremock/mappings" wiremock/wiremock

# Customer Data -> 9092
docker run --rm -p 9092:8080 -v "%cd%/mocks/customer-data:/home/wiremock/mappings" wiremock/wiremock

# Messaging -> 9093
docker run --rm -p 9093:8080 -v "%cd%/mocks/messaging:/home/wiremock/mappings" wiremock/wiremock
```

## Escenarios disponibles

| Escenario | Como se activa |
|---|---|
| Flujo exitoso | Afiliado `AFI-000123`, codigo `123456` |
| Codigo expirado | Enviar `999999` en la validacion |
| Correo duplicado | Afiliado `AFI-000999` al confirmar |
| Messaging caido | Detener el contenedor de Messaging antes de confirmar |
| Identity detenido | El orquestador responde `504 IDV-408` (timeout de conexion) |

En la prueba local tambien se puede usar la API simulada del frontend, que no requiere
Docker.
