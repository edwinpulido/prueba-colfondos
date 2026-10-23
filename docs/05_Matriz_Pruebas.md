# E5 - Matriz de pruebas

## 1. Alcance

Cubre el flujo de actualizacion de correo: validaciones, identidad, actualizacion, notificacion,
errores de servicios externos, reintentos, regresion y compatibilidad de navegador.

## 2. Estrategia

| Nivel | Objetivo | Herramienta |
|---|---|---|
| Unitaria | Reglas aisladas y traduccion de errores | JUnit 5 + Mockito |
| Integracion | Flujo con dependencias simuladas | Spring Boot Test + MockRestServiceServer |
| Contrato HTTP | Codigos, cabeceras y mensajes | MockMvc |
| Frontend | Validaciones, cliente HTTP y compatibilidad | `node --test` |
| Manual | Regresion, Safari y proveedores reales | Laboratorio QA |

## 3. Matriz de casos

| ID | Tipo | Caso | Datos | Resultado esperado | Automatizado |
|---|---|---|---|---|---|
| CP-01 | Unitaria | Correo valido se normaliza | `  Ana.Lopez@Dominio.COM  ` | `ana.lopez@dominio.com` | Si |
| CP-02 | Unitaria | Correos invalidos | `ana@`, `ana@dominio`, `@dominio.com`, con espacios, `..` | Rechazo `VAL-001` | Si |
| CP-03 | Unitaria | Correo mayor a 120 caracteres | 115 caracteres + dominio | Rechazo `VAL-001` | Si |
| CP-04 | Unitaria | Codigo OTP con formato incorrecto | `12345`, `12345a` | Rechazo de formato | Si |
| CP-05 | Unitaria | Correo enmascarado en logs | `ana@dominio.com` | `a***@dominio.com` | Si |
| CP-06 | Integracion | Inicio del flujo exitoso | `AFI-000123`, correo valido | `202`, estado `PENDIENTE_VERIFICACION`, canal y vigencia | Si |
| CP-07 | Integracion | Correo invalido no invoca servicios | `ana@` | Excepcion de validacion, cero llamadas externas | Si |
| CP-08 | Integracion | Validacion de OTP correcta | `123456` | Estado `IDENTIDAD_VALIDADA` | Si |
| CP-09 | Negativa | OTP incorrecto tres veces | `000000` x3 | `OTP-401`, luego `IDV-429` y `BLOQUEADA` | Si |
| CP-10 | Negativa | OTP expirado | `999999` | `OTP-410`, estado `EXPIRADA` | Si |
| CP-11 | Integracion | Actualizacion y notificacion exitosas | Correo valido | Estado `APLICADA`, `OK-200`, un update y una notificacion | Si |
| CP-12 | Negativa | Messaging no disponible | Error 503 del cliente | Estado `APLICADA_NOTIFICACION_PENDIENTE`, `MSG-503` | Si |
| CP-13 | Negativa | Correo duplicado | `duplicado@dominio.com` / 409 | `CD-409`, sin notificacion, estado se mantiene | Si |
| CP-14 | Negativa | Reintento con la misma Idempotency-Key | Dos confirmaciones | Una sola actualizacion, una sola notificacion | Si |
| CP-15 | Negativa | Confirmar sin identidad validada | Flujo en paso 1 | `CON-409`, sin update | Si |
| CP-16 | Negativa | Identity no disponible al iniciar | 503 | `SYS-503`, transaccion `FALLIDA` | Si |
| CP-17 | Contrato | `POST` inicia y propaga correlationId | Cabecera `corr-123` | `202`, cabecera devuelta, mismo `transactionId` | Si |
| CP-18 | Contrato | Cuerpo invalido | `nuevoCorreo` vacio | `400`, codigo `VAL-001` | Si |
| CP-19 | Contrato | Confirmacion con notificacion pendiente | Servicio simulado | `202`, codigo `MSG-503` | Si |
| CP-20 | Contrato | Conflicto por duplicado | Servicio simulado | `409`, codigo `CD-409` | Si |
| CP-21 | Integracion | Cliente de Customer Data exitoso | `AFI-000123` | Llamada `PUT` a la ruta gobernada | Si |
| CP-22 | Integracion | 409 de Customer Data | Respuesta 409 | `ContactoDuplicadoException` | Si |
| CP-23 | Integracion | Identity devuelve referencia | Respuesta 200 | Referencia disponible | Si |
| CP-24 | Negativa | Identity rechaza al iniciar | 401 | `IDV-401` | Si |
| CP-25 | Negativa | Identity reporta codigo expirado | 410 + `OTP-EXPIRADO` | `OTP-410` | Si |
| CP-26 | Negativa | Identity caido en validacion | 503 | `SYS-503` | Si |
| CP-27 | Frontend | CorrelationId en la peticion | Llamada de inicio | Cabecera no vacia | Si |
| CP-28 | Frontend | Error del backend traducido | 409 con cuerpo | Error `CD-409` con `correlationId` | Si |
| CP-29 | Frontend | Respuesta 200 sin cuerpo | Cuerpo vacio | Sin excepcion, objeto vacio | Si |
| CP-30 | Frontend | Timeout de red | Servicio que no responde | `GEN-408` | Si |
| CP-31 | Frontend | Flujo completo simulado | OTP `123456` | `APLICADA` / `OK-200` | Si |
| CP-32 | Frontend | Duplicado y notificacion pendiente simulados | Datos de demostracion | `CD-409` y `MSG-503` | Si |
| CP-33 | Compatibilidad | UUID sin `crypto.randomUUID` | Contexto sin la API | Identificador valido y distinto | Si |
| CP-34 | Compatibilidad | Clonado sin `structuredClone` | Contexto sin la API | Copia independiente | Si |
| CP-35 | Compatibilidad | Fecha sin configuracion regional | `new Date(...)` | `dd/mm/aaaa hh:mm` | Si |
| CP-36 | Manual | Safari 17, flujo completo | Safari 17 real | Sin excepciones en consola, flujo completo | Pendiente QA |
| CP-37 | Manual | Regresion Certificados, PQRS, Mi Perfil | Portal 7.4 | Sin cambios funcionales | Pendiente QA |
| CP-38 | Manual | Indisponibilidad real de Identity | Proveedor detenido | Mensaje controlado y canal alterno | Pendiente QA |
| CP-39 | Manual | Accesibilidad basica | Teclado y lector de pantalla | Navegacion y anuncios correctos | Pendiente QA |

## 4. Ejecucion

### 4.1 Backend

```bash
cd backend-orquestador
mvn test
```

Resultado: **34 pruebas ejecutadas, 0 fallos**.

| Clase | Pruebas |
|---|---|
| `CorreoValidatorTest` | 12 |
| `ActualizacionContactoServiceTest` | 11 |
| `ActualizacionContactoControllerTest` | 4 |
| `IdentityClientTest` | 4 |
| `CustomerDataClientTest` | 2 |
| `ContactoApplicationTest` | 1 |

### 4.2 Frontend

```bash
cd frontend-portalx74
node --test "tests/*.test.cjs"
```

Resultado: **17 pruebas ejecutadas, 0 fallos**.

| Archivo | Pruebas |
|---|---|
| `tests/validaciones.test.cjs` | 8 |
| `tests/api.test.cjs` | 6 |
| `tests/compat-safari.test.cjs` | 3 |

## 5. Evidencias a adjuntar en la HU

1. Salida de `mvn test` y de `node --test`.
2. Log del orquestador con `correlationId`, `transactionId` y correo enmascarado.
3. Capturas de los pasos 1, 2, 3 y del resultado con notificacion pendiente.
4. Video o captura de Safari 17 con la consola limpia (CP-36).
5. Resultado de regresion de los modulos existentes (CP-37).
6. Defectos encontrados, si aplica, con su estado.

## 6. Criterios de entrada y salida

**Entrada:** contrato de API acordado, ambiente de QA disponible, datos sinteticos cargados.

**Salida:**

- 100 % de casos automatizados en verde.
- Casos manuales ejecutados y evidenciados.
- Sin defectos abiertos de severidad alta.
- Cobertura minima de 80 % en servicio y validadores.
- Prueba de humo en produccion despues del despliegue.

## 7. Trazabilidad con criterios de aceptacion

| Criterio | Casos |
|---|---|
| CA-01 Flujo exitoso | CP-06, CP-08, CP-11, CP-17, CP-31 |
| CA-02 Identidad no validada | CP-15, CP-24 |
| CA-03 Codigo expirado | CP-10, CP-25 |
| CA-04 Correo invalido | CP-02, CP-03, CP-07, CP-18 |
| CA-05 Correo duplicado | CP-13, CP-22 |
| CA-06 Messaging caido | CP-12, CP-19, CP-32 |
| CA-07 Idempotencia | CP-14 |
| CA-08 Intentos agotados | CP-09 |
| CA-09 Timeout de identidad | CP-30, CP-26 |
| CA-10 Safari | CP-33 a CP-36 |
| CA-11 Consulta de estado | CP-16 |
| CA-12 Transaccion inexistente | Cubierto en contrato `SYS-404` |
