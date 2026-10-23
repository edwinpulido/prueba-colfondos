# E1 - Analisis de la historia de usuario

Iniciativa: INI-2026-001 - Actualizacion digital de datos de contacto
Historia: HU-001 - Actualizacion de correo electronico
Rol: Desarrollador
Ultima actualizacion: octubre de 2026

## 1. Entendimiento de la necesidad

Los documentos 01 y 14 del Mini-RAG describen que el afiliado no logra terminar procesos
digitales porque el correo o el celular registrados no sirven para recibir el codigo de
verificacion. Hoy la salida es el Contact Center o la oficina fisica.

La HU base es:

> Como afiliado quiero actualizar mi correo electronico para recibir comunicaciones en mi
> nueva direccion.

Para que la HU sea implementable se amplia con tres condiciones que ya estan en los
criterios iniciales del reto y en el alcance del documento 01:

1. La actualizacion solo procede con identidad validada (documento 06, observacion de
   Customer Data API).
2. El afiliado debe recibir confirmacion del resultado (documento 14).
3. La operacion debe quedar trazada (documento 11).

### Alcance

- Identificacion del afiliado dentro de una sesion autenticada del portal.
- Validacion de identidad con codigo de un solo uso (OTP).
- Actualizacion del correo electronico mediante la interfaz gobernada de Customer Data.
- Notificacion transaccional de confirmacion.
- Trazabilidad con `correlationId` y registro de auditoria.
- Mensajes de error controlados y reintento seguro.

### Fuera de alcance

- Actualizacion del numero celular. La iniciativa la contempla, pero esta HU solo cubre
  correo; el diseno deja la capacidad preparada para incorporarla despues.
- Validacion facial. El documento 06 no publica contrato ni proveedor definitivo.
- Cambio de contrasena, autenticacion o enrolamiento del afiliado.
- Modificaciones al modulo legado "Actualizacion de Datos" de PortalX 7.0.

### Dependencias

| Dependencia | Documento | Estado de la informacion |
|---|---|---|
| Customer Data API / Customer Data Service | 06, 07 | Documentada parcialmente, sin contrato |
| Identity Capability / Identity Orchestration | 06, 07 | Documentada parcialmente, sin contrato facial |
| Messaging Service | 06, 07 | Sin SLA ni politica de reintentos |
| IntegrationHub OnPrem | 04 | Operativo, con dependencias sincronicas |
| PortalX 7.4 | 03 | Lineamiento objetivo, no plataforma instalada |

## 2. Criterios de aceptacion refinados

| ID | Dado / Cuando / Entonces | Respuesta esperada |
|---|---|---|
| CA-01 | Un afiliado con sesion valida, correo nuevo valido y codigo OTP correcto, cuando confirma | 200, correo actualizado, notificacion enviada, auditoria registrada |
| CA-02 | Un afiliado con identidad no validada, cuando intenta continuar | 403 `IDV-401`, sin cambios en Customer Data |
| CA-03 | Un afiliado cuyo codigo expiro, cuando lo ingresa | 410 `OTP-410`, se ofrece reenviar |
| CA-04 | Un afiliado que ingresa un correo con formato invalido | 400 `VAL-001` antes de llamar servicios externos |
| CA-05 | Un correo que ya pertenece a otro afiliado | 409 `CD-409`, mensaje generico, sin exponer datos del otro afiliado |
| CA-06 | Messaging no disponible despues de actualizar | 202 `MSG-503`, actualizacion aplicada y notificacion en cola |
| CA-07 | Reintento con la misma `Idempotency-Key` | Misma respuesta, una sola actualizacion |
| CA-08 | Tres intentos de OTP fallidos | 429 `IDV-429`, transaccion bloqueada, se exige nuevo codigo |
| CA-09 | El proveedor de identidad tarda mas de 60 s | 504 `IDV-408`, mensaje de reintento |
| CA-10 | Safari 17 al confirmar | Flujo completo, sin excepciones de JavaScript en consola |
| CA-11 | Solicitud de estado de una transaccion existente | 200 con estado actual, sin datos sensibles |
| CA-12 | Consulta de una transaccion inexistente | 404 `SYS-404` |

## 3. Reglas de validacion

### En frontend (retroalimentacion inmediata)

- Correo obligatorio, maximo 120 caracteres, sin espacios ni puntos consecutivos.
- Formato `usuario@dominio.tld` con dominio de al menos dos caracteres.
- Codigo de verificacion de 6 digitos, solo numeros.
- Bloqueo del boton mientras hay una peticion en curso y proteccion contra doble clic.
- Rechazo de promesas siempre manejado (red de seguridad `unhandledrejection`).

### En backend (autoridad)

- Las mismas reglas de correo, repetidas en `CorreoValidator`.
- El afiliado se toma de la sesion; en la prueba llega en el cuerpo, con la anotacion de
  que en produccion no debe aceptarse desde el cliente.
- La actualizacion exige estado `IDENTIDAD_VALIDADA`.
- El correo se normaliza a minusculas antes de enviarlo a Customer Data.
- Idempotencia obligatoria en la confirmacion.
- Los errores de servicios externos se traducen a un codigo propio; nunca se propaga el
  mensaje tecnico del proveedor.

## 4. Supuestos

| ID | Supuesto | Justificacion |
|---|---|---|
| S-01 | El OTP tiene 6 digitos, vigencia de 5 minutos y 3 intentos | El documento 06 no detalla el contrato; el incidente INC-003 muestra que la expiracion fue un problema real |
| S-02 | El afiliado ya tiene una sesion autenticada en el portal | La HU dice "actualizar mi correo"; no plantea registro |
| S-03 | La validacion de identidad puede resolverse con OTP sobre el dato registrado | Es la capacidad documentada (generacion y validacion de codigo) |
| S-04 | Customer Data expone una actualizacion por afiliado y responde 409 ante duplicado | Es la convencion mas comun; el documento 06 no publica el contrato |
| S-05 | La notificacion de confirmacion se puede reintentar de forma asincrona | El documento 06 no garantiza entrega; la actualizacion no debe depender de ella |
| S-06 | El cambio se despliega por el procedimiento vigente (comite y ventana) | Documento 12 |
| S-07 | PortalX 7.4 es el destino, pero 7.0 sigue en produccion durante la transicion | Documentos 02 y 03 |
| S-08 | El servicio de identidad no requiere almacenar evidencia biometrica en el canal | Documento 08 lo prohibe expresamente |

## 5. Dudas para PO / LT

| ID | Pregunta | Opciones | Impacto si no se resuelve |
|---|---|---|---|
| D-01 | Si el correo nuevo ya esta registrado en otro afiliado, ¿se bloquea o se permite con alerta? | A) Bloquear con 409 / B) Permitir con marca de revision | Define CA-05 y el mensaje al usuario |
| D-02 | ¿Cuantos intentos de OTP se permiten y cuanto dura el codigo? | A) 3 intentos / 5 min / B) otro valor | Afecta la experiencia y el bloqueo de la transaccion |
| D-03 | ¿El reenvio de codigo invalida el anterior? | A) Si / B) No | Evita ventanas de codigos simultaneos |
| D-04 | ¿Que canal se usa para confirmar si el correo nuevo falla? | A) SMS al celular registrado / B) Solo mensaje en pantalla | Define el plan de contingencia |
| D-05 | ¿Se debe actualizar tambien el celular en la misma transaccion? | A) Solo correo / B) Correo y celular | Cambia el alcance y el contrato |
| D-06 | ¿Existe contrato del proveedor de identidad facial y cual es su SLA? | A) Contrato disponible / B) Se usa solo OTP | Sin respuesta, la validacion facial queda fuera |
| D-07 | ¿Cual es el SLA de entrega de Messaging y su politica de reintentos? | A) 5 min / 3 reintentos / B) otro | Define la cola de reintento y los mensajes |
| D-08 | ¿El cambio de correo debe notificarse tambien al correo anterior? | A) Si / B) No | Afecta la plantilla y el aviso de seguridad |
| D-09 | ¿Hay restriccion de horario para el flujo? | A) 24/7 / B) ventana definida | Afecta monitoreo y soporte |
| D-10 | ¿Quien aprueba las excepciones de identidad fallida? | A) Contact Center / B) Oficina | Define el flujo alterno |

## 6. Hechos, supuestos y recomendaciones

| Afirmacion | Tipo | Fuente |
|---|---|---|
| El portal opera hoy sobre PortalX 7.0 | Hecho | Doc 02, vigente 2025-11-10 |
| La evolucion objetivo es PortalX 7.4 | Hecho | Doc 03, lineamiento 2026-05-30 |
| La actualizacion de contacto exige validacion previa de identidad | Hecho | Doc 06 |
| No se deben almacenar imagenes faciales en el canal | Hecho | Doc 08, vigente |
| El flujo deberia completarse en menos de 3 minutos | Hecho (borrador) | Doc 14, estado borrador |
| Messaging no garantiza tiempos ni reintentos | Hecho | Doc 06 |
| El OTP dura 5 minutos y admite 3 intentos | Supuesto | S-01 |
| El correo duplicado se bloquea | Recomendacion | CA-05 |
| Conviene mantener IntegrationHub OnPrem para esta capacidad | Recomendacion | Docs 04 y 05 |
| La notificacion se maneja como proceso recuperable | Recomendacion | Doc 06 + INC-002 |

## 7. Riesgos iniciales

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| Contrato real de Customer Data distinto al propuesto | Media | Alto | Aislar en un adaptador y validar contrato en refinamiento tecnico |
| Proveedor de identidad inestable | Media | Alto | Timeout de 60 s, reintento controlado y canal alterno (INC-005) |
| Codigo OTP vence antes de terminar | Media | Medio | Mostrar contador, reenvio y mensajes claros (INC-003) |
| Diferencias de JavaScript en Safari | Alta | Medio | Componente sin dependencias no soportadas y pruebas de compatibilidad (INC-001) |
| Escaneo de seguridad fuera de la ventana | Media | Medio | Radicar el escaneo con 3 dias habiles de anticipacion (doc 08) |
| Variable de ambiente faltante en despliegue | Baja | Alto | `.env.example` y checklist (INC-004) |

## 8. Definition of Done propuesta

- Criterios CA-01 a CA-12 verificados.
- Pruebas unitarias y de integracion en verde.
- Pruebas de regresion sobre los modulos Certificados, PQRS y Mi Perfil.
- Revision de ciberseguridad y analisis estatico aprobados.
- Pull Request revisado por un par.
- README y variables de ambiente actualizados.
- Evidencia de ejecucion adjunta a la HU.
