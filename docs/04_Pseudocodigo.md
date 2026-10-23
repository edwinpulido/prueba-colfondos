# E4 - Pseudocodigo y fragmento representativo

## 1. Flujo completo en pseudocodigo

```
funcion actualizarCorreo(sesion, correoNuevo, idempotencyKey):
    // 1. Validaciones locales, sin llamar servicios
    correo = normalizarYValidar(correoNuevo)
    si correo es invalido:
        responder 400 VAL-001

    // 2. Confirmacion idempotente
    si existeTransaccion(idempotencyKey):
        responder respuestaAnterior(transaccion)

    transaccion = crearTransaccion(sesion.afiliado, correo, idempotencyKey)

    // 3. Identidad: obligatoria antes de tocar el dato
    intento = identidad.validar(sesion.referencia, codigo)
    si intento.timeout:
        responder 504 IDV-408
    si no intento.aprobado:
        responder 403 IDV-401

    // 4. Actualizacion gobernada
    resultado = customerData.actualizar(afiliado, correo)
    si resultado.duplicado:
        auditar("CD-409")
        responder 409 CD-409
    si resultado.error:
        responder 503 SYS-503

    // 5. Notificacion: no bloquea el resultado
    intentar:
        messaging.notificar(afiliado, correo)
        estado = APLICADA
    si falla:
        encolarReintento(transaccion)
        estado = APLICADA_NOTIFICACION_PENDIENTE

    auditar(estado)
    responder segunEstado(estado)
```

## 2. Fragmento real del backend

Tomado de `ActualizacionContactoService.aplicar`:

```java
if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
    Optional<Transaccion> previa = repositorio.buscarPorIdempotencyKey(idempotencyKey);
    if (previa.isPresent()) {
        return respuestaSegunEstado(previa.get());
    }
}

if (transaccion.getEstado() != EstadoActualizacion.IDENTIDAD_VALIDADA) {
    throw new EstadoInvalidoException(
            "La transaccion no se encuentra en un estado valido para actualizar el contacto.")
            .conTransaccion(transaccion.getId());
}

String correoNormalizado = CorreoValidator.normalizar(nuevoCorreo);
transaccion.setNuevoCorreo(correoNormalizado);

customerDataClient.actualizarCorreo(transaccion.getAfiliadoId(), correoNormalizado);

transaccion.setIdempotencyKey(idempotencyKey);
try {
    messagingClient.notificarActualizacion(
            transaccion.getAfiliadoId(), correoNormalizado, transaccion.getId());
    transaccion.cambiarEstado(EstadoActualizacion.APLICADA);
    transaccion.setResultado("OK-200", "Su correo electronico fue actualizado correctamente.");
} catch (RuntimeException error) {
    // La notificacion es recuperable: el cambio ya quedo aplicado.
    transaccion.cambiarEstado(EstadoActualizacion.APLICADA_NOTIFICACION_PENDIENTE);
    transaccion.setResultado("MSG-503",
            "Su correo fue actualizado. La confirmacion se enviara en los proximos minutos.");
}
```

### Por que este fragmento

- Muestra el orden real de las reglas: idempotencia, estado, validacion, actualizacion,
  notificacion.
- La notificacion esta en un `try` propio: un fallo de Messaging no revierte el cambio.
- No se guarda el codigo OTP ni el cuerpo de la respuesta del proveedor.
- La llave de idempotencia se registra antes de confirmar la respuesta.

## 3. Fragmento de frontend (confirmacion)

```javascript
Compat.ejecutarSeguro(function () {
    return cliente.aplicar(estado.transactionId, estado.correo, estado.idempotencyKey);
}).then(function (respuesta) {
    porId('resumen-correo').textContent = estado.correo;
    porId('resumen-estado').textContent = respuesta.estado === 'APLICADA'
        ? 'Actualizacion confirmada'
        : 'Actualizacion aplicada, notificacion en proceso';
    mostrarPaso(3);
}, mostrarError).then(function () {
    ocupado(boton, false);
});
```

La cadena siempre termina en un manejador de error (`mostrarError`) y libera el boton en
ambos casos. Un rechazo de promesa no queda sin manejar, que es el sintoma del incidente
de Safari.

## 4. Decisiones de implementacion

| Decision | Motivo |
|---|---|
| Estado persistido antes de cada llamada externa | Permite consultar y reintentar sin duplicar |
| `Idempotency-Key` en la confirmacion | Evita doble actualizacion por reintentos de red |
| Timeout diferenciado por servicio | Identidad puede tardar hasta 60 s; datos y notificacion no |
| Notificacion en cola ante fallo | El valor para el afiliado es el cambio, no el correo de confirmacion |
| Adaptadores por servicio | Si cambia el contrato, no cambia el flujo |
| Validacion duplicada frontend/backend | Experiencia inmediata y seguridad real |

## 5. Apoyo del asistente de desarrollo

El asistente se uso para proponer el borrador del catalogo de errores y para revisar el
manejo de promesas en la UI. Los ajustes posteriores (prioridad de errores, mensajes
genericos, traduccion de 409 y prueba de timeout) se hicieron contra los documentos del
Mini-RAG y quedaron cubiertos con pruebas automaticas. El detalle esta en
`docs/07_Registro_IA.md`.
