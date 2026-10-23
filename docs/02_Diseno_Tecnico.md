# E2 - Diseno tecnico

HU-001: actualizacion de correo electronico con validacion de identidad.

## 1. Principios de la solucion

1. **El cambio de contacto es una transaccion**, no una suma de llamadas: tiene estado,
   dueño y trazabilidad.
2. **La identidad se valida antes de tocar el dato.** Lo exige el documento 06 y es el
   control que da sentido al flujo.
3. **La notificacion no bloquea el resultado.** Si Messaging falla, el correo igual queda
   actualizado y la confirmacion se reintenta (aprendizaje de INC-002).
4. **La UI no conoce detalles de red ni de proveedores.** Todo pasa por un contrato propio.
5. **Compatibilidad primero.** Si una API de JavaScript no esta en los tres navegadores
   soportados, no se usa sin respaldo (INC-001).

## 2. Arquitectura propuesta

```
┌──────────────────────────────────────────────────────────────┐
│ PortalX 7.4 - componente "Actualizacion de contacto"          │
│ HTML + JS encapsulado, sin dependencia de librerias del       │
│ portal legado. Manejo explicito de errores y promesas.        │
└───────────────────────────────┬──────────────────────────────┘
                                │ HTTPS / TLS 1.2+
                                │ X-Correlation-ID, Idempotency-Key
                                ▼
┌──────────────────────────────────────────────────────────────┐
│ Orquestador de actualizacion de contacto (Spring Boot)        │
│ - validacion de entrada y sesion                              │
│ - maquina de estados de la transaccion                        │
│ - catalogo de errores                                         │
│ - auditoria y logs estructurados                              │
└───────┬─────────────────────┬──────────────────────┬─────────┘
        │                     │                      │
        ▼                     ▼                      ▼
┌───────────────┐   ┌────────────────────┐   ┌────────────────┐
│ Identity      │   │ Customer Data      │   │ Messaging      │
│ Orchestration │   │ Service            │   │ Service        │
│ / Capability  │   │ (IntegrationHub    │   │ (email/SMS)    │
│ (OTP)         │   │  OnPrem)           │   │                │
└───────────────┘   └────────────────────┘   └────────────────┘
```

### Por que un orquestador y no llamadas directas desde el portal

| Alternativa | Descripcion | Decision |
|---|---|---|
| A. El portal llama a cada servicio | Menos piezas, pero expone contratos, mezcla seguridad y no hay estado | Descartada |
| B. Orquestacion en IntegrationHub | Reutiliza la plataforma, pero el flujo tiene reglas de canal y UI | Descartada para esta HU |
| C. Microservicio de orquestacion en canales | Concentra reglas, estado y errores; aislado del portal legado | **Elegida** |

El orquestador se despliega como capacidad de canales y reutiliza IntegrationHub como
transporte hacia los sistemas internos. No reemplaza la plataforma de integracion.

### Ubicacion de la capacidad

- **Nuevo:** componente de UI en PortalX 7.4 y microservicio `contacto-orquestador`.
- **Reutilizado:** Identity Orchestration, Customer Data, Messaging, IntegrationHub OnPrem.
- **No se modifica:** modulo legado "Actualizacion de Datos" de PortalX 7.0. Convive
  durante la transicion; se documenta la dependencia, como pide el documento 03.
- **Justificacion de OnPrem:** el documento 05 indica que las capacidades que dependen de
  sistemas internos pueden permanecer en OnPrem. Este flujo depende de Customer Data y de
  reglas internas, por lo que la migracion a Cloud se evalua despues, con metricas de
  latencia de esta HU.

## 3. Contratos de API

Base: `/v1/contacto/actualizacion`
Cabeceras comunes: `X-Correlation-ID` (opcional, se genera si no llega),
`Content-Type: application/json`.

### 3.1 Iniciar el flujo

`POST /v1/contacto/actualizacion`

```json
{
  "afiliadoId": "AFI-000123",
  "nuevoCorreo": "ana.lopez@dominio.com",
  "canalPreferido": "SMS"
}
```

Respuesta `202 Accepted`:

```json
{
  "transactionId": "6f1c2a9e-0d4b-4a1e-9b6f-6a1f0f6d9c11",
  "estado": "PENDIENTE_VERIFICACION",
  "codigo": "OTP-202",
  "mensaje": "Enviamos un codigo de verificacion para confirmar su identidad.",
  "canal": "SMS",
  "expiraEnSegundos": 300,
  "correlationId": "d1f4c0f5-2a44-4c7c-9d5e-9f1a7b3c2e10"
}
```

Errores: `VAL-001` (400), `IDV-401` (403), `IDV-408` (504), `SYS-503` (503).

### 3.2 Validar el codigo

`POST /v1/contacto/actualizacion/{transactionId}/verificacion`

```json
{ "codigo": "123456" }
```

Respuesta `200 OK`:

```json
{
  "transactionId": "6f1c2a9e-...",
  "estado": "IDENTIDAD_VALIDADA",
  "codigo": "IDV-200",
  "mensaje": "Identidad validada. Confirme la actualizacion de su correo.",
  "correlationId": "..."
}
```

Errores: `OTP-401` (401), `OTP-410` (410), `IDV-429` (429), `CON-409` (409).

### 3.3 Confirmar la actualizacion

`PUT /v1/contacto/actualizacion/{transactionId}`
Cabecera obligatoria en produccion: `Idempotency-Key: <uuid>`.

```json
{ "nuevoCorreo": "ana.lopez@dominio.com" }
```

Respuesta `200 OK`:

```json
{
  "transactionId": "6f1c2a9e-...",
  "estado": "APLICADA",
  "codigo": "OK-200",
  "mensaje": "Su correo electronico fue actualizado correctamente. Enviamos la confirmacion.",
  "correlationId": "..."
}
```

Si Messaging falla, responde `202 Accepted` con estado
`APLICADA_NOTIFICACION_PENDIENTE` y codigo `MSG-503`. El cambio ya quedo aplicado.

Errores: `CD-409` (409), `CON-409` (409), `SYS-408` (504), `SYS-503` (503),
`AFI-404` (404).

### 3.4 Consultar estado

`GET /v1/contacto/actualizacion/{transactionId}` devuelve el estado actual. No expone
correo completo ni identificadores internos del proveedor. Se usa para soporte y para que
el portal retome un flujo interrumpido.

### 3.5 Contratos externos consumidos

| Servicio | Operacion | Uso |
|---|---|---|
| Identity | `POST /v1/identity/verificacion` | Generar y enviar OTP. Devuelve referencia, canal y vigencia |
| Identity | `POST /v1/identity/verificacion/validacion` | Validar OTP. Devuelve aprobado / rechazado / expirado |
| Customer Data | `PUT /v1/afiliados/{id}/contacto` | Actualizar correo. 409 si el dato ya existe |
| Messaging | `POST /v1/notificaciones` | Enviar confirmacion transaccional |

Estos contratos son **supuestos** hasta que Integracion publique los definitivos (D-06,
D-07). Por eso estan aislados en adaptadores: si cambian, solo se ajusta el cliente.

## 4. Maquina de estados

```
INICIADA
   │ OTP enviado
   ▼
PENDIENTE_VERIFICACION ──(expirado)──► EXPIRADA
   │ OTP correcto                     │
   ▼                                  │
IDENTIDAD_VALIDADA                    │
   │ confirmacion                     │
   ▼                                  │
APLICADA ◄── notificacion ok          │
   │                                  │
   └── notificacion falla ──► APLICADA_NOTIFICACION_PENDIENTE

Cualquier fallo tecnico previo a la actualizacion ──► FALLIDA
3 intentos fallidos de OTP ──► BLOQUEADA
Identidad rechazada ──► RECHAZADA
```

Reglas:

- `APLICADA` y `APLICADA_NOTIFICACION_PENDIENTE` son estados finales exitosos.
- Desde `EXPIRADA` o `BLOQUEADA` se inicia una transaccion nueva; no se reutiliza.
- Un fallo de Customer Data no cambia el estado: permite reintentar la confirmacion.
- El estado se persiste antes y despues de cada llamada externa relevante.

## 5. Catalogo de errores

| Codigo | HTTP | Situacion | Mensaje al afiliado | Accion del sistema |
|---|---|---|---|---|
| `VAL-001` | 400 | Correo o codigo con formato invalido | Mensaje de campo | No invoca servicios |
| `IDV-401` | 403 | Identidad rechazada | "No pudimos validar su identidad..." | Ofrece canal presencial |
| `IDV-408` | 504 | Timeout del proveedor de identidad | "La validacion esta tardando..." | Permite reintentar |
| `IDV-429` | 429 | Intentos de OTP agotados | "Solicite un nuevo codigo" | Bloquea transaccion |
| `OTP-401` | 401 | Codigo incorrecto | "El codigo no es correcto" | Incrementa contador |
| `OTP-410` | 410 | Codigo expirado | "El codigo expiro" | Habilita reenvio |
| `CD-409` | 409 | Correo ya registrado | Mensaje generico | Audita, no actualiza |
| `AFI-404` | 404 | Afiliado no encontrado | "No pudimos identificar el afiliado" | Audita |
| `CON-409` | 409 | Estado invalido para la operacion | "Debe completar la validacion" | No actualiza |
| `MSG-503` | 202 | Notificacion no disponible | "La confirmacion se enviara pronto" | Encola reintento |
| `SYS-404` | 404 | Transaccion inexistente | "La transaccion no existe o expiro" | Ninguna |
| `SYS-408` | 504 | Timeout de datos | "Intente nuevamente" | Permite reintento |
| `SYS-503` | 503 | Servicio no disponible | "Intente en unos minutos" | Monitorea y alerta |
| `SYS-500` | 500 | Error no controlado | Mensaje generico | Log con `correlationId` |
| `GEN-408` | - | Timeout en el navegador | "La operacion tardo..." | No reintenta solo |
| `GEN-503` | - | Fallo de red en el navegador | "Verifique su conexion" | No reintenta solo |

Los mensajes no revelan si un correo existe en otro afiliado ni detalles del proveedor.

## 6. Seguridad

| Control | Implementacion |
|---|---|
| Transporte | TLS 1.2 o superior entre portal, orquestador e IntegrationHub |
| Validacion de entradas | Bean Validation + `CorreoValidator` + validacion en frontend |
| OWASP Top 10 | Parametros tipados, sin concatenacion de SQL, salida escapada en la UI, sin `eval` |
| Secretos | Variables de ambiente / vault; no se versionan credenciales |
| Datos biometricos | No se reciben ni se almacenan; solo referencia, resultado, proveedor, fecha y hora (doc 08) |
| Logs | Correo enmascarado (`a***@dominio.com`); nunca OTP ni cuerpo completo |
| Rate limiting | Pendiente de gateway: maximo de intentos por afiliado e IP |
| Escaneo | Analisis estatico y escaneo de codigo seguro antes de produccion (3 dias habiles) |
| Formato de seguridad | Se diligencia la evaluacion de ciberseguridad con flujos, datos y responsables |

Riesgo residual: el `afiliadoId` llega en el cuerpo en esta prueba. En produccion debe
tomarse de la sesion autenticada para impedir que un usuario opere sobre otro afiliado.

## 7. Trazabilidad y observabilidad

- `correlationId` generado o recibido en el filtro y propagado a todos los servicios.
- Logs estructurados con evento, `transactionId`, estado y correo enmascarado.
- Eventos de auditoria: `ACTUALIZACION_INICIADA`, `IDENTIDAD_VALIDADA`,
  `ACTUALIZACION_APLICADA`, `ACTUALIZACION_FALLIDA`, `NOTIFICACION_PENDIENTE`.
- Metricas propuestas: tasa de exito, abandono por paso, latencia de API, errores por
  navegador, disponibilidad de Identity y de Messaging.
- Alertas: error 5xx sostenido, timeout del proveedor, caida de la tasa de exito.

## 8. Datos y persistencia

| Dato | Donde vive | Observacion |
|---|---|---|
| `transactionId` | Orquestador | UUID, no predecible |
| Estado e intentos | Repositorio transaccional | En la prueba, memoria; en produccion, base gobernada |
| Correo nuevo | Customer Data | Fuente autorizada |
| Referencia de identidad | Orquestador | Identificador del proveedor, sin biometria |
| Idempotency-Key | Repositorio | Indice unico por llave |

Datos sinteticos usados en pruebas: afiliados `AFI-000123` a `AFI-000125`, correos
`@dominio.com`, codigos `123456` / `000000` / `999999`.

## 9. Diseno de la interfaz

Tres pasos visibles y un paso de resultado:

1. Correo nuevo y numero de afiliado.
2. Codigo OTP con contador y reenvio.
3. Confirmacion explicita con el correo a actualizar.
4. Resumen con estado, fecha y `correlationId`.

Detalles de implementacion:

- Componente encapsulado en un IIFE, sin variables globales de negocio.
- Estados de carga en los botones para evitar doble envio.
- Mensajes con `role="alert"` y `aria-live` para lectores de pantalla.
- El paso de resultado distingue exito completo de notificacion pendiente.
- Panel de prueba local separado del componente, no se despliega a produccion.

## 10. Configuracion y despliegue

Variables en `.env.example`:

| Variable | Proposito |
|---|---|
| `SERVER_PORT` | Puerto del orquestador |
| `IDENTITY_BASE_URL` | URL de Identity |
| `CUSTOMER_DATA_BASE_URL` | URL de Customer Data |
| `MESSAGING_BASE_URL` | URL de Messaging |

Timeouts: conexion 2 s; lectura 60 s para identidad, 8 s para datos y 5 s para notificacion.

Despliegue:

1. Analisis estatico y escaneo de seguridad.
2. Radicacion en el comite de cambios (martes 11:00 a.m.).
3. Paso a produccion despues de las 9:00 p.m.
4. Aviso en los canales oficiales y validacion con referente funcional.
5. Cierre en Mesa de Servicio con evidencias.

Rollback: el componente de UI se puede desactivar con la bandera del portal; el servicio no
modifica datos por si solo, por lo que revertir la version no requiere compensacion. Las
transacciones en curso quedan consultables por `transactionId`.

## 11. Plantilla de diseno tecnico (seccion 8 del documento de la prueba)

| Campo | Respuesta |
|---|---|
| Componente frontend | Componente "Actualizacion de contacto" en PortalX 7.4: formulario de correo, OTP, confirmacion y resumen; JS encapsulado con utilidades de compatibilidad |
| Servicio / API | `POST /v1/contacto/actualizacion`, `POST /{id}/verificacion`, `PUT /{id}`, `GET /{id}`. Consume Identity, Customer Data y Messaging |
| Validaciones | Correo y codigo en frontend y backend; identidad antes de actualizar; idempotencia; limite de intentos |
| Manejo de errores | Catalogo unico de codigos; traduccion de fallas externas; timeout y reintento; notificacion no bloqueante |
| Datos sinteticos | `AFI-000123`, correos `@dominio.com`, OTP `123456`; sin datos reales |
| Logs / correlationId | Filtro que genera o propaga `X-Correlation-ID`; logs estructurados; auditoria por evento |
| Pruebas | Unitarias, integracion con mocks, negativas, regresion, Safari y caida de proveedor |
| README / PR | README con ejecucion, `.env.example`, `PULL_REQUEST.md` y checklist de despliegue |

## 12. Limitaciones y siguientes pasos

1. Reemplazar el repositorio en memoria por la base transaccional del dominio.
2. Confirmar contratos reales de Identity y Customer Data con Integracion.
3. Definir cola de reintento de notificaciones con su SLA.
4. Implementar rate limiting en el gateway.
5. Evaluar la migracion a IntegrationHub Cloud con metricas de esta HU.
6. Incorporar actualizacion de celular en una HU posterior, reutilizando el flujo.
