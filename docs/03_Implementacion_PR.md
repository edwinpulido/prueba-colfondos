# E3 - Implementacion, ramas, configuracion y PR

## 1. Estructura entregada

```
frontend-portalx74/
├── index.html                 Pantalla del componente
├── styles.css                 Estilos aislados del portal
├── src/compat.js              Utilidades compatibles con Safari
├── src/validaciones.js        Validacion de correo y codigo
├── src/api.js                 Cliente HTTP con timeout e idempotencia
├── src/mock-api.js            API simulada para demostracion
├── src/componente.js          Controlador de la pantalla
└── tests/*.test.cjs           Pruebas con el runner de Node

backend-orquestador/
├── pom.xml
└── src/
    ├── main/java/com/canalesdigitales/contacto/
    │   ├── api/               Controlador, DTO y manejador de errores
    │   ├── client/            Adaptadores Identity, Customer Data, Messaging
    │   ├── config/            RestTemplate, correlationId
    │   ├── domain/            Transaccion, estados, repositorio
    │   ├── exception/         Catalogo de errores
    │   └── service/           Reglas del flujo
    ├── main/resources/        application.yml, logback
    └── test/java/             Pruebas unitarias, web y de adaptadores
```

## 2. Tareas tecnicas

| # | Tarea | Estimacion | Dependencia |
|---|---|---|---|
| T-01 | Refinamiento de HU y catalogo de errores con LT | 2 h | E1 |
| T-02 | Contratos de Identity, Customer Data y Messaging | 4 h | Integracion |
| T-03 | Orquestador: estados, validaciones, idempotencia | 12 h | T-02 |
| T-04 | Componente de UI en PortalX 7.4 | 10 h | T-01 |
| T-05 | Pruebas unitarias y de integracion | 8 h | T-03, T-04 |
| T-06 | Pruebas de regresion y compatibilidad | 6 h | QA |
| T-07 | Revision de seguridad y escaneo | 3 dias habiles | T-03 |
| T-08 | Despliegue y cierre operativo | 2 h | T-07 |

## 3. Ramas y commits

| Rama | Proposito |
|---|---|
| `feature/HU-001-actualizacion-correo` | Desarrollo principal |
| `feature/HU-001-fix-safari` | Ajuste de compatibilidad y manejo de promesas |
| `hotfix/HU-001-timeout-identidad` | Ajuste de timeouts si se requiere en produccion |

Convencion de commits:

```
feat(api): expone inicio de actualizacion de contacto
feat(api): agrega validacion de codigo y bloqueo por intentos
feat(api): aplica actualizacion con idempotencia
feat(ui): construye componente de actualizacion en PortalX 7.4
fix(ui): manejo explicito de promesas para Safari 17
test(api): cubre timeout, duplicado y notificacion pendiente
docs: agrega analisis, diseno, pruebas y registro
```

## 4. Configuracion local

1. Copiar `.env.example` a `.env` y ajustar las URLs.
2. Levantar los simuladores de `mocks/` o los servicios reales en los puertos 9091 a 9093.
3. Ejecutar `mvn spring-boot:run -Dspring-boot.run.profiles=local` en `backend-orquestador`.
4. Abrir `frontend-portalx74/index.html` en modo demostracion o apuntar al backend.

Checklist previo al primer commit local:

- [x] `.gitignore` cubre `target/`, `*.log`, `.env`.
- [x] `.env.example` documenta todas las variables.
- [x] README con pasos reproducibles.
- [x] Sin credenciales ni datos reales en el repositorio.

## 5. Analisis estatico y calidad

- Reglas de codigo: sin `System.out`, sin `printStackTrace`, sin `catch` vacio.
- Complejidad controlada: el servicio concentra la orquestacion, los adaptadores solo
  traducen protocolo y errores.
- Dependencias: solo Spring Boot Web, Validation y Actuator.
- Cobertura propuesta minima: 80 % en servicio y validadores.

## 6. Estrategia de pruebas en el pipeline

```
build -> pruebas unitarias -> pruebas de integracion con mocks -> analisis estatico
      -> escaneo de seguridad -> despliegue a QA -> regresion -> comite -> produccion
```

## 7. Contenido del Pull Request

El PR incluye:

- Descripcion funcional y enlace a la HU.
- Lista de cambios por capa.
- Decisiones y supuestos relevantes.
- Evidencia de pruebas y de la revision de seguridad.
- Variables de ambiente nuevas.
- Riesgo y plan de rollback.
- Checklist de autorrevision.

El texto completo esta en `PULL_REQUEST.md`.

## 8. Plan de despliegue

| Paso | Responsable | Evidencia |
|---|---|---|
| Radicar en comite de cambios | Dev | Ticket aprobado |
| Escaneo de seguridad | Ciberseguridad | Informe sin hallazgos criticos |
| Despliegue en ventana posterior a 9:00 p.m. | Operacion TI | Log de despliegue |
| Prueba de humo en produccion | Dev + QA | Captura y `transactionId` de prueba |
| Aviso en canales oficiales | Lider tecnico | Mensaje publicado |
| Cierre en Mesa de Servicio | Dev | Caso cerrado con evidencias |

## 9. Riesgos de implementacion

| Riesgo | Mitigacion |
|---|---|
| Contrato real distinto al supuesto | Adaptadores aislados y contrato versionado |
| Diferencias entre 7.0 y 7.4 | Pruebas de regresion y bandera de activacion |
| Escaneo de seguridad retrasa la salida | Radicar con 3 dias habiles de anticipacion |
| Doble envio del usuario | Boton bloqueado, idempotencia y proteccion contra doble clic |
| Datos sensibles en logs | Enmascaramiento y revision de logs en el PR |
