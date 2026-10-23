# Orquestador de actualizacion de contacto

Microservicio Java (Spring Boot) que implementa la HU-001: validacion de identidad,
actualizacion del correo, notificacion y trazabilidad.

## Ejecutar

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Puerto por defecto: `8081`. Variables en `../.env.example`.

## API

```bash
# 1. Iniciar
curl -X POST http://localhost:8081/v1/contacto/actualizacion \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: prueba-001" \
  -d '{"afiliadoId":"AFI-000123","nuevoCorreo":"ana@dominio.com","canalPreferido":"SMS"}'

# 2. Validar codigo
curl -X POST http://localhost:8081/v1/contacto/actualizacion/{transactionId}/verificacion \
  -H "Content-Type: application/json" \
  -d '{"codigo":"123456"}'

# 3. Confirmar
curl -X PUT http://localhost:8081/v1/contacto/actualizacion/{transactionId} \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 7d1c0a4e-1111-4a4a-9a4a-222233334444" \
  -d '{"nuevoCorreo":"ana@dominio.com"}'

# 4. Consultar estado
curl http://localhost:8081/v1/contacto/actualizacion/{transactionId}
```

## Paquetes

| Paquete | Contenido |
|---|---|
| `api` | Controlador, DTO y manejador global de errores |
| `client` | Adaptadores de Identity, Customer Data y Messaging |
| `config` | RestTemplate con timeouts y filtro de `correlationId` |
| `domain` | Transaccion, estados y repositorio |
| `exception` | Catalogo de errores de negocio |
| `service` | Reglas del flujo y maquina de estados |

## Pruebas

```bash
mvn test
```

- `CorreoValidatorTest`: reglas de correo.
- `ActualizacionContactoServiceTest`: flujo, intentos, expiracion, idempotencia,
  duplicado, notificacion pendiente.
- `ActualizacionContactoControllerTest`: contrato HTTP y manejo de errores.
- `IdentityClientTest` y `CustomerDataClientTest`: traduccion de respuestas externas.

## Consideraciones

- El repositorio de transacciones es en memoria; en produccion se reemplaza por la base
  del dominio sin cambiar el servicio.
- El `afiliadoId` debe tomarse de la sesion autenticada en el ambiente corporativo.
- Los timeouts se configuran en `application.yml` (identidad 60 s, datos 8 s, mensajeria 5 s).
- Los logs enmascaran el correo y nunca registran el codigo de verificacion.
