# Pull Request - HU-001 actualizacion de correo electronico

## Resumen

Implementa el flujo de actualizacion de correo electronico del afiliado con validacion de
identidad, actualizacion gobernada del dato, confirmacion y trazabilidad. Incluye el
componente de interfaz para PortalX 7.4 y el microservicio de orquestacion.

## Cambios

### Backend (`backend-orquestador`)

- API `/v1/contacto/actualizacion` con inicio, validacion de codigo, confirmacion y consulta.
- Maquina de estados de la transaccion y bloqueo por intentos fallidos.
- Idempotencia en la confirmacion mediante cabecera `Idempotency-Key`.
- Adaptadores para Identity, Customer Data y Messaging con traduccion de errores.
- Filtro de `correlationId` y logs estructurados.
- Catalogo unico de errores y manejador global de excepciones.
- Pruebas unitarias, de controlador y de adaptadores.

### Frontend (`frontend-portalx74`)

- Componente de tres pasos con confirmacion y resumen.
- Validacion de correo y codigo, contador de vigencia y reenvio.
- Utilidades de compatibilidad para Safari (UUID, clonado, fecha, timeout).
- Cliente HTTP con `correlationId`, `Idempotency-Key` y manejo explicito de errores.
- API simulada para demostracion y pruebas con el runner de Node.

### Documentacion

- Analisis de HU, diseno tecnico, pseudocodigo, matriz de pruebas, analisis del incidente
  Safari, registro de apoyo y este PR.

## Decisiones relevantes

| Decision | Motivo |
|---|---|
| Orquestador propio en canales | Evita exponer contratos y centraliza estado y errores |
| IntegrationHub OnPrem para esta HU | Depende de sistemas internos; Cloud se evalua con metricas reales |
| Notificacion no bloqueante | La actualizacion es el valor principal; el mensaje se reintenta |
| Manejo de errores por codigo | Contrato comun entre portal, backend y QA |
| Componente sin librerias del portal legado | Reduce dependencias y facilita la compatibilidad |

## Supuestos y pendientes

- Contratos de Identity y Customer Data son supuestos hasta confirmacion de Integracion.
- El repositorio de transacciones es en memoria; se reemplaza por la base del dominio.
- La validacion en Safari 17 real queda pendiente en el laboratorio de QA.
- Rate limiting se implementa en el gateway, fuera de este cambio.

## Pruebas

```bash
cd backend-orquestador && mvn test
cd frontend-portalx74 && node --test "tests/*.test.cjs"
```

Cobertura funcional del cambio:

- Flujo exitoso de punta a punta.
- Correo invalido, codigo incorrecto, codigo expirado e intentos agotados.
- Correo duplicado.
- Timeout y caida de servicios externos.
- Reintento idempotente.
- Respuesta 200 sin cuerpo y timeouts en el cliente.

## Evidencia

- Resultado de `mvn test` y del runner de Node adjuntos en la HU.
- Log con `correlationId` de una transaccion de prueba.
- Capturas de la pantalla en los tres pasos y en el estado de notificacion pendiente.

## Variables de ambiente

Ninguna obligatoria nueva para el portal. El backend usa `IDENTITY_BASE_URL`,
`CUSTOMER_DATA_BASE_URL` y `MESSAGING_BASE_URL`, documentadas en `.env.example`.

## Riesgo y rollback

- Riesgo: bajo para el portal, porque el componente se activa con bandera y no modifica
  vistas existentes.
- Rollback: desactivar el componente y revertir el despliegue del orquestador. Las
  transacciones en curso se consultan por `transactionId` y no requieren compensacion.
- El servicio no altera datos sin confirmacion de identidad ni sin llamada explicita del
  afiliado.

## Checklist

- [x] Criterios de aceptacion cubiertos por pruebas automaticas o manuales documentadas
- [x] Sin credenciales ni datos reales en el repositorio
- [x] Logs sin datos sensibles
- [x] `README` y `.env.example` actualizados
- [x] Analisis estatico sin hallazgos bloqueantes
- [ ] Escaneo de codigo seguro (se radica con el comite de cambios)
- [ ] Prueba de regresion en Safari 17 (laboratorio QA)
- [x] Revisión de par solicitada
