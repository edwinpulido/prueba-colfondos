# Prueba tecnica Dev - Actualizacion de datos de contacto

Solucion de la historia de usuario **HU-001** del reto de Canales Digitales: permitir que un
afiliado actualice su correo electronico con validacion de identidad, confirmacion y trazabilidad.

## Contexto

El ejercicio parte de los 15 documentos del Mini-RAG entregados con la prueba. La solucion
distingue de forma explicita tres tipos de afirmacion:

- **Hecho**: lo que dicen los documentos (se cita el numero de documento).
- **Supuesto**: lo que se asume ante informacion faltante, con su justificacion.
- **Recomendacion**: la decision tecnica propuesta.

El detalle de esa clasificacion esta en `docs/01_Analisis_HU.md`.

## Contenido del repositorio

```
.
├── docs/                        Entregables E1 a E7
│   ├── 01_Analisis_HU.md
│   ├── 02_Diseno_Tecnico.md
│   ├── 03_Implementacion_PR.md
│   ├── 04_Pseudocodigo.md
│   ├── 05_Matriz_Pruebas.md
│   ├── 06_Debug_Safari.md
│   └── 07_Registro_IA.md
├── frontend-portalx74/          Componente de actualizacion (PortalX 7.4)
├── backend-orquestador/         Microservicio Java de orquestacion
├── mocks/                       Respuestas simuladas de los servicios externos
├── PULL_REQUEST.md              Descripcion del cambio
└── .env.example                 Variables de ambiente requeridas
```

## Como ejecutar

### 1. Backend

Requiere JDK 8 o superior y Maven 3.6+.

```bash
cd backend-orquestador
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

El API queda en `http://localhost:8081`. Los servicios externos se configuran con las
variables de `.env.example`; para una prueba sin dependencias reales se pueden usar los
mapeos de `mocks/` con WireMock:

```bash
docker run --rm -p 9091:8080 -v "%cd%/mocks/identity:/home/wiremock/mappings" wiremock/wiremock
docker run --rm -p 9092:8080 -v "%cd%/mocks/customer-data:/home/wiremock/mappings" wiremock/wiremock
docker run --rm -p 9093:8080 -v "%cd%/mocks/messaging:/home/wiremock/mappings" wiremock/wiremock
```

### 2. Frontend

Abrir `frontend-portalx74/index.html` en el navegador. Por defecto funciona en **modo
demostracion** con una API simulada en memoria, sin necesidad de backend.

Para probar contra el orquestador real: desmarcar "Usar API simulada" y confirmar la URL.
El backend de la prueba no incluye configuracion CORS porque en el ambiente corporativo el
portal y el API comparten el mismo dominio a traves del gateway.

### 3. Pruebas

```bash
cd backend-orquestador
mvn test

cd ../frontend-portalx74
node --test "tests/*.test.cjs"
```

## Datos para la demostracion local

| Escenario | Valor |
|---|---|
| Codigo valido | `123456` |
| Codigo expirado | `999999` |
| Correo duplicado | `duplicado@dominio.com` |
| Correo invalido | `ana@` |
| Falla de notificacion | Activar la casilla del panel de prueba |

## Resumen de la decision tecnica

| Tema | Decision |
|---|---|
| Frontend | Componente nuevo y desacoplado sobre PortalX 7.4 |
| Orquestacion | Microservicio Java que concentra la transaccion |
| Identidad | Identity Orchestration / Identity Capability (OTP) |
| Datos | Customer Data Service a traves de IntegrationHub OnPrem |
| Notificacion | Messaging Service, no bloqueante para la actualizacion |
| Errores | Catalogo unico `VAL/IDV/OTP/CD/AFI/CON/MSG/SYS` |
| Trazabilidad | `correlationId` en todas las capas + evento de auditoria |
| Datos sensibles | No se almacena biometria ni se registran datos completos en logs |

## Limitaciones conocidas

- El repositorio de transacciones es en memoria: en produccion se reemplaza por el
  repositorio transaccional del dominio.
- No se incluye el contrato de validacion facial porque el documento 06 no lo detalla.
- La validacion en Safari 17 requiere el laboratorio de QA; el procedimiento y el ajuste
  aplicado estan en `docs/06_Debug_Safari.md`.
- El escaneo de codigo seguro (3 dias habiles) y el comite de cambios condicionan la fecha
  de salida a produccion.
